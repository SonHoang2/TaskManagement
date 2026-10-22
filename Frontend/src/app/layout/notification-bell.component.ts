import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { MatMenuModule } from '@angular/material/menu';
import { MatTooltipModule } from '@angular/material/tooltip';
import { AppNotification } from '../core/models';
import { NotificationsService } from '../core/services/notifications.service';
import { ToastService } from '../core/ui/toast.service';
import { AgoPipe } from '../shared/pipes';
import { notificationMeta } from '../features/notifications/notification-meta';

@Component({
  selector: 'app-notification-bell',
  imports: [MatMenuModule, MatTooltipModule, RouterLink, AgoPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './notification-bell.component.html',
  styleUrl: './notification-bell.component.scss',
})
export class NotificationBellComponent {
  protected readonly svc = inject(NotificationsService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);
  protected readonly meta = notificationMeta;

  protected open(n: AppNotification): void {
    if (!n.isRead) this.svc.markRead(n).subscribe({ error: () => {} });
    this.router.navigateByUrl(this.meta(n).link);
  }

  protected markAll(): void {
    this.svc.markAllRead(this.svc.recent()).subscribe({
      next: () => this.svc.refresh(),
      error: (e) => this.toast.error(e),
    });
  }
}
