import { Component, signal, inject, output } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../../features/auth/services/auth.service';
import { MatMenuModule } from '@angular/material/menu';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDividerModule } from '@angular/material/divider';
import { MatSnackBarModule } from '@angular/material/snack-bar';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [MatMenuModule, MatButtonModule, MatIconModule, MatDividerModule, MatSnackBarModule],
  templateUrl: './header.component.html',
  styleUrl: './header.component.scss'
})
export class HeaderComponent {
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);
  readonly menuToggle = output<void>();

  protected readonly currentUser = signal<{ name: string; email: string } | null>(null);

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

  onLogout() {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  onMenuToggle() {
    this.menuToggle.emit();
  }
}
