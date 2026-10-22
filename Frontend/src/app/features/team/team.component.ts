import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inject,
  signal,
  untracked,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import {
  Subject,
  catchError,
  debounceTime,
  from,
  map,
  mergeMap,
  of,
  startWith,
  switchMap,
  toArray,
} from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { MemberRole, Page, User } from '../../core/models';
import { ProjectsService } from '../../core/services/projects.service';
import { UsersService } from '../../core/services/users.service';
import { WorkspaceService } from '../../core/services/workspace.service';
import { colorFor } from '../../core/utils/format';
import { AvatarComponent } from '../../shared/components/avatar.component';
import {
  EmptyStateComponent,
  SkeletonComponent,
} from '../../shared/components/empty-state.component';
import { ShortDatePipe } from '../../shared/pipes';

interface Teammate {
  userId: string;
  projects: { id: string; name: string; role: MemberRole }[];
}

@Component({
  selector: 'app-team',
  imports: [
    FormsModule,
    RouterLink,
    AvatarComponent,
    EmptyStateComponent,
    SkeletonComponent,
    ShortDatePipe,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './team.component.html',
  styleUrl: './team.component.scss',
})
export class TeamComponent {
  private readonly ws = inject(WorkspaceService);
  private readonly projectsApi = inject(ProjectsService);
  private readonly auth = inject(AuthService);
  protected readonly users = inject(UsersService);
  protected readonly colorFor = colorFor;

  protected readonly tab = signal<'team' | 'directory'>('team');
  protected readonly filter = signal('');
  protected readonly teammates = signal<Teammate[]>([]);
  protected readonly loadingTeam = signal(true);

  protected readonly query = signal('');
  protected readonly dirPage = signal(0);
  protected readonly directory = signal<Page<User> | null>(null);
  protected readonly loadingDir = signal(false);
  private readonly dir$ = new Subject<void>();

  protected readonly openTasks = computed(() => {
    const m = new Map<string, number>();
    for (const t of this.ws.tasks())
      if (t.assigneeId && t.status !== 'DONE') m.set(t.assigneeId, (m.get(t.assigneeId) ?? 0) + 1);
    return m;
  });

  protected readonly visibleTeam = computed(() => {
    const q = this.filter().trim().toLowerCase();
    const me = this.auth.userId();
    return this.teammates()
      .filter((t) => {
        if (!q) return true;
        const u = this.users.get(t.userId);
        return !!u && (u.fullName.toLowerCase().includes(q) || u.email.toLowerCase().includes(q));
      })
      .sort(
        (a, b) =>
          Number(b.userId === me) - Number(a.userId === me) ||
          b.projects.length - a.projects.length,
      );
  });

  constructor() {
    this.ws.load();
    effect(() => {
      const projects = this.ws.projects();
      if (this.ws.loaded())
        untracked(() => this.loadTeam(projects.map((p) => ({ id: p.id, name: p.name }))));
    });

    this.dir$
      .pipe(
        startWith(undefined),
        debounceTime(250),
        switchMap(() => {
          this.loadingDir.set(true);
          return this.users
            .search(this.query().trim(), { page: this.dirPage(), size: 24, sort: 'fullName,asc' })
            .pipe(catchError(() => of(null)));
        }),
        takeUntilDestroyed(),
      )
      .subscribe((p) => {
        this.directory.set(p);
        this.loadingDir.set(false);
      });
  }

  private loadTeam(projects: { id: string; name: string }[]): void {
    if (!projects.length) {
      this.teammates.set([]);
      this.loadingTeam.set(false);
      return;
    }
    from(projects)
      .pipe(
        mergeMap(
          (p) =>
            this.projectsApi.members(p.id).pipe(
              map((members) => ({ p, members })),
              catchError(() => of({ p, members: [] })),
            ),
          3,
        ),
        toArray(),
      )
      .subscribe((rows) => {
        const byUser = new Map<string, Teammate>();
        for (const { p, members } of rows) {
          for (const m of members) {
            const t = byUser.get(m.userId) ?? { userId: m.userId, projects: [] };
            t.projects.push({ id: p.id, name: p.name, role: m.role });
            byUser.set(m.userId, t);
          }
        }
        this.users.ensure(byUser.keys());
        this.teammates.set([...byUser.values()]);
        this.loadingTeam.set(false);
      });
  }

  protected search(q: string): void {
    this.query.set(q);
    this.dirPage.set(0);
    this.dir$.next();
  }

  protected goTo(page: number): void {
    this.dirPage.set(page);
    this.dir$.next();
  }

  protected isMe(id: string): boolean {
    return id === this.auth.userId();
  }
}
