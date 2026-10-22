import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  HostListener,
  computed,
  inject,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatMenuModule } from '@angular/material/menu';
import { MatTooltipModule } from '@angular/material/tooltip';
import { filter, interval } from 'rxjs';
import { AuthService } from '../core/auth/auth.service';
import { NotificationsService } from '../core/services/notifications.service';
import { WorkspaceService } from '../core/services/workspace.service';
import { ThemeService } from '../core/ui/theme.service';
import { colorFor } from '../core/utils/format';
import { AvatarComponent } from '../shared/components/avatar.component';
import { DialogsService } from '../shared/dialogs.service';
import { NotificationBellComponent } from './notification-bell.component';

interface NavItem {
  path: string;
  label: string;
  icon: string;
  badge?: () => number;
}

@Component({
  selector: 'app-shell',
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    MatMenuModule,
    MatTooltipModule,
    AvatarComponent,
    NotificationBellComponent,
  ],
  templateUrl: './shell.component.html',
  styleUrl: './shell.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ShellComponent {
  protected readonly auth = inject(AuthService);
  protected readonly workspace = inject(WorkspaceService);
  protected readonly theme = inject(ThemeService);
  private readonly notifications = inject(NotificationsService);
  private readonly dialogs = inject(DialogsService);
  private readonly router = inject(Router);

  protected readonly mobileOpen = signal(false);
  protected readonly isMac = /Mac|iPhone|iPad/.test(navigator.platform);
  protected readonly colorFor = colorFor;

  protected readonly nav: NavItem[] = [
    { path: '/dashboard', label: 'Dashboard', icon: 'space_dashboard' },
    { path: '/my-tasks', label: 'My tasks', icon: 'task_alt' },
    { path: '/projects', label: 'Projects', icon: 'folder_open' },
    { path: '/team', label: 'Team', icon: 'group' },
    {
      path: '/invitations',
      label: 'Invitations',
      icon: 'mail',
      badge: () => this.workspace.pendingInvitations(),
    },
    { path: '/notifications', label: 'Notifications', icon: 'notifications' },
  ];

  protected readonly sidebarProjects = computed(() => this.workspace.projects().slice(0, 7));

  constructor() {
    this.workspace.load();
    this.notifications.refresh();

    interval(60_000)
      .pipe(takeUntilDestroyed())
      .subscribe(() => {
        this.notifications.refresh();
        this.workspace.refreshInvitations();
      });

    this.router.events
      .pipe(
        filter((e) => e instanceof NavigationEnd),
        takeUntilDestroyed(inject(DestroyRef)),
      )
      .subscribe(() => this.mobileOpen.set(false));
  }

  @HostListener('document:keydown', ['$event'])
  onKey(e: KeyboardEvent): void {
    if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === 'k') {
      e.preventDefault();
      this.openPalette();
    }
  }

  protected openPalette(): void {
    this.dialogs.commandPalette();
  }

  protected newTask(): void {
    const match = this.router.url.match(/^\/projects\/([0-9a-f-]{36})/i);
    this.dialogs.taskForm({ projectId: match?.[1] }).subscribe((task) => {
      if (task) this.router.navigate(['/projects', task.projectId, 'board']);
    });
  }

  protected newProject(): void {
    this.dialogs.projectForm().subscribe((project) => {
      if (project) this.router.navigate(['/projects', project.id]);
    });
  }

  protected logout(): void {
    this.auth.logout();
  }
}
