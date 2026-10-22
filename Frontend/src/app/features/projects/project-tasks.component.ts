import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TaskDetail, TaskStatus } from '../../core/models';
import { UsersService } from '../../core/services/users.service';
import { STATUSES, STATUS_META, compareTasks, taskKey } from '../../core/utils/format';
import { AvatarComponent } from '../../shared/components/avatar.component';
import {
  DueDateComponent,
  PriorityBadgeComponent,
  StatusBadgeComponent,
} from '../../shared/components/badges';
import {
  EmptyStateComponent,
  SkeletonComponent,
} from '../../shared/components/empty-state.component';
import { DialogsService } from '../../shared/dialogs.service';
import { AgoPipe } from '../../shared/pipes';
import { ProjectContext } from './project-context';

type SortKey = 'priority' | 'due' | 'created' | 'updated' | 'title';

@Component({
  selector: 'app-project-tasks',
  imports: [
    FormsModule,
    AvatarComponent,
    StatusBadgeComponent,
    PriorityBadgeComponent,
    DueDateComponent,
    EmptyStateComponent,
    SkeletonComponent,
    AgoPipe,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="page page--wide" style="padding-top: 20px">
      <div class="toolbar">
        <div class="input-icon">
          <span class="i">search</span>
          <input
            class="input"
            placeholder="Search tasks…"
            [ngModel]="search()"
            (ngModelChange)="search.set($event)"
          />
        </div>
        <select
          class="select"
          [ngModel]="status()"
          (ngModelChange)="status.set($event)"
          aria-label="Status"
        >
          <option value="">All statuses</option>
          @for (s of statuses; track s) {
            <option [value]="s">{{ statusMeta[s].label }}</option>
          }
        </select>
        <select
          class="select"
          [ngModel]="sort()"
          (ngModelChange)="sort.set($event)"
          aria-label="Sort"
        >
          <option value="priority">Sort: Priority</option>
          <option value="due">Sort: Due date</option>
          <option value="created">Sort: Newest</option>
          <option value="updated">Sort: Recently updated</option>
          <option value="title">Sort: Title</option>
        </select>
        <span class="spacer"></span>
        <span class="subtle small">{{ rows().length }} tasks</span>
      </div>

      <div class="card">
        @if (ctx.tasksLoading() && !ctx.tasks().length) {
          <div class="card__body"><app-skeleton [count]="6" [height]="40" /></div>
        } @else if (!rows().length) {
          <app-empty-state
            icon="checklist"
            title="No tasks match"
            text="Try a different filter, or create a new task."
            [compact]="true"
          >
            <button class="btn btn--primary btn--sm" type="button" (click)="create()">
              <span class="i">add</span>New task
            </button>
          </app-empty-state>
        } @else {
          <div class="table-wrap">
            <table class="table">
              <thead>
                <tr>
                  <th style="width: 80px">Key</th>
                  <th>Title</th>
                  <th>Status</th>
                  <th>Priority</th>
                  <th>Assignee</th>
                  <th>Due</th>
                  <th>Updated</th>
                </tr>
              </thead>
              <tbody>
                @for (t of rows(); track t.id) {
                  <tr class="clickable" (click)="open(t)">
                    <td class="mono subtle">{{ key(t) }}</td>
                    <td style="max-width: 420px">
                      <div class="truncate" style="font-weight: 550">{{ t.title }}</div>
                      @if (t.labels.length) {
                        <div class="row" style="gap: 4px; margin-top: 4px; flex-wrap: wrap">
                          @for (tl of t.labels; track tl.labelId) {
                            @if (ctx.labelMap().get(tl.labelId); as l) {
                              <span class="label-chip" [style.--c]="l.color || '#64748b'">{{
                                l.name
                              }}</span>
                            }
                          }
                        </div>
                      }
                    </td>
                    <td><app-status [value]="t.status" /></td>
                    <td><app-priority [value]="t.priority" /></td>
                    <td>
                      <div class="row">
                        <app-avatar [userId]="t.assigneeId" size="xs" />
                        <span class="truncate small" style="max-width: 140px">{{
                          users.name(t.assigneeId)
                        }}</span>
                      </div>
                    </td>
                    <td>
                      @if (t.dueDate) {
                        <app-due [date]="t.dueDate" [status]="t.status" />
                      } @else {
                        <span class="subtle">—</span>
                      }
                    </td>
                    <td class="subtle small">{{ t.updatedAt | ago }}</td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        }
      </div>
    </div>
  `,
})
export class ProjectTasksComponent {
  protected readonly ctx = inject(ProjectContext);
  protected readonly users = inject(UsersService);
  private readonly dialogs = inject(DialogsService);
  protected readonly statuses = STATUSES;
  protected readonly statusMeta = STATUS_META;
  protected readonly key = taskKey;

  protected readonly search = signal('');
  protected readonly status = signal<TaskStatus | ''>('');
  protected readonly sort = signal<SortKey>('priority');

  protected readonly rows = computed(() => {
    const q = this.search().trim().toLowerCase();
    const s = this.status();
    const list = this.ctx
      .tasks()
      .filter((t) => (!s || t.status === s) && (!q || t.title.toLowerCase().includes(q)));
    const time = (v: string | null) => (v ? new Date(v).getTime() : Infinity);
    const sorters: Record<SortKey, (a: TaskDetail, b: TaskDetail) => number> = {
      priority: compareTasks,
      due: (a, b) => time(a.dueDate) - time(b.dueDate),
      created: (a, b) => time(b.createdAt) - time(a.createdAt),
      updated: (a, b) => time(b.updatedAt) - time(a.updatedAt),
      title: (a, b) => a.title.localeCompare(b.title),
    };
    return [...list].sort(sorters[this.sort()]);
  });

  protected open(t: TaskDetail): void {
    this.dialogs.taskDetail(t.id).subscribe((c) => c && this.ctx.reloadTasks());
  }

  protected create(): void {
    this.dialogs
      .taskForm({ projectId: this.ctx.id() })
      .subscribe((t) => t && this.ctx.reloadTasks());
  }
}
