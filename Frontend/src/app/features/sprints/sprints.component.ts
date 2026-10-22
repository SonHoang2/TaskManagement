import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inject,
  signal,
  untracked,
} from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatMenuModule } from '@angular/material/menu';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Sprint, SprintStatus, TaskDetail } from '../../core/models';
import { SprintsService } from '../../core/services/sprints.service';
import { ConfirmService } from '../../core/ui/confirm.service';
import { ToastService } from '../../core/ui/toast.service';
import { STATUS_META, compareTasks, daysUntil, percent, shortDate } from '../../core/utils/format';
import { AvatarComponent } from '../../shared/components/avatar.component';
import {
  DueDateComponent,
  PriorityBadgeComponent,
  SprintStatusBadgeComponent,
} from '../../shared/components/badges';
import {
  EmptyStateComponent,
  SkeletonComponent,
} from '../../shared/components/empty-state.component';
import { DialogsService } from '../../shared/dialogs.service';
import { ProjectContext } from '../projects/project-context';
import { SprintFormDialog } from './sprint-form.dialog';

const ORDER: Record<SprintStatus, number> = { ACTIVE: 0, PLANNED: 1, COMPLETED: 2 };

@Component({
  selector: 'app-sprints',
  imports: [
    MatMenuModule,
    MatTooltipModule,
    AvatarComponent,
    PriorityBadgeComponent,
    DueDateComponent,
    SprintStatusBadgeComponent,
    EmptyStateComponent,
    SkeletonComponent,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './sprints.component.html',
  styleUrl: './sprints.component.scss',
})
export class SprintsComponent {
  protected readonly ctx = inject(ProjectContext);
  private readonly sprintsApi = inject(SprintsService);
  private readonly dialog = inject(MatDialog);
  private readonly dialogs = inject(DialogsService);
  private readonly confirm = inject(ConfirmService);
  private readonly toast = inject(ToastService);
  protected readonly statusMeta = STATUS_META;
  protected readonly shortDate = shortDate;

  /** sprintId -> task ids */
  protected readonly membership = signal<Record<string, string[]>>({});
  protected readonly loadingMembership = signal(true);
  protected readonly expanded = signal<Set<string>>(new Set());

  protected readonly sorted = computed(() =>
    [...this.ctx.sprints()].sort(
      (a, b) =>
        ORDER[a.status] - ORDER[b.status] || (b.startDate ?? '').localeCompare(a.startDate ?? ''),
    ),
  );
  protected readonly openSprints = computed(() =>
    this.sorted().filter((s) => s.status !== 'COMPLETED'),
  );
  private readonly taskById = computed(() => new Map(this.ctx.tasks().map((t) => [t.id, t])));
  protected readonly backlog = computed(() => {
    const assigned = new Set(Object.values(this.membership()).flat());
    return this.ctx
      .tasks()
      .filter((t) => !assigned.has(t.id) && t.status !== 'DONE')
      .sort(compareTasks);
  });

  constructor() {
    effect(() => {
      const ids = this.ctx.sprints().map((s) => s.id);
      untracked(() => this.loadMembership(ids));
    });
    effect(() => {
      const active = this.ctx.activeSprint();
      if (active) untracked(() => this.expanded.update((s) => new Set(s).add(active.id)));
    });
  }

  private loadMembership(ids: string[]): void {
    if (!ids.length) {
      this.membership.set({});
      this.loadingMembership.set(false);
      return;
    }
    this.ctx.sprintTaskIds(ids).subscribe((map) => {
      const next: Record<string, string[]> = {};
      for (const [id, list] of Object.entries(map)) next[id] = list.map((x) => x.taskId);
      this.membership.set(next);
      this.loadingMembership.set(false);
    });
  }

  protected tasksOf(s: Sprint): TaskDetail[] {
    const byId = this.taskById();
    return (this.membership()[s.id] ?? [])
      .map((id) => byId.get(id))
      .filter((t): t is TaskDetail => !!t)
      .sort(compareTasks);
  }

  protected progress(s: Sprint): { done: number; total: number; pct: number } {
    const tasks = this.tasksOf(s);
    const done = tasks.filter((t) => t.status === 'DONE').length;
    return {
      done,
      total: tasks.length,
      pct: percent(done, tasks.length),
    };
  }

  protected timeLeft(s: Sprint): string {
    if (!s.endDate) return '';
    if (s.status === 'COMPLETED') return `Ended ${shortDate(s.endDate)}`;
    const n = daysUntil(s.endDate);
    if (n < 0) return `${-n} days overdue`;
    if (n === 0) return 'Ends today';
    return `${n} days left`;
  }

  protected toggle(id: string): void {
    this.expanded.update((s) => {
      const n = new Set(s);
      n.has(id) ? n.delete(id) : n.add(id);
      return n;
    });
  }

  protected create(): void {
    this.dialog
      .open(SprintFormDialog, {
        width: '520px',
        data: {
          projectId: this.ctx.id(),
          suggestedName: `Sprint ${this.ctx.sprints().length + 1}`,
        },
      })
      .afterClosed()
      .subscribe((s) => s && this.ctx.reloadSprints());
  }

  protected edit(s: Sprint): void {
    this.dialog
      .open(SprintFormDialog, { width: '520px', data: { projectId: this.ctx.id(), sprint: s } })
      .afterClosed()
      .subscribe((u) => u && this.ctx.reloadSprints());
  }

  protected setStatus(s: Sprint, status: SprintStatus): void {
    const other = this.ctx.activeSprint();
    const go = () =>
      this.sprintsApi
        .update(s.id, { name: s.name, startDate: s.startDate, endDate: s.endDate, status })
        .subscribe({
          next: () => {
            this.toast.success(
              status === 'ACTIVE'
                ? `${s.name} started`
                : status === 'COMPLETED'
                  ? `${s.name} completed`
                  : 'Sprint updated',
            );
            this.ctx.reloadSprints();
          },
          error: (e) => this.toast.error(e),
        });

    if (status === 'ACTIVE' && other && other.id !== s.id) {
      this.confirm
        .ask({
          title: 'Start this sprint?',
          message: `“${other.name}” is still active. You can run more than one, but it's usually clearer to complete it first.`,
          confirmText: 'Start anyway',
        })
        .subscribe((ok) => ok && go());
    } else if (status === 'COMPLETED') {
      const open = this.tasksOf(s).filter((t) => t.status !== 'DONE').length;
      if (!open) {
        go();
        return;
      }
      this.confirm
        .ask({
          title: `Complete ${s.name}?`,
          message: `${open} task(s) are not done yet. They will stay in this sprint — you can move them back to the backlog afterwards.`,
          confirmText: 'Complete sprint',
        })
        .subscribe((ok) => ok && go());
    } else {
      go();
    }
  }

  protected remove(s: Sprint): void {
    this.confirm
      .ask({
        title: `Delete ${s.name}?`,
        message: 'Tasks in this sprint stay in the project and return to the backlog.',
        confirmText: 'Delete',
        danger: true,
      })
      .subscribe((ok) => {
        if (!ok) return;
        this.sprintsApi.delete(s.id).subscribe({
          next: () => {
            this.toast.success('Sprint deleted');
            this.ctx.reloadSprints();
          },
          error: (e) => this.toast.error(e),
        });
      });
  }

  protected addToSprint(task: TaskDetail, s: Sprint): void {
    this.membership.update((m) => ({ ...m, [s.id]: [...(m[s.id] ?? []), task.id] }));
    this.sprintsApi.addTask(s.id, task.id).subscribe({
      next: () => this.toast.success(`Added to ${s.name}`),
      error: (e) => {
        this.membership.update((m) => ({
          ...m,
          [s.id]: (m[s.id] ?? []).filter((id) => id !== task.id),
        }));
        this.toast.error(e);
      },
    });
  }

  protected removeFromSprint(task: TaskDetail, s: Sprint): void {
    const prev = this.membership()[s.id] ?? [];
    this.membership.update((m) => ({ ...m, [s.id]: prev.filter((id) => id !== task.id) }));
    this.sprintsApi.removeTask(s.id, task.id).subscribe({
      error: (e) => {
        this.membership.update((m) => ({ ...m, [s.id]: prev }));
        this.toast.error(e);
      },
    });
  }

  protected open(t: TaskDetail): void {
    this.dialogs.taskDetail(t.id).subscribe((c) => {
      if (!c) return;
      this.ctx.reloadTasks();
      this.loadMembership(this.ctx.sprints().map((s) => s.id));
    });
  }

  protected newTaskIn(s: Sprint): void {
    this.dialogs.taskForm({ projectId: this.ctx.id(), sprintId: s.id }).subscribe((t) => {
      if (!t) return;
      this.ctx.reloadTasks();
      this.loadMembership(this.ctx.sprints().map((x) => x.id));
    });
  }
}
