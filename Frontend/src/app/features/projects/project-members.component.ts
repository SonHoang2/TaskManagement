import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inject,
  signal,
  untracked,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Subject, debounceTime, distinctUntilChanged, switchMap, of, catchError } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Invitation, User } from '../../core/models';
import { ProjectsService } from '../../core/services/projects.service';
import { UsersService } from '../../core/services/users.service';
import { ToastService } from '../../core/ui/toast.service';
import { AvatarComponent } from '../../shared/components/avatar.component';
import { RoleBadgeComponent } from '../../shared/components/badges';
import { EmptyStateComponent } from '../../shared/components/empty-state.component';
import { AgoPipe, ShortDatePipe } from '../../shared/pipes';
import { ProjectContext } from './project-context';

@Component({
  selector: 'app-project-members',
  imports: [
    FormsModule,
    AvatarComponent,
    RoleBadgeComponent,
    EmptyStateComponent,
    AgoPipe,
    ShortDatePipe,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="page" style="padding-top: 24px">
      <div class="members-grid">
        <section class="card">
          <header class="card__header">
            <h2>Members</h2>
            <span class="count">{{ ctx.members().length }}</span>
          </header>
          <ul class="people">
            @for (m of ctx.members(); track m.userId) {
              <li>
                <app-avatar [userId]="m.userId" size="md" />
                <div class="grow">
                  <div class="truncate" style="font-weight: 600">{{ users.name(m.userId) }}</div>
                  <div class="truncate subtle small">{{ users.get(m.userId)?.email }}</div>
                </div>
                <span class="subtle small hide-sm">Joined {{ m.joinedAt | shortDate }}</span>
                <app-role [value]="m.role" />
              </li>
            }
          </ul>
        </section>

        <aside class="stack">
          @if (ctx.canManage()) {
            <section class="card">
              <header class="card__header">
                <span class="i" style="color: var(--primary)">person_add</span>
                <h2>Invite people</h2>
              </header>
              <div class="card__body">
                <div class="input-icon">
                  <span class="i">search</span>
                  <input
                    class="input"
                    placeholder="Search by name or email…"
                    [ngModel]="query()"
                    (ngModelChange)="onQuery($event)"
                  />
                </div>
                <div class="results">
                  @if (searching()) {
                    <div class="subtle small" style="padding: 10px 2px">Searching…</div>
                  }
                  @for (u of results(); track u.id) {
                    <div class="result">
                      <app-avatar
                        [userId]="u.id"
                        [name]="u.fullName"
                        [url]="u.avatarUrl"
                        size="sm"
                      />
                      <div class="grow">
                        <div class="truncate" style="font-weight: 550">{{ u.fullName }}</div>
                        <div class="truncate subtle small">{{ u.email }}</div>
                      </div>
                      @if (isMember(u.id)) {
                        <span class="pill pill--done">Member</span>
                      } @else if (isPending(u.id)) {
                        <span class="pill pill--warning">Invited</span>
                      } @else {
                        <button
                          class="btn btn--soft btn--sm"
                          type="button"
                          (click)="invite(u)"
                          [disabled]="inviting() === u.id"
                        >
                          Invite
                        </button>
                      }
                    </div>
                  } @empty {
                    @if (query().trim().length >= 2 && !searching()) {
                      <div class="subtle small" style="padding: 10px 2px">No users found.</div>
                    } @else if (!query().trim()) {
                      <div class="subtle small" style="padding: 10px 2px">
                        Invitees get a notification and can accept from their Invitations page.
                      </div>
                    }
                  }
                </div>
              </div>
            </section>
          }

          <section class="card">
            <header class="card__header">
              <h2>Invitations</h2>
              <span class="count">{{ invitations().length }}</span>
            </header>
            @if (invitations().length) {
              <ul class="people">
                @for (inv of invitations(); track inv.id) {
                  <li>
                    <app-avatar [userId]="inv.inviteeId" size="sm" />
                    <div class="grow">
                      <div class="truncate" style="font-weight: 550">
                        {{ users.name(inv.inviteeId) }}
                      </div>
                      <div class="subtle small">
                        by {{ users.name(inv.invitedById) }} · {{ inv.createdAt | ago }}
                      </div>
                    </div>
                    <span
                      class="pill"
                      [class.pill--warning]="inv.status === 'PENDING'"
                      [class.pill--done]="inv.status === 'ACCEPTED'"
                      [class.pill--danger]="inv.status === 'REJECTED'"
                    >
                      {{ inv.status.charAt(0) + inv.status.slice(1).toLowerCase() }}
                    </span>
                  </li>
                }
              </ul>
            } @else {
              <app-empty-state icon="mail" title="No invitations" [compact]="true" />
            }
          </section>
        </aside>
      </div>
    </div>
  `,
  styles: `
    .members-grid {
      display: grid;
      grid-template-columns: minmax(0, 1.4fr) minmax(0, 1fr);
      gap: 20px;
      align-items: start;
      @media (max-width: 980px) {
        grid-template-columns: 1fr;
      }
    }
    .people {
      list-style: none;
      margin: 0;
      padding: 6px 0;
      li {
        display: flex;
        align-items: center;
        gap: 12px;
        padding: 10px 20px;
      }
      li + li {
        border-top: 1px solid var(--border);
      }
    }
    .results {
      margin-top: 8px;
      display: flex;
      flex-direction: column;
    }
    .result {
      display: flex;
      align-items: center;
      gap: 10px;
      padding: 8px 2px;
      border-bottom: 1px solid var(--border);
      &:last-child {
        border-bottom: 0;
      }
    }
    @media (max-width: 640px) {
      .hide-sm {
        display: none;
      }
    }
  `,
})
export class ProjectMembersComponent {
  protected readonly ctx = inject(ProjectContext);
  protected readonly users = inject(UsersService);
  private readonly projects = inject(ProjectsService);
  private readonly toast = inject(ToastService);

  protected readonly invitations = signal<Invitation[]>([]);
  protected readonly query = signal('');
  protected readonly results = signal<User[]>([]);
  protected readonly searching = signal(false);
  protected readonly inviting = signal<string | null>(null);
  private readonly memberIds = computed(() => new Set(this.ctx.members().map((m) => m.userId)));
  private readonly query$ = new Subject<string>();

  constructor() {
    effect(() => {
      const id = this.ctx.id();
      if (id) untracked(() => this.loadInvitations());
    });

    this.query$
      .pipe(
        debounceTime(250),
        distinctUntilChanged(),
        switchMap((q) => {
          if (q.length < 2) return of([] as User[]);
          this.searching.set(true);
          return this.users.search(q, { size: 8 }).pipe(
            switchMap((p) => of(p.content)),
            catchError(() => of([] as User[])),
          );
        }),
        takeUntilDestroyed(),
      )
      .subscribe((list) => {
        this.searching.set(false);
        this.results.set(list);
      });
  }

  protected onQuery(q: string): void {
    this.query.set(q);
    this.query$.next(q.trim());
  }

  protected isMember(id: string): boolean {
    return this.memberIds().has(id);
  }

  protected isPending(id: string): boolean {
    return this.invitations().some((i) => i.inviteeId === id && i.status === 'PENDING');
  }

  protected invite(u: User): void {
    this.inviting.set(u.id);
    this.projects.invite(this.ctx.id(), u.id).subscribe({
      next: (inv) => {
        this.inviting.set(null);
        this.invitations.update((l) => [inv, ...l]);
        this.toast.success(`Invitation sent to ${u.fullName}`);
      },
      error: (e) => {
        this.inviting.set(null);
        this.toast.error(e);
      },
    });
  }

  private loadInvitations(): void {
    this.projects.invitations(this.ctx.id()).subscribe({
      next: (list) => {
        const sorted = [...list].sort((a, b) => b.createdAt.localeCompare(a.createdAt));
        this.invitations.set(sorted);
        this.users.ensure(sorted.flatMap((i) => [i.inviteeId, i.invitedById]));
      },
      error: () => {},
    });
  }
}
