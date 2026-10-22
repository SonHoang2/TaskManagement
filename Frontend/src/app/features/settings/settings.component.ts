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
  template: `
    <div class="page" style="max-width: 900px">
      <div class="page-header">
        <div class="page-header__text">
          <h1>Settings</h1>
          <p>Manage your profile, security and appearance.</p>
        </div>
      </div>

      <div class="settings">
        <section class="settings__row">
          <div class="settings__intro">
            <h3>Profile</h3>
            <p class="muted small">How teammates see you across projects.</p>
          </div>
          <form class="card" [formGroup]="profile" (ngSubmit)="saveProfile()">
            <div class="card__body stack" style="gap: 16px">
              <div class="row" style="gap: 16px">
                <app-avatar
                  [userId]="auth.userId()"
                  [name]="profile.controls.fullName.value || auth.user()?.fullName"
                  [url]="avatarPreview() || null"
                  size="xl"
                />
                <div>
                  <strong>{{ auth.user()?.fullName }}</strong>
                  <div class="subtle small">
                    Member since {{ auth.user()?.createdAt | shortDate }}
                  </div>
                  <span
                    class="pill"
                    style="margin-top: 6px"
                    [class.pill--warning]="auth.isAdmin()"
                    >{{ auth.isAdmin() ? 'Admin' : 'User' }}</span
                  >
                </div>
              </div>
              <div class="form-grid">
                <div class="field">
                  <label for="st-name">Full name</label>
                  <input id="st-name" class="input" formControlName="fullName" maxlength="120" />
                </div>
                <div class="field">
                  <label for="st-email">Email</label>
                  <input id="st-email" class="input" type="email" formControlName="email" />
                  @if (profile.controls.email.touched && profile.controls.email.invalid) {
                    <span class="field__error">Enter a valid email.</span>
                  }
                </div>
                <div class="field full">
                  <label for="st-avatar">Avatar URL</label>
                  <input
                    id="st-avatar"
                    class="input"
                    formControlName="avatarUrl"
                    placeholder="https://…/me.png"
                    maxlength="500"
                  />
                  <span class="field__hint"
                    >Paste a link to a square image. Leave empty to use your initials.</span
                  >
                </div>
              </div>
            </div>
            <div class="card__footer">
              <button
                class="btn btn--ghost"
                type="button"
                (click)="resetProfile()"
                [disabled]="profile.pristine"
              >
                Reset
              </button>
              <button
                class="btn btn--primary"
                type="submit"
                [disabled]="profile.pristine || savingProfile()"
              >
                @if (savingProfile()) {
                  <span class="spinner"></span>
                }
                Save profile
              </button>
            </div>
          </form>
        </section>

        <section class="settings__row">
          <div class="settings__intro">
            <h3>Password</h3>
            <p class="muted small">Use at least 8 characters.</p>
          </div>
          <form class="card" [formGroup]="security" (ngSubmit)="savePassword()">
            <div class="card__body form-grid">
              <div class="field">
                <label for="st-pw">New password</label>
                <input
                  id="st-pw"
                  class="input"
                  type="password"
                  formControlName="password"
                  autocomplete="new-password"
                />
                @if (security.controls.password.touched && security.controls.password.invalid) {
                  <span class="field__error">At least 8 characters.</span>
                }
              </div>
              <div class="field">
                <label for="st-pw2">Confirm password</label>
                <input
                  id="st-pw2"
                  class="input"
                  type="password"
                  formControlName="confirm"
                  autocomplete="new-password"
                />
                @if (security.controls.confirm.touched && security.hasError('mismatch')) {
                  <span class="field__error">Passwords don't match.</span>
                }
              </div>
            </div>
            <div class="card__footer">
              <button class="btn btn--primary" type="submit" [disabled]="savingPassword()">
                Update password
              </button>
            </div>
          </form>
        </section>

        <section class="settings__row">
          <div class="settings__intro">
            <h3>Appearance</h3>
            <p class="muted small">Choose a theme, or follow your system.</p>
          </div>
          <div class="card card__body themes">
            @for (opt of themes; track opt.value) {
              <button
                type="button"
                class="theme-opt"
                [class.on]="theme.pref() === opt.value"
                (click)="theme.set(opt.value)"
              >
                <span class="theme-opt__preview theme-opt__preview--{{ opt.value }}"
                  ><span></span><span></span
                ></span>
                <span class="row"
                  ><span class="i">{{ opt.icon }}</span
                  >{{ opt.label }}</span
                >
              </button>
            }
          </div>
        </section>

        <section class="settings__row">
          <div class="settings__intro">
            <h3>Session</h3>
            <p class="muted small">Sign out of TaskFlow on this device.</p>
          </div>
          <div
            class="card card__body row"
            style="justify-content: space-between; flex-wrap: wrap; gap: 12px"
          >
            <span class="muted"
              >Signed in as <strong>{{ auth.user()?.email }}</strong></span
            >
            <button class="btn" type="button" (click)="logout()">
              <span class="i">logout</span>Sign out
            </button>
          </div>
        </section>
      </div>
    </div>
  `,
  styles: `
    .settings {
      display: flex;
      flex-direction: column;
      gap: 28px;
    }
    .settings__row {
      display: grid;
      grid-template-columns: 220px minmax(0, 1fr);
      gap: 24px;
      @media (max-width: 760px) {
        grid-template-columns: 1fr;
        gap: 10px;
      }
    }
    .settings__intro h3 {
      margin-bottom: 4px;
    }
    .themes {
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: 12px;
      @media (max-width: 520px) {
        grid-template-columns: 1fr;
      }
    }
    .theme-opt {
      display: flex;
      flex-direction: column;
      gap: 10px;
      padding: 10px;
      border-radius: var(--radius);
      border: 1.5px solid var(--border);
      background: var(--surface);
      cursor: pointer;
      text-align: left;
      font-weight: 550;
      font-size: 13px;
      &.on {
        border-color: var(--primary);
        box-shadow: 0 0 0 3px var(--ring);
      }
      &__preview {
        height: 64px;
        border-radius: 8px;
        padding: 8px;
        display: flex;
        flex-direction: column;
        gap: 6px;
        border: 1px solid var(--border);
        span {
          height: 10px;
          border-radius: 4px;
        }
        span:first-child {
          width: 60%;
        }
        span:last-child {
          width: 85%;
        }
        &--light {
          background: #f5f6f8;
          span {
            background: #dfe2ea;
          }
        }
        &--dark {
          background: #13161e;
          span {
            background: #2a3040;
          }
        }
        &--system {
          background: linear-gradient(110deg, #f5f6f8 50%, #13161e 50%);
          span {
            background: #8a93a6;
          }
        }
      }
    }
  `,
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
