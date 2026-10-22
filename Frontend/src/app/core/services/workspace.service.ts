import { Injectable, computed, effect, inject, signal, untracked } from '@angular/core';
import { catchError, from, map, mergeMap, of, switchMap, toArray } from 'rxjs';
import { AuthService } from '../auth/auth.service';
import { errorMessage } from '../http/api-client';
import { ProjectSummary, Task, TaskDetail } from '../models';
import { ProjectsService } from './projects.service';
import { TasksService } from './tasks.service';
import { UsersService } from './users.service';

const STALE_MS = 30_000;

/**
 * Cross-project view of "my work": the projects I belong to and every task in
 * them. Shared by the dashboard, My tasks and the command palette.
 */
@Injectable({ providedIn: 'root' })
export class WorkspaceService {
  private readonly projectsApi = inject(ProjectsService);
  private readonly tasksApi = inject(TasksService);
  private readonly users = inject(UsersService);
  private readonly auth = inject(AuthService);

  readonly projects = signal<ProjectSummary[]>([]);
  readonly tasks = signal<TaskDetail[]>([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly loaded = signal(false);
  readonly pendingInvitations = signal(0);

  readonly myTasks = computed(() => {
    const me = this.auth.userId();
    return this.tasks().filter((t) => t.assigneeId === me);
  });

  private lastLoad = 0;

  constructor() {
    // Drop cached data whenever the signed-in user changes (logout / switch account).
    let owner = this.auth.userId();
    effect(() => {
      const id = this.auth.userId();
      if (id !== owner) {
        owner = id;
        untracked(() => this.reset());
      }
    });
  }

  load(force = false): void {
    if (this.loading()) return;
    if (!force && this.loaded() && Date.now() - this.lastLoad < STALE_MS) return;

    this.loading.set(true);
    this.error.set(null);
    this.projectsApi
      .mine({ size: 100, sortBy: 'updatedat', sortDirection: 'desc' })
      .pipe(
        map((page) => page.content),
        switchMap((projects) => {
          this.projects.set(projects);
          if (!projects.length) return of([] as TaskDetail[][]);
          return from(projects).pipe(
            mergeMap(
              (p) =>
                this.tasksApi.byProject(p.id).pipe(
                  map((page) => page.content),
                  catchError(() => of([] as TaskDetail[])),
                ),
              3,
            ),
            toArray(),
          );
        }),
      )
      .subscribe({
        next: (chunks) => {
          const tasks = chunks.flat();
          this.tasks.set(tasks);
          this.users.ensure(tasks.map((t) => t.assigneeId));
          this.loaded.set(true);
          this.loading.set(false);
          this.lastLoad = Date.now();
        },
        error: (err) => {
          this.error.set(errorMessage(err));
          this.loading.set(false);
        },
      });
    this.refreshInvitations();
  }

  refreshInvitations(): void {
    this.projectsApi.myInvitations().subscribe({
      next: (list) =>
        this.pendingInvitations.set(list.filter((i) => i.status === 'PENDING').length),
      error: () => {},
    });
  }

  project(id: string | null | undefined): ProjectSummary | undefined {
    return id ? this.projects().find((p) => p.id === id) : undefined;
  }

  /** Keep the cache in sync after a local edit without refetching everything. */
  upsertTask(task: Task): void {
    this.tasks.update((list) => {
      const i = list.findIndex((t) => t.id === task.id);
      if (i === -1) return [...list, { ...task, comments: [], attachments: [], labels: [] }];
      const next = [...list];
      next[i] = { ...next[i], ...task };
      return next;
    });
  }

  removeTask(id: string): void {
    this.tasks.update((list) => list.filter((t) => t.id !== id));
  }

  invalidate(): void {
    this.lastLoad = 0;
  }

  reset(): void {
    this.projects.set([]);
    this.tasks.set([]);
    this.loaded.set(false);
    this.lastLoad = 0;
    this.pendingInvitations.set(0);
  }
}
