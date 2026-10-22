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
  template: `
    <button
      class="btn btn--ghost btn--icon bell"
      type="button"
      [matMenuTriggerFor]="menu"
      (menuOpened)="svc.refresh()"
      matTooltip="Notifications"
      aria-label="Notifications"
    >
      <span class="i" [class.fill]="svc.hasUnread()">notifications</span>
      @if (svc.unreadCount() > 0) {
        <span class="bell__badge">{{ svc.unreadCount() > 99 ? '99+' : svc.unreadCount() }}</span>
      }
    </button>

    <mat-menu #menu="matMenu" class="menu notif-menu" xPosition="before">
      <div class="notif-head" (click)="$event.stopPropagation()">
        <h3>Notifications</h3>
        <span class="spacer"></span>
        @if (svc.hasUnread()) {
          <button class="btn btn--ghost btn--sm" type="button" (click)="markAll()">
            Mark all read
          </button>
        }
      </div>
      <div class="notif-list">
        @for (n of svc.recent(); track n.id) {
          <button class="notif" mat-menu-item [class.notif--unread]="!n.isRead" (click)="open(n)">
            <span class="notif__icon notif__icon--{{ meta(n).tone }}">
              <span class="i">{{ meta(n).icon }}</span>
            </span>
            <span class="notif__body">
              <span class="notif__text">{{ n.content || meta(n).label }}</span>
              <span class="notif__time">{{ n.createdAt | ago }}</span>
            </span>
            @if (!n.isRead) {
              <span class="dot"></span>
            }
          </button>
        } @empty {
          <div class="notif-empty">
            <span class="i">notifications_off</span>
            <p>You're all caught up.</p>
          </div>
        }
      </div>
      <a class="notif-all" mat-menu-item routerLink="/notifications">View all notifications</a>
    </mat-menu>
  `,
  styles: `
    :host {
      position: relative;
      display: inline-flex;
    }
    .bell__badge {
      position: absolute;
      top: 3px;
      right: 2px;
      min-width: 16px;
      height: 16px;
      padding: 0 4px;
      border-radius: 999px;
      background: var(--danger);
      color: #fff;
      font-size: 10px;
      font-weight: 700;
      display: grid;
      place-items: center;
      box-shadow: 0 0 0 2px var(--bg);
    }
  `,
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
