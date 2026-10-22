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
  templateUrl: './my-tasks.component.html',
  styleUrl: './my-tasks.component.scss',
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
