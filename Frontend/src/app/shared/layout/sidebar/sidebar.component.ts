import { Component, input, output, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { MatNavList, MatListItem } from '@angular/material/list';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

interface NavItem {
  path: string;
  label: string;
  icon: string;
}

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, MatNavList, MatListItem, MatButtonModule, MatIconModule],
  templateUrl: './sidebar.component.html',
  styleUrl: './sidebar.component.scss',
  host: {
    '[class.mobile-open]': 'isMobileOpen()',
  },
})
export class SidebarComponent {
  readonly isOpen = input<boolean>(true);
  readonly isMobileOpen = input<boolean>(false);
  readonly toggle = output<void>();
  readonly close = output<void>();
  private readonly router = inject(Router);

  protected readonly isMobile = window.innerWidth <= 768;

  protected readonly navItems: NavItem[] = [
    { path: '/dashboard', label: 'Dashboard', icon: 'dashboard' },
    { path: '/tasks', label: 'Tasks', icon: 'task' },
    { path: '/projects', label: 'Projects', icon: 'folder' },
    { path: '/team', label: 'Team', icon: 'people' },
    { path: '/settings', label: 'Settings', icon: 'settings' },
  ];

  onToggle() {
    this.toggle.emit();
  }

  onNavigate() {
    this.close.emit();
  }

  navigateTo(path: string) {
    this.router.navigate([path]);
    this.onNavigate();
  }
}
