import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import {
  AbstractControl,
  NonNullableFormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { ApiError, errorMessage } from '../../core/http/api-client';
import { AuthHeroComponent } from './auth-hero.component';

function matchPassword(group: AbstractControl): ValidationErrors | null {
  const { password, confirm } = group.value as { password: string; confirm: string };
  return confirm && password !== confirm ? { mismatch: true } : null;
}

@Component({
  selector: 'app-signup',
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
        <h2>Create your account</h2>
        <p>Start organising your projects in under a minute.</p>

        @if (error()) {
          <div class="alert" style="margin-bottom: 16px" role="alert">
            <span class="i">error</span>{{ error() }}
          </div>
        }

        <form [formGroup]="form" (ngSubmit)="submit()" novalidate>
          <div class="field">
            <label for="fullName">Full name</label>
            <div class="input-icon">
              <span class="i">badge</span>
              <input
                id="fullName"
                class="input"
                formControlName="fullName"
                autocomplete="name"
                placeholder="Jane Cooper"
              />
            </div>
            @if (form.controls.fullName.touched && form.controls.fullName.invalid) {
              <span class="field__error">Please enter your name.</span>
            }
          </div>

          <div class="field">
            <label for="email">Work email</label>
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
            } @else if (fieldErrors()['email']) {
              <span class="field__error">{{ fieldErrors()['email'] }}</span>
            }
          </div>

          <div class="field">
            <label for="password">Password</label>
            <div class="input-icon">
              <span class="i">lock</span>
              <input
                id="password"
                class="input"
                type="password"
                formControlName="password"
                autocomplete="new-password"
                placeholder="At least 8 characters"
              />
            </div>
            <div class="strength" [attr.data-level]="strength()">
              <span></span><span></span><span></span><span></span>
            </div>
            @if (form.controls.password.touched && form.controls.password.invalid) {
              <span class="field__error">Password must be at least 8 characters.</span>
            }
          </div>

          <div class="field">
            <label for="confirm">Confirm password</label>
            <div class="input-icon">
              <span class="i">lock_reset</span>
              <input
                id="confirm"
                class="input"
                type="password"
                formControlName="confirm"
                autocomplete="new-password"
              />
            </div>
            @if (form.controls.confirm.touched && form.hasError('mismatch')) {
              <span class="field__error">Passwords don't match.</span>
            }
          </div>

          <button class="btn btn--primary btn--lg btn--block" type="submit" [disabled]="loading()">
            @if (loading()) {
              <span class="spinner"></span>Creating account…
            } @else {
              Create account<span class="i">arrow_forward</span>
            }
          </button>
        </form>

        <p class="switch">Already have an account? <a routerLink="/login">Sign in</a></p>
      </div>
    </section>
  `,
})
export class SignupComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly fieldErrors = signal<Record<string, string>>({});

  protected readonly form = inject(NonNullableFormBuilder).group(
    {
      fullName: ['', [Validators.required, Validators.maxLength(120)]],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(8)]],
      confirm: ['', Validators.required],
    },
    { validators: matchPassword },
  );

  private readonly password = toSignal(this.form.controls.password.valueChanges, {
    initialValue: '',
  });
  protected readonly strength = computed(() => {
    const p = this.password();
    if (!p) return 0;
    let s = p.length >= 8 ? 1 : 0;
    if (p.length >= 12) s++;
    if (/[A-Z]/.test(p) && /[a-z]/.test(p)) s++;
    if (/\d/.test(p) && /[^A-Za-z0-9]/.test(p)) s++;
    return Math.max(1, Math.min(4, s));
  });

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    this.fieldErrors.set({});
    const { fullName, email, password } = this.form.getRawValue();
    this.auth.signup(fullName.trim(), email.trim(), password).subscribe({
      next: () => this.router.navigate(['/dashboard']),
      error: (err) => {
        this.loading.set(false);
        this.error.set(errorMessage(err));
        if (err instanceof ApiError) this.fieldErrors.set(err.fieldErrors);
      },
    });
  }
}
