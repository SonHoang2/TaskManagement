import { Component, input, output, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';

interface NavItem {
  path: string;
  label: string;
  icon: 'dashboard' | 'tasks' | 'projects' | 'team' | 'settings';
}

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './sidebar.component.html',
  styleUrl: './sidebar.component.scss',
  host: {
    '[class.mobile-open]': 'isMobileOpen()'
  }
})
export class SidebarComponent {
  readonly isOpen = input<boolean>(true);
  readonly isMobileOpen = input<boolean>(false);
  readonly toggle = output<void>();
  readonly close = output<void>();

  protected readonly navItems: NavItem[] = [
    { path: '/dashboard', label: 'Dashboard', icon: 'dashboard' },
    { path: '/tasks', label: 'Tasks', icon: 'tasks' },
    { path: '/projects', label: 'Projects', icon: 'projects' },
    { path: '/team', label: 'Team', icon: 'team' },
    { path: '/settings', label: 'Settings', icon: 'settings' }
  ];

  constructor(private router: Router) {}

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
