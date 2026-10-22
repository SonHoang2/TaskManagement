import { Injectable, computed, inject, signal } from '@angular/core';
import { catchError, forkJoin, of } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { errorMessage } from '../../core/http/api-client';
import { Label, Project, ProjectMember, Sprint, TaskDetail, TaskStatus } from '../../core/models';
import { ProjectsService } from '../../core/services/projects.service';
import { SprintsService } from '../../core/services/sprints.service';
import { TasksService } from '../../core/services/tasks.service';
import { UsersService } from '../../core/services/users.service';
import { WorkspaceService } from '../../core/services/workspace.service';
import { ToastService } from '../../core/ui/toast.service';
import { percent } from '../../core/utils/format';

/** State for one open project, shared by its tabs (board, list, sprints…). */
@Injectable()
export class ProjectContext {
  private readonly projectsApi = inject(ProjectsService);
  private readonly tasksApi = inject(TasksService);
  private readonly sprintsApi = inject(SprintsService);
  private readonly users = inject(UsersService);
  private readonly workspace = inject(WorkspaceService);
  private readonly toast = inject(ToastService);
  private readonly auth = inject(AuthService);

  readonly id = signal('');
  readonly project = signal<Project | null>(null);
  readonly members = signal<ProjectMember[]>([]);
  readonly labels = signal<Label[]>([]);
  readonly sprints = signal<Sprint[]>([]);
  readonly tasks = signal<TaskDetail[]>([]);
  readonly loading = signal(true);
  readonly tasksLoading = signal(true);
  readonly error = signal<string | null>(null);

  readonly myRole = computed(
    () => this.members().find((m) => m.userId === this.auth.userId())?.role ?? null,
  );
  readonly canManage = computed(() => this.myRole() === 'OWNER' || this.myRole() === 'ADMIN');
  readonly labelMap = computed(() => new Map(this.labels().map((l) => [l.id, l])));
  readonly activeSprint = computed(() => this.sprints().find((s) => s.status === 'ACTIVE') ?? null);
  readonly stats = computed(() => {
    const t = this.tasks();
    const done = t.filter((x) => x.status === 'DONE').length;
    return {
      total: t.length,
      todo: t.filter((x) => x.status === 'TODO').length,
      inProgress: t.filter((x) => x.status === 'IN_PROGRESS').length,
      done,
      pct: percent(done, t.length),
    };
  });

  load(id: string): void {
    this.id.set(id);
    this.loading.set(true);
    this.error.set(null);
    this.project.set(null);
    this.tasks.set([]);
    this.projectsApi.get(id).subscribe({
      next: (p) => {
        this.project.set(p);
        this.loading.set(false);
      },
      error: (e) => {
        this.error.set(errorMessage(e));
        this.loading.set(false);
      },
    });
    this.reloadMembers();
    this.reloadLabels();
    this.reloadSprints();
    this.reloadTasks();
  }

  reloadTasks(): void {
    this.tasksLoading.set(true);
    this.tasksApi.byProject(this.id()).subscribe({
      next: (page) => {
        this.tasks.set(page.content);
        this.users.ensure(page.content.flatMap((t) => [t.assigneeId, t.reporterId]));
        this.tasksLoading.set(false);
      },
      error: (e) => {
        this.tasksLoading.set(false);
        this.toast.error(e);
      },
    });
  }

  reloadMembers(): void {
    this.projectsApi.members(this.id()).subscribe({
      next: (m) => {
        this.members.set(m);
        this.users.ensure(m.map((x) => x.userId));
      },
      error: () => {},
    });
  }

  reloadLabels(): void {
    this.projectsApi
      .labels(this.id())
      .subscribe({ next: (l) => this.labels.set(l), error: () => {} });
  }

  reloadSprints(): void {
    this.sprintsApi
      .list(this.id())
      .pipe(catchError(() => of([] as Sprint[])))
      .subscribe((s) => this.sprints.set(s));
  }

  /** Optimistically moves a task to another column. */
  moveTask(task: TaskDetail, status: TaskStatus): void {
    if (task.status === status) return;
    const prev = task.status;
    this.patchLocal(task.id, { status });
    this.tasksApi.update(task, { status }, this.auth.userId()).subscribe({
      next: (t) => {
        this.patchLocal(t.id, t);
        this.workspace.upsertTask(t);
      },
      error: (e) => {
        this.patchLocal(task.id, { status: prev });
        this.toast.error(e);
      },
    });
  }

  patchLocal(id: string, patch: Partial<TaskDetail>): void {
    this.tasks.update((list) => list.map((t) => (t.id === id ? { ...t, ...patch } : t)));
  }

  /** Task ids per sprint, used for sprint filters and progress. */
  sprintTaskIds(sprintIds: string[]) {
    return forkJoin(
      Object.fromEntries(
        sprintIds.map((id) => [
          id,
          this.sprintsApi.tasksOfSprint(id).pipe(catchError(() => of([]))),
        ]),
      ),
    );
  }
}
