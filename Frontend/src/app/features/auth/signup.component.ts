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
  templateUrl: './signup.component.html',
  styleUrl: './auth.scss',
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
