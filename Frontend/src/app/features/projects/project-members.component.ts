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
  templateUrl: './project-members.component.html',
  styleUrl: './project-members.component.scss',
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
