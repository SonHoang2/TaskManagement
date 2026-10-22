import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Invitation } from '../../core/models';
import { ProjectsService } from '../../core/services/projects.service';
import { UsersService } from '../../core/services/users.service';
import { WorkspaceService } from '../../core/services/workspace.service';
import { ToastService } from '../../core/ui/toast.service';
import { colorFor } from '../../core/utils/format';
import { AvatarComponent } from '../../shared/components/avatar.component';
import {
  EmptyStateComponent,
  SkeletonComponent,
} from '../../shared/components/empty-state.component';
import { AgoPipe } from '../../shared/pipes';

@Component({
  selector: 'app-invitations',
  imports: [AvatarComponent, EmptyStateComponent, SkeletonComponent, AgoPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="page" style="max-width: 880px">
      <div class="page-header">
        <div class="page-header__text">
          <h1>Invitations</h1>
          <p>Projects you've been invited to join.</p>
        </div>
      </div>

      @if (loading()) {
        <app-skeleton [count]="3" [height]="76" />
      } @else {
        <div class="section-title">
          <h2>Pending</h2>
          <span class="count count--primary">{{ pending().length }}</span>
        </div>
        <div class="stack">
          @for (inv of pending(); track inv.id) {
            <article class="card invite">
              <span class="invite__icon" [style.background]="colorFor(inv.projectId)">{{
                projectName(inv.projectId).charAt(0)
              }}</span>
              <div class="grow">
                <h3 class="truncate">{{ projectName(inv.projectId) }}</h3>
                <div class="row subtle small" style="gap: 6px; margin-top: 2px">
                  <app-avatar [userId]="inv.invitedById" size="xs" />
                  <span
                    ><strong class="muted">{{ users.name(inv.invitedById) }}</strong> invited you ·
                    {{ inv.createdAt | ago }}</span
                  >
                </div>
              </div>
              <button
                class="btn btn--ghost"
                type="button"
                (click)="decide(inv, 'REJECT')"
                [disabled]="busy() === inv.id"
              >
                Decline
              </button>
              <button
                class="btn btn--primary"
                type="button"
                (click)="decide(inv, 'ACCEPT')"
                [disabled]="busy() === inv.id"
              >
                @if (busy() === inv.id) {
                  <span class="spinner"></span>
                } @else {
                  <span class="i">check</span>
                }
                Accept
              </button>
            </article>
          } @empty {
            <div class="card">
              <app-empty-state
                icon="mark_email_read"
                title="No pending invitations"
                text="When someone invites you to a project it'll appear here."
                [compact]="true"
              />
            </div>
          }
        </div>

        @if (history().length) {
          <div class="section-title" style="margin-top: 32px"><h2>History</h2></div>
          <div class="card">
            @for (inv of history(); track inv.id) {
              <div class="task-row" style="cursor: default">
                <span
                  class="invite__icon invite__icon--sm"
                  [style.background]="colorFor(inv.projectId)"
                  >{{ projectName(inv.projectId).charAt(0) }}</span
                >
                <span class="task-row__title">{{ projectName(inv.projectId) }}</span>
                <span class="subtle small hide-sm">from {{ users.name(inv.invitedById) }}</span>
                <span
                  class="pill"
                  [class.pill--done]="inv.status === 'ACCEPTED'"
                  [class.pill--danger]="inv.status === 'REJECTED'"
                >
                  {{ inv.status === 'ACCEPTED' ? 'Accepted' : 'Declined' }}
                </span>
                <span class="subtle small" style="width: 80px; text-align: right">{{
                  inv.respondedAt || inv.createdAt | ago
                }}</span>
              </div>
            }
          </div>
        }
      }
    </div>
  `,
  styles: `
    .invite {
      display: flex;
      align-items: center;
      gap: 14px;
      padding: 16px 18px;
      flex-wrap: wrap;
      h3 {
        font-size: 15px;
      }
    }
    .invite__icon {
      width: 42px;
      height: 42px;
      border-radius: 12px;
      display: grid;
      place-items: center;
      flex-shrink: 0;
      color: #fff;
      font-weight: 700;
      font-size: 17px;
      text-transform: uppercase;
      &--sm {
        width: 26px;
        height: 26px;
        border-radius: 8px;
        font-size: 12px;
      }
    }
    @media (max-width: 640px) {
      .hide-sm {
        display: none;
      }
    }
  `,
})
export class InvitationsComponent {
  private readonly projects = inject(ProjectsService);
  private readonly workspace = inject(WorkspaceService);
  private readonly toast = inject(ToastService);
  private readonly router = inject(Router);
  protected readonly users = inject(UsersService);
  protected readonly colorFor = colorFor;

  protected readonly invitations = signal<Invitation[]>([]);
  protected readonly loading = signal(true);
  protected readonly busy = signal<string | null>(null);
  private readonly names = signal<Record<string, string>>({});

  protected readonly pending = computed(() =>
    this.invitations().filter((i) => i.status === 'PENDING'),
  );
  protected readonly history = computed(() =>
    this.invitations().filter((i) => i.status !== 'PENDING'),
  );

  constructor() {
    this.projects.myInvitations().subscribe({
      next: (list) => {
        const sorted = [...list].sort((a, b) => b.createdAt.localeCompare(a.createdAt));
        this.invitations.set(sorted);
        this.users.ensure(sorted.map((i) => i.invitedById));
        this.loading.set(false);
        this.resolveNames(sorted);
      },
      error: (e) => {
        this.loading.set(false);
        this.toast.error(e);
      },
    });
  }

  protected projectName(id: string): string {
    return this.workspace.project(id)?.name ?? this.names()[id] ?? 'Project';
  }

  /** Invitees can't GET a project they haven't joined, so look names up in the public listing. */
  private resolveNames(list: Invitation[]): void {
    const unknown = list.some((i) => !this.workspace.project(i.projectId));
    if (!unknown) return;
    this.projects.all({ size: 100, sortBy: 'updatedat', sortDirection: 'desc' }).subscribe({
      next: (page) => this.names.set(Object.fromEntries(page.content.map((p) => [p.id, p.name]))),
      error: () => {},
    });
  }

  protected decide(inv: Invitation, decision: 'ACCEPT' | 'REJECT'): void {
    this.busy.set(inv.id);
    this.projects.decide(inv.id, decision).subscribe({
      next: (updated) => {
        this.busy.set(null);
        this.invitations.update((l) => l.map((i) => (i.id === inv.id ? updated : i)));
        this.workspace.load(true);
        if (decision === 'ACCEPT') {
          this.toast.success(`You joined ${this.projectName(inv.projectId)}`);
          this.router.navigate(['/projects', inv.projectId]);
        } else {
          this.toast.info('Invitation declined');
        }
      },
      error: (e) => {
        this.busy.set(null);
        this.toast.error(e);
      },
    });
  }
}
