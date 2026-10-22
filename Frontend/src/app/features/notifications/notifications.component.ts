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
  templateUrl: './notifications.component.html',
  styleUrl: './notifications.component.scss',
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
