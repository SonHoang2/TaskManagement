import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Observable, catchError, forkJoin, map, of, switchMap } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import {
  Label,
  ProjectMember,
  Sprint,
  Task,
  TaskInput,
  TaskPriority,
  TaskStatus,
} from '../../core/models';
import { ProjectsService } from '../../core/services/projects.service';
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
  fromDateInput,
  toDateInput,
} from '../../core/utils/format';
import { TaskFormData } from '../../shared/dialogs.service';

@Component({
  selector: 'app-task-form',
  imports: [ReactiveFormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './task-form.dialog.html',
  styleUrl: './task-form.dialog.scss',
})
export class TaskFormDialog {
  private readonly data = inject<TaskFormData>(MAT_DIALOG_DATA);
  protected readonly ref = inject(MatDialogRef<TaskFormDialog, Task | undefined>);
  private readonly tasks = inject(TasksService);
  private readonly projects = inject(ProjectsService);
  private readonly sprints = inject(SprintsService);
  private readonly auth = inject(AuthService);
  private readonly toast = inject(ToastService);
  protected readonly users = inject(UsersService);
  protected readonly workspace = inject(WorkspaceService);

  protected readonly editing = !!this.data.task;
  protected readonly fixedProject = !!(this.data.projectId || this.data.task);
  protected readonly statuses = STATUSES;
  protected readonly priorities = PRIORITIES;
  protected readonly statusMeta = STATUS_META;
  protected readonly priorityMeta = PRIORITY_META;
  protected readonly me = this.auth.userId;

  protected readonly members = signal<ProjectMember[]>([]);
  protected readonly labels = signal<Label[]>([]);
  protected readonly sprintList = signal<Sprint[]>([]);
  protected readonly selectedLabels = signal(new Set<string>());
  protected readonly saving = signal(false);
  protected readonly dateError = signal(false);
  protected readonly openSprints = computed(() =>
    this.sprintList().filter((s) => s.status !== 'COMPLETED'),
  );

  protected readonly form = inject(NonNullableFormBuilder).group({
    projectId: [this.data.task?.projectId ?? this.data.projectId ?? '', Validators.required],
    title: [this.data.task?.title ?? '', [Validators.required, Validators.maxLength(200)]],
    description: [this.data.task?.description ?? ''],
    status: [(this.data.task?.status ?? this.data.status ?? 'TODO') as TaskStatus],
    priority: [(this.data.task?.priority ?? (this.editing ? '' : 'MEDIUM')) as TaskPriority | ''],
    assigneeId: [this.data.task?.assigneeId ?? ''],
    dueDate: [toDateInput(this.data.task?.dueDate)],
    startDate: [toDateInput(this.data.task?.startDate)],
    sprintId: [this.data.sprintId ?? ''],
  });

  constructor() {
    this.workspace.load();
    const pid = this.form.controls.projectId.value;
    if (pid) this.loadProject(pid);
  }

  protected loadProject(projectId: string): void {
    if (!projectId) return;
    this.form.controls.assigneeId.setValue(this.data.task?.assigneeId ?? '');
    this.form.controls.sprintId.setValue(this.data.sprintId ?? '');
    this.selectedLabels.set(new Set());
    forkJoin({
      members: this.projects.members(projectId).pipe(catchError(() => of([] as ProjectMember[]))),
      labels: this.projects.labels(projectId).pipe(catchError(() => of([] as Label[]))),
      sprints: this.sprints.list(projectId).pipe(catchError(() => of([] as Sprint[]))),
    }).subscribe(({ members, labels, sprints }) => {
      this.users.ensure(members.map((m) => m.userId));
      this.members.set(members);
      this.labels.set(labels);
      this.sprintList.set(sprints);
    });
  }

  protected toggleLabel(id: string): void {
    this.selectedLabels.update((s) => {
      const next = new Set(s);
      next.has(id) ? next.delete(id) : next.add(id);
      return next;
    });
  }

  protected save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    if (v.dueDate && v.startDate && v.dueDate < v.startDate) {
      this.dateError.set(true);
      return;
    }
    this.dateError.set(false);

    const body: TaskInput = {
      title: v.title.trim(),
      description: v.description.trim() || null,
      status: v.status,
      priority: v.priority || null,
      assigneeId: v.assigneeId || null,
      dueDate: fromDateInput(v.dueDate),
      startDate: fromDateInput(v.startDate),
    };
    this.saving.set(true);

    const me = this.auth.userId();
    const request = this.data.task
      ? this.tasks.update(this.data.task, body, me)
      : this.tasks.create({ ...body, projectId: v.projectId, reporterId: me }, me).pipe(
          switchMap((task) => {
            const extras: Observable<unknown>[] = [
              ...[...this.selectedLabels()].map((id) => this.tasks.addLabel(task.id, id)),
              ...(v.sprintId ? [this.sprints.addTask(v.sprintId, task.id)] : []),
            ].map((o: Observable<unknown>) => o.pipe(catchError(() => of(null))));
            return extras.length ? forkJoin(extras).pipe(map(() => task)) : of(task);
          }),
        );

    request.subscribe({
      next: (task) => {
        this.workspace.upsertTask(task);
        this.toast.success(this.data.task ? 'Task updated' : 'Task created');
        this.ref.close(task);
      },
      error: (err) => {
        this.saving.set(false);
        this.toast.error(err);
      },
    });
  }
}
