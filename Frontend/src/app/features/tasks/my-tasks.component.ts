import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatTooltipModule } from '@angular/material/tooltip';
import { AuthService } from '../../core/auth/auth.service';
import { TaskDetail, TaskPriority, TaskStatus } from '../../core/models';
import { TasksService } from '../../core/services/tasks.service';
import { WorkspaceService } from '../../core/services/workspace.service';
import { ToastService } from '../../core/ui/toast.service';
import {
  PRIORITIES,
  PRIORITY_META,
  STATUSES,
  STATUS_META,
  colorFor,
  compareTasks,
  daysUntil,
} from '../../core/utils/format';
import { AvatarComponent } from '../../shared/components/avatar.component';
import { DueDateComponent, PriorityBadgeComponent } from '../../shared/components/badges';
import {
  EmptyStateComponent,
  SkeletonComponent,
} from '../../shared/components/empty-state.component';
import { DialogsService } from '../../shared/dialogs.service';

type Scope = 'assigned' | 'reported' | 'all';
type GroupBy = 'status' | 'due' | 'project';

interface Group {
  key: string;
  label: string;
  icon: string;
  tone?: string;
  color?: string;
  tasks: TaskDetail[];
}

const NEXT: Record<TaskStatus, TaskStatus> = {
  TODO: 'IN_PROGRESS',
  IN_PROGRESS: 'DONE',
  DONE: 'TODO',
};

