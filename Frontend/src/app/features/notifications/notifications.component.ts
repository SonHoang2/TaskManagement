import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { MatTooltipModule } from '@angular/material/tooltip';
import { AppNotification, Page } from '../../core/models';
import { NotificationsService } from '../../core/services/notifications.service';
import { ToastService } from '../../core/ui/toast.service';
import {
  EmptyStateComponent,
  SkeletonComponent,
} from '../../shared/components/empty-state.component';
import { AgoPipe } from '../../shared/pipes';
import { notificationMeta } from './notification-meta';

@Component({
  selector: 'app-notifications',
  imports: [MatTooltipModule, EmptyStateComponent, SkeletonComponent, AgoPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="page" style="max-width: 860px">
      <div class="page-header">
        <div class="page-header__text">
          <h1>Notifications</h1>
          <p>Assignments, comments and invitations from your projects.</p>
        </div>
        <div class="page-header__actions">
          <button class="btn" type="button" (click)="markAll()" [disabled]="!svc.hasUnread()">
            <span class="i">done_all</span>Mark all as read
          </button>
        </div>
      </div>

      <div class="toolbar">
        <div class="segmented">
          <button type="button" [class.active]="!unreadOnly()" (click)="setFilter(false)">
            All
          </button>
          <button type="button" [class.active]="unreadOnly()" (click)="setFilter(true)">
            Unread <span class="count">{{ svc.unreadCount() }}</span>
          </button>
        </div>
      </div>

      <div class="card">
        @if (loading() && !page()) {
          <div class="card__body"><app-skeleton [count]="5" [height]="52" /></div>
        } @else {
          @for (n of items(); track n.id) {
            <div class="n-row" [class.n-row--unread]="!n.isRead" (click)="open(n)">
              <span class="notif__icon notif__icon--{{ meta(n).tone }}"
                ><span class="i">{{ meta(n).icon }}</span></span
              >
              <div class="grow">
                <div class="n-row__text">{{ n.content || meta(n).label }}</div>
                <div class="subtle small">{{ meta(n).label }} · {{ n.createdAt | ago }}</div>
              </div>
              @if (!n.isRead) {
                <span class="dot"></span>
              }
              <div class="n-row__actions">
                <button
                  class="btn btn--ghost btn--icon btn--sm"
                  type="button"
                  (click)="$event.stopPropagation(); toggleRead(n)"
                  [matTooltip]="n.isRead ? 'Mark as unread' : 'Mark as read'"
                >
                  <span class="i">{{ n.isRead ? 'mark_email_unread' : 'mark_email_read' }}</span>
                </button>
                <button
                  class="btn btn--ghost btn--icon btn--sm"
                  type="button"
                  (click)="$event.stopPropagation(); remove(n)"
                  matTooltip="Delete"
                >
                  <span class="i">delete</span>
                </button>
              </div>
            </div>
          } @empty {
            <app-empty-state
              icon="notifications_off"
              [title]="unreadOnly() ? 'No unread notifications' : 'No notifications yet'"
              text="You're all caught up."
            />
          }
          @if (page()?.hasNext) {
            <div style="padding: 12px; text-align: center; border-top: 1px solid var(--border)">
              <button class="btn btn--sm" type="button" (click)="more()" [disabled]="loading()">
                Load more
              </button>
            </div>
          }
        }
      </div>
    </div>
  `,
  styles: `
    .n-row {
      display: flex;
      align-items: center;
      gap: 14px;
      padding: 14px 18px;
      border-bottom: 1px solid var(--border);
      cursor: pointer;
      transition: background 0.12s;
      &:last-of-type {
        border-bottom: 0;
      }
      &:hover {
        background: var(--hover);
        .n-row__actions {
          opacity: 1;
        }
      }
      &--unread {
        background: color-mix(in srgb, var(--primary-soft) 45%, transparent);
        .n-row__text {
          font-weight: 600;
        }
      }
      &__text {
        font-size: 14px;
        line-height: 1.45;
      }
      &__actions {
        display: flex;
        gap: 2px;
        opacity: 0;
        transition: opacity 0.12s;
      }
      @media (hover: none) {
        &__actions {
          opacity: 1;
        }
      }
    }
    .notif__icon {
      width: 36px;
      height: 36px;
      border-radius: 10px;
      display: grid;
      place-items: center;
      flex-shrink: 0;
      background: var(--primary-soft);
      color: var(--primary-text);
      &--progress {
        background: var(--progress-soft);
        color: var(--progress);
      }
      &--done {
        background: var(--done-soft);
        color: var(--done);
      }
      &--warning {
        background: var(--warning-soft);
        color: var(--warning);
      }
    }
  `,
})
export class NotificationsComponent {
  protected readonly svc = inject(NotificationsService);
  private readonly toast = inject(ToastService);
  private readonly router = inject(Router);
  protected readonly meta = notificationMeta;

  protected readonly unreadOnly = signal(false);
  protected readonly items = signal<AppNotification[]>([]);
  protected readonly page = signal<Page<AppNotification> | null>(null);
  protected readonly loading = signal(true);

  constructor() {
    this.fetch(0);
  }

  protected setFilter(unread: boolean): void {
    this.unreadOnly.set(unread);
    this.items.set([]);
    this.page.set(null);
    this.fetch(0);
  }

  protected more(): void {
    this.fetch((this.page()?.page ?? 0) + 1);
  }

  private fetch(pageNo: number): void {
    this.loading.set(true);
    this.svc.list({ unreadOnly: this.unreadOnly(), page: pageNo, size: 20 }).subscribe({
      next: (p) => {
        this.page.set(p);
        this.items.update((l) => (pageNo === 0 ? p.content : [...l, ...p.content]));
        this.loading.set(false);
      },
      error: (e) => {
        this.loading.set(false);
        this.toast.error(e);
      },
    });
  }

  protected open(n: AppNotification): void {
    if (!n.isRead) this.toggleRead(n);
    this.router.navigateByUrl(this.meta(n).link);
  }

  protected toggleRead(n: AppNotification): void {
    this.svc.markRead(n, !n.isRead).subscribe({
      next: (u) => {
        if (this.unreadOnly() && u.isRead) this.items.update((l) => l.filter((x) => x.id !== u.id));
        else this.items.update((l) => l.map((x) => (x.id === u.id ? u : x)));
      },
      error: (e) => this.toast.error(e),
    });
  }

  protected remove(n: AppNotification): void {
    this.svc.delete(n).subscribe({
      next: () => this.items.update((l) => l.filter((x) => x.id !== n.id)),
      error: (e) => this.toast.error(e),
    });
  }

  protected markAll(): void {
    this.svc.markAllRead(this.items()).subscribe({
      next: () => {
        this.svc.refresh();
        this.setFilter(this.unreadOnly());
      },
      error: (e) => this.toast.error(e),
    });
  }
}
