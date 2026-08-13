import { Component, output, signal, inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../../features/auth/services/auth.service';

@Component({
  selector: 'app-header',
  standalone: true,
  templateUrl: './header.component.html',
  styleUrl: './header.component.scss'
})
export class HeaderComponent {
  readonly toggleSidebar = output<void>();
  
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);

  protected readonly currentUser = signal<{ name: string; email: string } | null>(null);
  protected readonly isUserMenuOpen = signal(false);

  constructor() {
    this.loadUserInfo();
  }

  private loadUserInfo() {
    const userName = this.authService.getUserName();
    const userEmail = this.authService.getUserEmail();
    
    if (userName || userEmail) {
      this.currentUser.set({
        name: userName || 'User',
        email: userEmail || ''
      });
    }
  }

  onToggleSidebar() {
    this.toggleSidebar.emit();
  }

  toggleUserMenu() {
    this.isUserMenuOpen.update(isOpen => !isOpen);
  }

  onLogout() {
    this.authService.logout();
    this.router.navigate(['/login']);
    this.isUserMenuOpen.set(false);
  }

  closeUserMenu() {
    this.isUserMenuOpen.set(false);
  }
}
