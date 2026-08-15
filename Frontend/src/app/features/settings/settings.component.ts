import { Component, signal, inject } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatSliderModule } from '@angular/material/slider';
import { MatRadioModule } from '@angular/material/radio';
import { MatDividerModule } from '@angular/material/divider';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTabsModule } from '@angular/material/tabs';
import { MatSnackBar } from '@angular/material/snack-bar';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { AuthService } from '../auth/services/auth.service';

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [
    MatCardModule,
    MatIconModule,
    MatButtonModule,
    MatInputModule,
    MatFormFieldModule,
    MatSelectModule,
    MatCheckboxModule,
    MatSliderModule,
    MatRadioModule,
    MatDividerModule,
    MatProgressSpinnerModule,
    MatTabsModule,
    ReactiveFormsModule
  ],
  templateUrl: './settings.component.html',
  styleUrl: './settings.component.scss'
})
export class SettingsComponent {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly snackBar = inject(MatSnackBar);

  readonly isLoading = signal(false);
  readonly selectedTab = signal(0);

  readonly profileForm: FormGroup = this.fb.group({
    name: ['', [Validators.required, Validators.minLength(2)]],
    email: ['', [Validators.required, Validators.email]],
    phone: [''],
    bio: ['']
  });

  readonly notificationForm: FormGroup = this.fb.group({
    emailNotifications: [true],
    pushNotifications: [false],
    taskReminders: [true],
    projectUpdates: [true],
    weeklyDigest: [false]
  });

  readonly appearanceForm: FormGroup = this.fb.group({
    theme: ['light'],
    language: ['en'],
    fontSize: [14],
    density: ['comfortable']
  });

  readonly securityForm: FormGroup = this.fb.group({
    currentPassword: ['', Validators.required],
    newPassword: ['', [Validators.required, Validators.minLength(8)]],
    confirmPassword: ['', Validators.required]
  }, { validators: this.passwordMatchValidator });

  passwordMatchValidator(formGroup: FormGroup): { [key: string]: boolean } | null {
    const password = formGroup.get('newPassword');
    const confirmPassword = formGroup.get('confirmPassword');

    if (password && confirmPassword && password.value !== confirmPassword.value) {
      confirmPassword.setErrors({ passwordMismatch: true });
      return { passwordMismatch: true };
    }
    return null;
  }

  onProfileSubmit() {
    if (this.profileForm.invalid) {
      return;
    }

    this.isLoading.set(true);
    // Simulate API call
    setTimeout(() => {
      this.isLoading.set(false);
      this.snackBar.open('Profile updated successfully', 'Close', { duration: 3000 });
    }, 1000);
  }

  onNotificationSubmit() {
    this.isLoading.set(true);
    setTimeout(() => {
      this.isLoading.set(false);
      this.snackBar.open('Notification preferences saved', 'Close', { duration: 3000 });
    }, 1000);
  }

  onAppearanceSubmit() {
    this.isLoading.set(true);
    setTimeout(() => {
      this.isLoading.set(false);
      this.snackBar.open('Appearance settings saved', 'Close', { duration: 3000 });
    }, 1000);
  }

  onSecuritySubmit() {
    if (this.securityForm.invalid) {
      return;
    }

    this.isLoading.set(true);
    setTimeout(() => {
      this.isLoading.set(false);
      this.snackBar.open('Password changed successfully', 'Close', { duration: 3000 });
      this.securityForm.reset();
    }, 1000);
  }

  onTabChange(index: number) {
    this.selectedTab.set(index);
  }

  logout() {
    this.authService.logout();
    this.snackBar.open('Logged out successfully', 'Close', { duration: 3000 });
  }
}
