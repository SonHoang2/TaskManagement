import { ChangeDetectionStrategy, Component, inject, input, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { errorMessage } from '../../core/http/api-client';
import { AuthHeroComponent } from './auth-hero.component';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, RouterLink, AuthHeroComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './auth.scss',
  template: `
    <app-auth-hero />
    <section class="panel">
      <div class="form-card">
        <div class="mobile-brand">
          <span class="mark"><span class="i fill">bolt</span></span
          >TaskFlow
        </div>
        <h2>Welcome back</h2>
        <p>Sign in to continue to your workspace.</p>

        @if (expired()) {
          <div class="alert alert--info" style="margin-bottom: 16px">
            <span class="i">schedule</span>Your session expired. Please sign in again.
          </div>
        }
        @if (error()) {
          <div class="alert" style="margin-bottom: 16px" role="alert">
            <span class="i">error</span>{{ error() }}
          </div>
        }

        <form [formGroup]="form" (ngSubmit)="submit()" novalidate>
          <div class="field">
            <label for="email">Email</label>
            <div class="input-icon">
              <span class="i">mail</span>
              <input
                id="email"
                class="input"
                type="email"
                formControlName="email"
                autocomplete="email"
                placeholder="you@company.com"
              />
            </div>
            @if (form.controls.email.touched && form.controls.email.invalid) {
              <span class="field__error">Enter a valid email address.</span>
            }
          </div>

          <div class="field">
            <label for="password">Password</label>
            <div class="input-icon">
              <span class="i">lock</span>
              <input
                id="password"
                class="input"
                [type]="showPassword() ? 'text' : 'password'"
                formControlName="password"
                autocomplete="current-password"
                placeholder="••••••••"
              />
              <button
                type="button"
                class="btn btn--ghost btn--icon btn--sm input-icon__end"
                (click)="showPassword.set(!showPassword())"
                [attr.aria-label]="showPassword() ? 'Hide password' : 'Show password'"
              >
                <span class="i">{{ showPassword() ? 'visibility_off' : 'visibility' }}</span>
              </button>
            </div>
            @if (form.controls.password.touched && form.controls.password.invalid) {
              <span class="field__error">Password is required.</span>
            }
          </div>

          <button class="btn btn--primary btn--lg btn--block" type="submit" [disabled]="loading()">
            @if (loading()) {
              <span class="spinner"></span>Signing in…
            } @else {
              Sign in<span class="i">arrow_forward</span>
            }
          </button>
        </form>

        <p class="switch">New to TaskFlow? <a routerLink="/signup">Create an account</a></p>
      </div>
    </section>
  `,
})
export class LoginComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly returnUrl = input<string>('/dashboard');
  readonly expired = input<string | undefined>(undefined);

  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly showPassword = signal(false);

  protected readonly form = inject(NonNullableFormBuilder).group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required],
  });

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    const { email, password } = this.form.getRawValue();
    this.auth.login(email.trim(), password).subscribe({
      next: () => this.router.navigateByUrl(this.safeReturnUrl()),
      error: (err) => {
        this.loading.set(false);
        this.error.set(errorMessage(err));
      },
    });
  }

  private safeReturnUrl(): string {
    const url = this.returnUrl() || '/dashboard';
    return url.startsWith('/') && !url.startsWith('//') && !url.startsWith('/login')
      ? url
      : '/dashboard';
  }
}
