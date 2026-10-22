import { Injectable, inject, signal } from '@angular/core';
import { Observable, from, map, mergeMap, tap } from 'rxjs';
import { ApiClient, pageParams } from '../http/api-client';
import { Page, PageQuery, UpdateUserRequest, User } from '../models';

/**
 * User lookups plus a small in-memory directory so that ids coming from other
 * services (assignees, members, comment authors…) can be rendered as people.
 */
@Injectable({ providedIn: 'root' })
export class UsersService {
  private readonly api = inject(ApiClient);
  private readonly directory = signal<Record<string, User>>({});
  private readonly inflight = new Set<string>();

  search(keyword: string, q: PageQuery = {}): Observable<Page<User>> {
    return this.api
      .get<{ page: Page<User> }>('user-service', '/users', pageParams(q, { keyword }))
      .pipe(
        map((d) => d.page),
        tap((p) => this.remember(p.content)),
      );
  }

  getById(id: string): Observable<User> {
    return this.api.get<{ user: User }>('user-service', `/users/${id}`).pipe(
      map((d) => d.user),
      tap((u) => this.remember([u])),
    );
  }

  update(id: string, body: UpdateUserRequest): Observable<User> {
    return this.api.patch<{ user: User }>('user-service', `/users/${id}`, body).pipe(
      map((d) => d.user),
      tap((u) => this.remember([u])),
    );
  }

  /** Reactive read: returns undefined until {@link ensure} has resolved the id. */
  get(id: string | null | undefined): User | undefined {
    return id ? this.directory()[id] : undefined;
  }

  name(id: string | null | undefined, fallback = 'Unassigned'): string {
    if (!id) return fallback;
    return this.directory()[id]?.fullName ?? '…';
  }

  ensure(ids: Iterable<string | null | undefined>): void {
    const known = this.directory();
    const missing = [...new Set(ids)].filter(
      (id): id is string => !!id && !known[id] && !this.inflight.has(id),
    );
    if (!missing.length) return;
    missing.forEach((id) => this.inflight.add(id));

    from(missing)
      .pipe(mergeMap((id) => this.lookup(id), 4))
      .subscribe();
  }

  remember(users: User[]): void {
    if (!users.length) return;
    this.directory.update((d) => {
      const next = { ...d };
      for (const u of users) next[u.id] = u;
      return next;
    });
  }

  private lookup(id: string): Observable<unknown> {
    return new Observable((sub) => {
      const s = this.getById(id).subscribe({
        next: () => {
          this.inflight.delete(id);
          sub.complete();
        },
        error: () => {
          this.inflight.delete(id);
          this.remember([placeholder(id)]);
          sub.complete();
        },
      });
      return () => s.unsubscribe();
    });
  }
}

function placeholder(id: string): User {
  return {
    id,
    fullName: 'Unknown user',
    email: '',
    avatarUrl: null,
    role: 'USER',
    createdAt: '',
    updatedAt: '',
  };
}
