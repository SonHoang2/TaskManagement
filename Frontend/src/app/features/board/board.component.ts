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
import { CdkDragDrop, DragDropModule } from '@angular/cdk/drag-drop';
import { MatTooltipModule } from '@angular/material/tooltip';
import { AuthService } from '../../core/auth/auth.service';
import { TaskDetail, TaskPriority, TaskStatus } from '../../core/models';
import { SprintsService } from '../../core/services/sprints.service';
import { TasksService } from '../../core/services/tasks.service';
import { UsersService } from '../../core/services/users.service';
import { WorkspaceService } from '../../core/services/workspace.service';
import { ToastService } from '../../core/ui/toast.service';
import {
  PRIORITIES,
  PRIORITY_META,
  STATUSES,
  STATUS_META,
  compareTasks,
  taskKey,
} from '../../core/utils/format';
import { AvatarComponent } from '../../shared/components/avatar.component';
import { DueDateComponent, PriorityBadgeComponent } from '../../shared/components/badges';
import { EmptyStateComponent } from '../../shared/components/empty-state.component';
import { DialogsService } from '../../shared/dialogs.service';
import { ProjectContext } from '../projects/project-context';

@Component({
  selector: 'app-board',
  imports: [
    FormsModule,
    DragDropModule,
    MatTooltipModule,
    AvatarComponent,
    PriorityBadgeComponent,
    DueDateComponent,
    EmptyStateComponent,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './board.component.html',
  styleUrl: './board.component.scss',
})
export class BoardComponent {
  protected readonly ctx = inject(ProjectContext);
  protected readonly users = inject(UsersService);
  private readonly dialogs = inject(DialogsService);
  private readonly tasksApi = inject(TasksService);
  private readonly sprintsApi = inject(SprintsService);
  private readonly workspace = inject(WorkspaceService);
  private readonly toast = inject(ToastService);
  private readonly auth = inject(AuthService);

  protected readonly statuses = STATUSES;
  protected readonly statusMeta = STATUS_META;
  protected readonly priorities = PRIORITIES;
  protected readonly priorityMeta = PRIORITY_META;
  protected readonly taskKey = taskKey;
  protected readonly me = this.auth.userId;

  protected readonly search = signal('');
  protected readonly assignee = signal('');
  protected readonly priority = signal<TaskPriority | ''>('');
  protected readonly label = signal('');
  protected readonly sprint = signal('');
  protected readonly sprintTasks = signal<Set<string> | null>(null);
  protected readonly quickAdd = signal<TaskStatus | null>(null);
  protected readonly quickTitle = signal('');

  protected readonly hasFilters = computed(
    () => !!(this.search() || this.assignee() || this.priority() || this.label() || this.sprint()),
  );

  protected readonly filtered = computed(() => {
    const q = this.search().trim().toLowerCase();
    const a = this.assignee();
    const p = this.priority();
    const l = this.label();
    const inSprint = this.sprintTasks();
    return this.ctx.tasks().filter((t) => {
      if (q && !t.title.toLowerCase().includes(q) && taskKey(t).toLowerCase() !== q) return false;
      if (a === 'none' ? !!t.assigneeId : a && t.assigneeId !== a) return false;
      if (p && t.priority !== p) return false;
      if (l && !t.labels.some((x) => x.labelId === l)) return false;
      if (inSprint && !inSprint.has(t.id)) return false;
      return true;
    });
  });

  protected readonly columns = computed(() => {
    const groups: Record<TaskStatus, TaskDetail[]> = { TODO: [], IN_PROGRESS: [], DONE: [] };
    for (const t of this.filtered()) groups[t.status]?.push(t);
    for (const s of STATUSES) groups[s].sort(compareTasks);
    return STATUSES.map((s) => ({ status: s, meta: STATUS_META[s], tasks: groups[s] }));
  });

  constructor() {
    // Default the sprint filter to the active sprint once sprints are known.
    let initialised = false;
    effect(() => {
      const active = this.ctx.activeSprint();
      if (!initialised && this.ctx.sprints().length) {
        initialised = true;
        if (active) untracked(() => this.setSprint(active.id));
      }
    });
  }

  protected setSprint(id: string): void {
    this.sprint.set(id);
    this.sprintTasks.set(null);
    if (!id) return;
    this.sprintsApi.tasksOfSprint(id).subscribe({
      next: (list) => this.sprintTasks.set(new Set(list.map((x) => x.taskId))),
      error: (e) => this.toast.error(e),
    });
  }

  protected clearFilters(): void {
    this.search.set('');
    this.assignee.set('');
    this.priority.set('');
    this.label.set('');
    this.setSprint('');
  }

  protected drop(e: CdkDragDrop<TaskStatus, TaskStatus, TaskDetail>): void {
    if (e.previousContainer === e.container) return;
    this.ctx.moveTask(e.item.data, e.container.data);
  }

  protected open(t: TaskDetail): void {
    this.dialogs.taskDetail(t.id).subscribe((changed) => changed && this.ctx.reloadTasks());
  }

  protected startQuickAdd(status: TaskStatus): void {
    this.quickTitle.set('');
    this.quickAdd.set(status);
  }

  protected submitQuickAdd(status: TaskStatus): void {
    const title = this.quickTitle().trim();
    if (!title) {
      this.quickAdd.set(null);
      return;
    }
    const me = this.me();
    const sprintId = this.sprint();
    this.tasksApi
      .create({ projectId: this.ctx.id(), title, status, priority: 'MEDIUM', reporterId: me }, me)
      .subscribe({
        next: (task) => {
          this.quickTitle.set('');
          this.ctx.tasks.update((l) => [
            ...l,
            { ...task, comments: [], attachments: [], labels: [] },
          ]);
          this.workspace.upsertTask(task);
          if (sprintId) {
            this.sprintsApi.addTask(sprintId, task.id).subscribe({
              next: () => this.sprintTasks.update((s) => new Set([...(s ?? []), task.id])),
              error: () => {},
            });
          }
        },
        error: (e) => this.toast.error(e),
      });
  }

  protected newTask(status: TaskStatus = 'TODO'): void {
    this.dialogs
      .taskForm({ projectId: this.ctx.id(), status, sprintId: this.sprint() || undefined })
      .subscribe((t) => {
        if (!t) return;
        this.ctx.reloadTasks();
        if (this.sprint()) this.setSprint(this.sprint());
      });
  }

  protected labelOf(id: string) {
    return this.ctx.labelMap().get(id);
  }
}