@Component({
  selector: 'app-my-tasks',
  imports: [
    FormsModule,
    MatTooltipModule,
    AvatarComponent,
    PriorityBadgeComponent,
    DueDateComponent,
    EmptyStateComponent,
    SkeletonComponent,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="page">
      <div class="page-header">
        <div class="page-header__text">
          <h1>My tasks</h1>
          <p>Everything on your plate across all of your projects.</p>
        </div>
        <div class="page-header__actions">
          <button
            class="btn"
            type="button"
            (click)="ws.load(true)"
            [disabled]="ws.loading()"
            matTooltip="Refresh"
          >
            <span class="i" [class.spin]="ws.loading()">refresh</span>
          </button>
          <button class="btn btn--primary" type="button" (click)="create()">
            <span class="i">add</span>New task
          </button>
        </div>
      </div>

      <div class="toolbar">
        <div class="segmented">
          <button
            type="button"
            [class.active]="scope() === 'assigned'"
            (click)="scope.set('assigned')"
          >
            Assigned to me <span class="count">{{ counts().assigned }}</span>
          </button>
          <button
            type="button"
            [class.active]="scope() === 'reported'"
            (click)="scope.set('reported')"
          >
            Created by me <span class="count">{{ counts().reported }}</span>
          </button>
          <button type="button" [class.active]="scope() === 'all'" (click)="scope.set('all')">
            All <span class="count">{{ counts().all }}</span>
          </button>
        </div>
        <div class="input-icon">
          <span class="i">search</span>
          <input
            class="input"
            placeholder="Search…"
            [ngModel]="search()"
            (ngModelChange)="search.set($event)"
          />
        </div>
        <select
          class="select"
          [ngModel]="project()"
          (ngModelChange)="project.set($event)"
          aria-label="Project"
        >
          <option value="">All projects</option>
          @for (p of ws.projects(); track p.id) {
            <option [value]="p.id">{{ p.name }}</option>
          }
        </select>
        <select
          class="select"
          [ngModel]="priority()"
          (ngModelChange)="priority.set($event)"
          aria-label="Priority"
        >
          <option value="">Any priority</option>
          @for (p of priorities; track p) {
            <option [value]="p">{{ priorityMeta[p].label }}</option>
          }
        </select>
        <label class="checkbox"
          ><input
            type="checkbox"
            [ngModel]="showDone()"
            (ngModelChange)="showDone.set($event)"
          />Show done</label
        >
        <span class="spacer"></span>
        <select
          class="select"
          [ngModel]="groupBy()"
          (ngModelChange)="groupBy.set($event)"
          aria-label="Group by"
        >
          <option value="status">Group: Status</option>
          <option value="due">Group: Due date</option>
          <option value="project">Group: Project</option>
        </select>
      </div>

      <div class="card">
        @if (!ws.loaded()) {
          <div class="card__body"><app-skeleton [count]="6" [height]="40" /></div>
        } @else if (!filtered().length) {
          <app-empty-state
            icon="task_alt"
            title="No tasks here"
            text="Nothing matches these filters. Enjoy the calm — or create something new."
          >
            <button class="btn btn--primary btn--sm" type="button" (click)="create()">
              <span class="i">add</span>New task
            </button>
          </app-empty-state>
        } @else {
          @for (g of groups(); track g.key) {
            <div class="group-header">
              @if (g.color) {
                <span class="gdot" [style.background]="g.color"></span>
              } @else {
                <span class="i tone-{{ g.tone }}">{{ g.icon }}</span>
              }
              {{ g.label }}
              <span class="count">{{ g.tasks.length }}</span>
            </div>
            @for (t of g.tasks; track t.id) {
              <div class="task-row" (click)="open(t)">
                <button
                  class="task-row__status task-row__status--{{ t.status }}"
                  type="button"
                  (click)="$event.stopPropagation(); cycle(t)"
                  [matTooltip]="'Mark as ' + statusMeta[next(t.status)].label"
                  [attr.aria-label]="'Mark as ' + statusMeta[next(t.status)].label"
                >
                  <span class="i">{{ statusMeta[t.status].icon }}</span>
                </button>
                <span class="task-row__title" [class.done]="t.status === 'DONE'">{{
                  t.title
                }}</span>
                <div class="task-row__meta">
                  @if (groupBy() !== 'project') {
                    <span class="task-row__project truncate">{{
                      ws.project(t.projectId)?.name
                    }}</span>
                  }
                  <app-priority [value]="t.priority" [iconOnly]="true" />
                  @if (t.dueDate) {
                    <app-due [date]="t.dueDate" [status]="t.status" />
                  }
                  @if (scope() !== 'assigned') {
                    <app-avatar [userId]="t.assigneeId" size="xs" />
                  }
                </div>
              </div>
            }
          }
        }
      </div>
    </div>
  `,
  styles: `
    .gdot {
      width: 10px;
      height: 10px;
      border-radius: 3px;
    }
    .tone-todo {
      color: var(--todo);
    }
    .tone-progress {
      color: var(--progress);
    }
    .tone-done {
      color: var(--done);
    }
    .tone-danger {
      color: var(--danger);
    }
    .tone-warning {
      color: var(--warning);
    }
    .spin {
      animation: spin 0.8s linear infinite;
    }
    .segmented .count {
      margin-left: 2px;
    }
  `,
})
export class MyTasksComponent {
  protected readonly ws = inject(WorkspaceService);
  private readonly auth = inject(AuthService);
  private readonly tasksApi = inject(TasksService);
  private readonly dialogs = inject(DialogsService);
  private readonly toast = inject(ToastService);

  protected readonly statusMeta = STATUS_META;
  protected readonly priorities = PRIORITIES;
  protected readonly priorityMeta = PRIORITY_META;

  protected readonly scope = signal<Scope>('assigned');
  protected readonly search = signal('');
  protected readonly project = signal('');
  protected readonly priority = signal<TaskPriority | ''>('');
  protected readonly showDone = signal(false);
  protected readonly groupBy = signal<GroupBy>('status');

  protected readonly counts = computed(() => {
    const me = this.auth.userId();
    const open = this.ws.tasks().filter((t) => t.status !== 'DONE');
    return {
      assigned: open.filter((t) => t.assigneeId === me).length,
      reported: open.filter((t) => t.reporterId === me).length,
      all: open.length,
    };
  });

  protected readonly filtered = computed(() => {
    const me = this.auth.userId();
    const q = this.search().trim().toLowerCase();
    return this.ws
      .tasks()
      .filter((t) => {
        if (this.scope() === 'assigned' && t.assigneeId !== me) return false;
        if (this.scope() === 'reported' && t.reporterId !== me) return false;
        if (!this.showDone() && t.status === 'DONE') return false;
        if (this.project() && t.projectId !== this.project()) return false;
        if (this.priority() && t.priority !== this.priority()) return false;
        if (q && !t.title.toLowerCase().includes(q)) return false;
        return true;
      })
      .sort(compareTasks);
  });

  protected readonly groups = computed<Group[]>(() => {
    const tasks = this.filtered();
    const by = this.groupBy();
    if (by === 'status') {
      return STATUSES.map((s) => ({
        key: s,
        label: STATUS_META[s].label,
        icon: STATUS_META[s].icon,
        tone: STATUS_META[s].tone,
        tasks: tasks.filter((t) => t.status === s),
      })).filter((g) => g.tasks.length);
    }
    if (by === 'project') {
      const map = new Map<string, TaskDetail[]>();
      for (const t of tasks) map.set(t.projectId, [...(map.get(t.projectId) ?? []), t]);
      return [...map.entries()].map(([id, list]) => ({
        key: id,
        label: this.ws.project(id)?.name ?? 'Project',
        icon: 'folder',
        color: colorFor(id),
        tasks: list,
      }));
    }
    const buckets: {
      key: string;
      label: string;
      icon: string;
      tone: string;
      test: (t: TaskDetail) => boolean;
    }[] = [
      {
        key: 'overdue',
        label: 'Overdue',
        icon: 'event_busy',
        tone: 'danger',
        test: (t) => !!t.dueDate && t.status !== 'DONE' && daysUntil(t.dueDate) < 0,
      },
      {
        key: 'today',
        label: 'Today',
        icon: 'today',
        tone: 'warning',
        test: (t) => !!t.dueDate && daysUntil(t.dueDate) === 0,
      },
      {
        key: 'week',
        label: 'Next 7 days',
        icon: 'date_range',
        tone: 'progress',
        test: (t) => !!t.dueDate && daysUntil(t.dueDate) > 0 && daysUntil(t.dueDate) <= 7,
      },
      {
        key: 'later',
        label: 'Later',
        icon: 'event_upcoming',
        tone: 'todo',
        test: (t) => !!t.dueDate && daysUntil(t.dueDate) > 7,
      },
      {
        key: 'none',
        label: 'No due date',
        icon: 'event_available',
        tone: 'todo',
        test: (t) => !t.dueDate,
      },
      { key: 'past', label: 'Done earlier', icon: 'history', tone: 'done', test: () => true },
    ];
    const used = new Set<string>();
    return buckets
      .map((b) => {
        const list = tasks.filter((t) => !used.has(t.id) && b.test(t));
        list.forEach((t) => used.add(t.id));
        return { key: b.key, label: b.label, icon: b.icon, tone: b.tone, tasks: list };
      })
      .filter((g) => g.tasks.length);
  });

  constructor() {
    this.ws.load();
  }

  protected next(s: TaskStatus): TaskStatus {
    return NEXT[s];
  }

  protected cycle(t: TaskDetail): void {
    const status = NEXT[t.status];
    this.ws.upsertTask({ ...t, status });
    this.tasksApi.update(t, { status }, this.auth.userId()).subscribe({
      next: (u) => {
        this.ws.upsertTask(u);
        if (status === 'DONE') this.toast.success('Nice work — task completed 🎉');
      },
      error: (e) => {
        this.ws.upsertTask(t);
        this.toast.error(e);
      },
    });
  }

  protected open(t: TaskDetail): void {
    this.dialogs.taskDetail(t.id).subscribe((c) => c && this.ws.load(true));
  }

  protected create(): void {
    this.dialogs.taskForm().subscribe((t) => t && this.ws.load(true));
  }
}
