import { Component, output, signal, inject, ElementRef } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../../features/auth/services/auth.service';

@Component({
  selector: 'app-header',
  standalone: true,
  templateUrl: './header.component.html',
  styleUrl: './header.component.scss',
  host: {
    '(document:click)': 'onDocumentClick($event)'
  }
})
export class HeaderComponent {
  readonly toggleSidebar = output<void>();
  
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);
  private readonly elementRef = inject(ElementRef);

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

  onDocumentClick(event: MouseEvent) {
    if (this.isUserMenuOpen()) {
      const target = event.target as HTMLElement;
      const clickedInside = this.elementRef.nativeElement.contains(target);
      if (!clickedInside) {
        this.closeUserMenu();
      }
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
