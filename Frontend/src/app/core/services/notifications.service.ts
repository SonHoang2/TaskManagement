import { Injectable, computed, inject, signal } from '@angular/core';
import { Client } from '@stomp/stompjs';
import { Observable, Subject, forkJoin, map, of, tap } from 'rxjs';
import { ApiClient } from '../http/api-client';
import { AuthService } from '../auth/auth.service';
import { AppNotification, Page } from '../models';

@Injectable({ providedIn: 'root' })
export class NotificationsService {
  private readonly api = inject(ApiClient);
  private readonly auth = inject(AuthService);

  /** Latest notifications for the bell; refreshed by {@link refresh}. */
  readonly recent = signal<AppNotification[]>([]);
  readonly unreadCount = signal(0);
  readonly hasUnread = computed(() => this.unreadCount() > 0);

  private readonly pushed = new Subject<AppNotification>();
  /** Notifications pushed by the server as they are created. */
  readonly incoming$ = this.pushed.asObservable();
  private client: Client | null = null;

  list(
    opts: { unreadOnly?: boolean; page?: number; size?: number } = {},
  ): Observable<Page<AppNotification>> {
    return this.api
      .get<{ page: Page<AppNotification> }>('notification-service', '/notifications', {
        isRead: opts.unreadOnly ? false : undefined,
        page: opts.page ?? 0,
        size: opts.size ?? 20,
        sort: 'createdAt,desc',
      })
      .pipe(map((d) => d.page));
  }

  refresh(): void {
    if (!this.auth.userId()) return;
    forkJoin({
      recent: this.list({ size: 8 }),
      unread: this.list({ unreadOnly: true, size: 1 }),
    }).subscribe({
      next: ({ recent, unread }) => {
        this.recent.set(recent.content);
        this.unreadCount.set(unread.totalElements);
      },
      error: () => {
        /* notification-service may be down; the bell just stays quiet */
      },
    });
  }

  /** Opens the STOMP connection that delivers new notifications in real time. */
  connect(): void {
    if (this.client || !this.auth.token) return;
    this.client = new Client({
      brokerURL: this.api.wsUrl('notification-service', '/ws'),
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      // Read the token on every (re)connect so a renewed session is picked up.
      beforeConnect: (client) => {
        client.connectHeaders = { Authorization: `Bearer ${this.auth.token ?? ''}` };
      },
      onConnect: () => {
        this.client?.subscribe('/user/topic/notifications', (frame) =>
          this.onPushed(JSON.parse(frame.body) as AppNotification),
        );
        // Catch up on anything created while we were disconnected.
        this.refresh();
      },
    });
    this.client.activate();
  }

  disconnect(): void {
    void this.client?.deactivate();
    this.client = null;
  }

  private onPushed(n: AppNotification): void {
    this.recent.update((list) => [n, ...list.filter((x) => x.id !== n.id)].slice(0, 8));
    if (!n.isRead) this.unreadCount.update((c) => c + 1);
    this.pushed.next(n);
  }

  markRead(n: AppNotification, isRead = true): Observable<AppNotification> {
    return this.api
      .patch<{ notification: AppNotification }>('notification-service', `/notifications/${n.id}`, {
        isRead,
      })
      .pipe(
        map((d) => d.notification),
        tap((updated) => {
          this.recent.update((list) => list.map((x) => (x.id === updated.id ? updated : x)));
          if (n.isRead !== isRead)
            this.unreadCount.update((c) => Math.max(0, c + (isRead ? -1 : 1)));
        }),
      );
  }

  markAllRead(items: AppNotification[]): Observable<unknown> {
    const unread = items.filter((n) => !n.isRead);
    if (!unread.length) return of(null);
    return forkJoin(unread.map((n) => this.markRead(n)));
  }

  delete(n: AppNotification): Observable<void> {
    return this.api.delete('notification-service', `/notifications/${n.id}`).pipe(
      tap(() => {
        this.recent.update((list) => list.filter((x) => x.id !== n.id));
        if (!n.isRead) this.unreadCount.update((c) => Math.max(0, c - 1));
      }),
    );
  }
}
