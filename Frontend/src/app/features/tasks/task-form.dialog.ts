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
  template: `
    <form class="dialog" [formGroup]="form" (ngSubmit)="save()">
      <div class="dialog__header">
        <div class="grow">
          <h2 class="dialog__title">{{ editing ? 'Edit task' : 'New task' }}</h2>
          <p class="muted small">
            {{ editing ? 'Update the details below.' : 'Add a task to one of your projects.' }}
          </p>
        </div>
        <button
          class="btn btn--ghost btn--icon btn--sm"
          type="button"
          (click)="ref.close()"
          aria-label="Close"
        >
          <span class="i">close</span>
        </button>
      </div>

      <div class="dialog__body">
        <div class="form-grid">
          @if (!fixedProject) {
            <div class="field full">
              <label for="tf-project">Project</label>
              <select
                id="tf-project"
                class="select"
                formControlName="projectId"
                (change)="loadProject(form.controls.projectId.value)"
              >
                <option value="" disabled>Select a project…</option>
                @for (p of workspace.projects(); track p.id) {
                  <option [value]="p.id">{{ p.name }}</option>
                }
              </select>
              @if (!workspace.projects().length && !workspace.loading()) {
                <span class="field__hint"
                  >You need a project first — create one from the Projects page.</span
                >
              }
            </div>
          }

          <div class="field full">
            <label for="tf-title">Title</label>
            <input
              id="tf-title"
              class="input"
              formControlName="title"
              placeholder="What needs to be done?"
              maxlength="200"
              cdkFocusInitial
            />
            @if (form.controls.title.touched && form.controls.title.invalid) {
              <span class="field__error">A title is required.</span>
            }
          </div>

          <div class="field full">
            <label for="tf-desc">Description</label>
            <textarea
              id="tf-desc"
              class="textarea"
              formControlName="description"
              rows="4"
              placeholder="Add more context, acceptance criteria, links…"
            ></textarea>
          </div>

          <div class="field">
            <label for="tf-status">Status</label>
            <select id="tf-status" class="select" formControlName="status">
              @for (s of statuses; track s) {
                <option [value]="s">{{ statusMeta[s].label }}</option>
              }
            </select>
          </div>

          <div class="field">
            <label for="tf-priority">Priority</label>
            <select id="tf-priority" class="select" formControlName="priority">
              <option value="">None</option>
              @for (p of priorities; track p) {
                <option [value]="p">{{ priorityMeta[p].label }}</option>
              }
            </select>
          </div>

          <div class="field">
            <label for="tf-assignee">Assignee</label>
            <select id="tf-assignee" class="select" formControlName="assigneeId">
              <option value="">Unassigned</option>
              @for (m of members(); track m.userId) {
                <option [value]="m.userId">
                  {{ users.name(m.userId) }}{{ m.userId === me() ? ' (you)' : '' }}
                </option>
              }
            </select>
          </div>

          <div class="field">
            <label for="tf-due">Due date</label>
            <input id="tf-due" class="input" type="date" formControlName="dueDate" />
          </div>

          <div class="field">
            <label for="tf-start">Start date</label>
            <input id="tf-start" class="input" type="date" formControlName="startDate" />
          </div>

          @if (!editing) {
            <div class="field">
              <label for="tf-sprint">Sprint</label>
              <select id="tf-sprint" class="select" formControlName="sprintId">
                <option value="">No sprint</option>
                @for (s of openSprints(); track s.id) {
                  <option [value]="s.id">
                    {{ s.name }}{{ s.status === 'ACTIVE' ? ' · active' : '' }}
                  </option>
                }
              </select>
            </div>

            @if (labels().length) {
              <div class="field full">
                <span class="field__label">Labels</span>
                <div class="row" style="flex-wrap: wrap; gap: 6px">
                  @for (l of labels(); track l.id) {
                    <button
                      type="button"
                      class="label-toggle"
                      [class.on]="selectedLabels().has(l.id)"
                      [style.--c]="l.color || '#64748b'"
                      (click)="toggleLabel(l.id)"
                    >
                      <span class="i">{{ selectedLabels().has(l.id) ? 'check' : 'add' }}</span
                      >{{ l.name }}
                    </button>
                  }
                </div>
              </div>
            }
          }
        </div>
        @if (dateError()) {
          <p class="field__error" style="margin-top: 12px">
            Due date can't be before the start date.
          </p>
        }
      </div>

      <div class="dialog__footer">
        <button class="btn btn--ghost" type="button" (click)="ref.close()">Cancel</button>
        <button class="btn btn--primary" type="submit" [disabled]="saving()">
          @if (saving()) {
            <span class="spinner"></span>
          }
          {{ editing ? 'Save changes' : 'Create task' }}
        </button>
      </div>
    </form>
  `,
  styles: `
    .label-toggle {
      --c: #64748b;
      display: inline-flex;
      align-items: center;
      gap: 4px;
      height: 28px;
      padding: 0 10px 0 6px;
      border-radius: 999px;
      border: 1px dashed var(--border-strong);
      background: var(--surface);
      color: var(--text-2);
      font-size: 12.5px;
      font-weight: 550;
      cursor: pointer;
      .i {
        font-size: 15px;
      }
      &.on {
        border-style: solid;
        border-color: color-mix(in srgb, var(--c) 45%, transparent);
        background: color-mix(in srgb, var(--c) 14%, transparent);
        color: var(--text);
        .i {
          color: var(--c);
        }
      }
    }
  `,
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
