import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import {
  AbstractControl,
  NonNullableFormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { toSignal } from '@angular/core/rxjs-interop';
import { AuthService } from '../../core/auth/auth.service';
import { UsersService } from '../../core/services/users.service';
import { ThemePref, ThemeService } from '../../core/ui/theme.service';
import { ToastService } from '../../core/ui/toast.service';
import { ConfirmService } from '../../core/ui/confirm.service';
import { AvatarComponent } from '../../shared/components/avatar.component';
import { ShortDatePipe } from '../../shared/pipes';

function matchPassword(g: AbstractControl): ValidationErrors | null {
  const { password, confirm } = g.value as { password: string; confirm: string };
  return confirm && password !== confirm ? { mismatch: true } : null;
}

@Component({
  selector: 'app-settings',
  imports: [ReactiveFormsModule, AvatarComponent, ShortDatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './settings.component.html',
  styleUrl: './settings.component.scss',
})
export class SettingsComponent {
  protected readonly auth = inject(AuthService);
  protected readonly theme = inject(ThemeService);
  private readonly users = inject(UsersService);
  private readonly toast = inject(ToastService);
  private readonly confirm = inject(ConfirmService);
  private readonly fb = inject(NonNullableFormBuilder);

  protected readonly themes: { value: ThemePref; label: string; icon: string }[] = [
    { value: 'light', label: 'Light', icon: 'light_mode' },
    { value: 'dark', label: 'Dark', icon: 'dark_mode' },
    { value: 'system', label: 'System', icon: 'computer' },
  ];

  protected readonly savingProfile = signal(false);
  protected readonly savingPassword = signal(false);

  protected readonly profile = this.fb.group({
    fullName: [this.auth.user()?.fullName ?? '', [Validators.required, Validators.maxLength(120)]],
    email: [this.auth.user()?.email ?? '', [Validators.required, Validators.email]],
    avatarUrl: [this.auth.user()?.avatarUrl ?? '', Validators.maxLength(500)],
  });
  protected readonly avatarPreview = toSignal(this.profile.controls.avatarUrl.valueChanges, {
    initialValue: this.profile.controls.avatarUrl.value,
  });

  protected readonly security = this.fb.group(
    {
      password: ['', [Validators.required, Validators.minLength(8)]],
      confirm: ['', Validators.required],
    },
    { validators: matchPassword },
  );

  protected resetProfile(): void {
    const u = this.auth.user();
    this.profile.reset({
      fullName: u?.fullName ?? '',
      email: u?.email ?? '',
      avatarUrl: u?.avatarUrl ?? '',
    });
  }

  protected saveProfile(): void {
    if (this.profile.invalid) {
      this.profile.markAllAsTouched();
      return;
    }
    const v = this.profile.getRawValue();
    this.savingProfile.set(true);
    this.users
      .update(this.auth.userId(), {
        fullName: v.fullName.trim(),
        email: v.email.trim(),
        avatarUrl: v.avatarUrl.trim(),
      })
      .subscribe({
        next: (u) => {
          this.auth.updateUser(u);
          this.savingProfile.set(false);
          this.resetProfile();
          this.toast.success('Profile saved');
        },
        error: (e) => {
          this.savingProfile.set(false);
          this.toast.error(e);
        },
      });
  }

  protected savePassword(): void {
    if (this.security.invalid) {
      this.security.markAllAsTouched();
      return;
    }
    this.savingPassword.set(true);
    this.users
      .update(this.auth.userId(), { password: this.security.getRawValue().password })
      .subscribe({
        next: () => {
          this.savingPassword.set(false);
          this.security.reset();
          this.toast.success('Password updated');
        },
        error: (e) => {
          this.savingPassword.set(false);
          this.toast.error(e);
        },
      });
  }

  protected logout(): void {
    this.confirm
      .ask({
        title: 'Sign out?',
        message: 'You will need to sign in again to access your workspace.',
        confirmText: 'Sign out',
      })
      .subscribe((ok) => ok && this.auth.logout());
  }
}
