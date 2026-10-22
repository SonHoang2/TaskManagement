import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { ApiClient } from '../http/api-client';
import { LoginResponse, User } from '../models';

interface Session {
  token: string;
  expiresAt: number;
  user: User;
}

const STORAGE_KEY = 'tm.session';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly api = inject(ApiClient);
  private readonly router = inject(Router);

  private readonly session = signal<Session | null>(readSession());

  readonly user = computed(() => this.session()?.user ?? null);
  readonly userId = computed(() => this.session()?.user.id ?? '');
  readonly isAdmin = computed(() => this.session()?.user.role === 'ADMIN');

  get token(): string | null {
    return this.session()?.token ?? null;
  }

  isAuthenticated(): boolean {
    const s = this.session();
    if (!s) return false;
    if (Date.now() >= s.expiresAt) {
      this.clear();
      return false;
    }
    return true;
  }

  login(email: string, password: string): Observable<LoginResponse> {
    return this.api
      .post<LoginResponse>('user-service', '/auth/login', { email, password })
      .pipe(tap((res) => this.store(res)));
  }

  signup(fullName: string, email: string, password: string): Observable<LoginResponse> {
    return this.api
      .post<LoginResponse>('user-service', '/auth/signup', { fullName, email, password })
      .pipe(tap((res) => this.store(res)));
  }

  logout(redirect = true): void {
    if (this.session()) {
      // Best effort: the backend is stateless, so failures don't matter.
      this.api.post('user-service', '/auth/logout', {}).subscribe({ error: () => {} });
    }
    this.clear();
    if (redirect) this.router.navigate(['/login']);
  }

  /** Called when the gateway rejects our token. */
  expire(): void {
    this.clear();
    this.router.navigate(['/login'], { queryParams: { returnUrl: this.router.url, expired: 1 } });
  }

  updateUser(user: User): void {
    const s = this.session();
    if (!s) return;
    this.persist({ ...s, user });
  }

  private store(res: LoginResponse): void {
    this.persist({
      token: res.accessToken,
      expiresAt: Date.now() + (res.expiresInMs || 3_600_000),
      user: res.user,
    });
  }

  private persist(s: Session): void {
    this.session.set(s);
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(s));
    } catch {
      /* storage unavailable: keep the in-memory session */
    }
  }

  private clear(): void {
    this.session.set(null);
    try {
      localStorage.removeItem(STORAGE_KEY);
    } catch {
      /* ignore */
    }
  }
}

function readSession(): Session | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return null;
    const s = JSON.parse(raw) as Session;
    return s.token && s.user && s.expiresAt > Date.now() ? s : null;
  } catch {
    return null;
  }
}
