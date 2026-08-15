import { Component, signal, inject, output, input, computed } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../../features/auth/services/auth.service';
import { MatMenuModule } from '@angular/material/menu';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDividerModule } from '@angular/material/divider';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { NgOptimizedImage } from '@angular/common';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [
    MatMenuModule,
    MatButtonModule,
    MatIconModule,
    MatDividerModule,
    MatSnackBarModule,
    NgOptimizedImage,
  ],
  templateUrl: './header.component.html',
  styleUrl: './header.component.scss',
})
export class HeaderComponent {
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);
  readonly menuToggle = output<void>();
  readonly sidebarToggle = output<void>();
  readonly isSidebarOpen = input<boolean>(true);

  protected readonly currentUser = signal<{
    name: string;
    email: string;
    avatarUrl: string | null;
  } | null>(null);
  protected readonly userInitials = computed(() => {
    const user = this.currentUser();
    if (!user || !user.name) return '';
    return user.name.charAt(0).toUpperCase();
  });

  constructor() {
    this.loadUserInfo();
  }

  private loadUserInfo() {
    const userName = this.authService.getUserName();
    const userEmail = this.authService.getUserEmail();
    const userAvatar = this.authService.getUserAvatar();

    if (userName || userEmail) {
      this.currentUser.set({
        name: userName || 'User',
        email: userEmail || '',
        avatarUrl: userAvatar,
      });
    }
  }

  onLogout() {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  onMenuToggle() {
    this.menuToggle.emit();
  }

  onSidebarToggle() {
    this.sidebarToggle.emit();
  }
}
