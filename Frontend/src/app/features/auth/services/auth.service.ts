import { Injectable } from '@angular/core';
import { HttpService } from '../../../core/services/http.service';
import { Observable, BehaviorSubject, tap } from 'rxjs';
import { Router } from '@angular/router';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface SignupRequest {
  email: string;
  password: string;
  name: string;
}

export interface User {
  id: string;
  fullName: string;
  email: string;
  avatarUrl: string | null;
  role: string;
  createdAt: string;
  updatedAt: string;
}

export interface AuthResponse {
  accessToken: string;
  tokenType: string;
  expiresInMs: number;
  user: User;
}

export interface JSendResponse<T> {
  status: 'success' | 'error' | 'fail';
  data?: T;
  message?: string;
}

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private readonly TOKEN_KEY = 'jwt_token';
  private readonly USER_ID_KEY = 'user_id';
  private readonly USER_EMAIL_KEY = 'user_email';
  private readonly USER_NAME_KEY = 'user_name';
  private readonly USER_AVATAR_KEY = 'user_avatar';
  private readonly USER_ROLE_KEY = 'user_role';

  private isAuthenticatedSubject = new BehaviorSubject<boolean>(this.hasToken());
  isAuthenticated$ = this.isAuthenticatedSubject.asObservable();

  constructor(
    private httpService: HttpService,
    private router: Router,
  ) {}

  login(credentials: LoginRequest): Observable<JSendResponse<AuthResponse>> {
    return this.httpService
      .post<JSendResponse<AuthResponse>>('/user-service/auth/login', credentials)
      .pipe(
        tap((response) => {
          if (response.status === 'success' && response.data) {
            this.setSession(response.data);
          }
        }),
      );
  }

  signup(userData: SignupRequest): Observable<JSendResponse<AuthResponse>> {
    return this.httpService
      .post<JSendResponse<AuthResponse>>('/user-service/auth/signup', userData)
      .pipe(
        tap((response) => {
          if (response.status === 'success' && response.data) {
            this.setSession(response.data);
          }
        }),
      );
  }

  logout(): void {
    this.clearSession();
    this.router.navigate(['/login']);
  }

  private setSession(authData: AuthResponse): void {
    localStorage.setItem(this.TOKEN_KEY, authData.accessToken);
    localStorage.setItem(this.USER_ID_KEY, authData.user.id);
    localStorage.setItem(this.USER_EMAIL_KEY, authData.user.email);
    localStorage.setItem(this.USER_NAME_KEY, authData.user.fullName);
    localStorage.setItem(this.USER_AVATAR_KEY, authData.user.avatarUrl || '');
    localStorage.setItem(this.USER_ROLE_KEY, authData.user.role);
    this.isAuthenticatedSubject.next(true);
  }

  private clearSession(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.USER_ID_KEY);
    localStorage.removeItem(this.USER_EMAIL_KEY);
    localStorage.removeItem(this.USER_NAME_KEY);
    localStorage.removeItem(this.USER_AVATAR_KEY);
    localStorage.removeItem(this.USER_ROLE_KEY);
    this.isAuthenticatedSubject.next(false);
  }

  private hasToken(): boolean {
    return !!localStorage.getItem(this.TOKEN_KEY);
  }

  getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  getUserId(): string | null {
    return localStorage.getItem(this.USER_ID_KEY);
  }

  getUserEmail(): string | null {
    return localStorage.getItem(this.USER_EMAIL_KEY);
  }

  getUserName(): string | null {
    return localStorage.getItem(this.USER_NAME_KEY);
  }

  getUserAvatar(): string | null {
    const avatar = localStorage.getItem(this.USER_AVATAR_KEY);
    return avatar && avatar !== '' ? avatar : null;
  }

  getUserRole(): string | null {
    return localStorage.getItem(this.USER_ROLE_KEY);
  }

  isAdmin(): boolean {
    return this.getUserRole() === 'ADMIN';
  }

  isLoggedIn(): boolean {
    return this.isAuthenticatedSubject.value;
  }
}
