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
  templateUrl: './project-tasks.component.html',
  styleUrl: './project-tasks.component.scss',
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
