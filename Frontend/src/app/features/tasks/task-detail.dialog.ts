import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatMenuModule } from '@angular/material/menu';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Observable, catchError, forkJoin, of } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import {
  Attachment,
  Comment,
  Label,
  ProjectMember,
  Sprint,
  Task,
  TaskHistory,
  TaskInput,
  TaskPriority,
  TaskStatus,
} from '../../core/models';
import { ProjectsService } from '../../core/services/projects.service';
import { SprintsService } from '../../core/services/sprints.service';
import { TasksService } from '../../core/services/tasks.service';
import { UsersService } from '../../core/services/users.service';
import { WorkspaceService } from '../../core/services/workspace.service';
import { ConfirmService } from '../../core/ui/confirm.service';
import { ToastService } from '../../core/ui/toast.service';
import {
  PRIORITIES,
  PRIORITY_META,
  STATUSES,
  STATUS_META,
  fromDateInput,
  shortDate,
  taskKey,
  toDateInput,
} from '../../core/utils/format';
import { AvatarComponent } from '../../shared/components/avatar.component';
import { SkeletonComponent } from '../../shared/components/empty-state.component';
import { TaskDetailData } from '../../shared/dialogs.service';
import { AgoPipe } from '../../shared/pipes';

@Component({
  selector: 'app-task-detail',
  imports: [
    FormsModule,
    RouterLink,
    MatMenuModule,
    MatTooltipModule,
    AvatarComponent,
    SkeletonComponent,
    AgoPipe,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './task-detail.dialog.html',
  styleUrl: './task-detail.dialog.scss',
})
export class TaskDetailDialog {
  private readonly data = inject<TaskDetailData>(MAT_DIALOG_DATA);
  private readonly ref = inject(MatDialogRef<TaskDetailDialog, boolean>);
  private readonly tasksApi = inject(TasksService);
  private readonly projectsApi = inject(ProjectsService);
  private readonly sprintsApi = inject(SprintsService);
  private readonly workspace = inject(WorkspaceService);
  private readonly confirm = inject(ConfirmService);
  private readonly toast = inject(ToastService);
  private readonly auth = inject(AuthService);
  protected readonly users = inject(UsersService);

  protected readonly statuses = STATUSES;
  protected readonly priorities = PRIORITIES;
  protected readonly statusMeta = STATUS_META;
  protected readonly priorityMeta = PRIORITY_META;
  protected readonly toDateInput = toDateInput;
  protected readonly shortDate = shortDate;
  protected readonly me = this.auth.userId;

  protected readonly task = signal<Task | null>(null);
  protected readonly loading = signal(true);
  protected readonly notFound = signal(false);
  protected readonly projectName = signal('');
  protected readonly members = signal<ProjectMember[]>([]);
  protected readonly labels = signal<Label[]>([]);
  protected readonly taskLabelIds = signal(new Set<string>());
  protected readonly sprints = signal<Sprint[]>([]);
  protected readonly sprintId = signal<string>('');
  protected readonly comments = signal<Comment[]>([]);
  protected readonly attachments = signal<Attachment[]>([]);
  protected readonly history = signal<TaskHistory[]>([]);
  protected readonly tab = signal<'comments' | 'activity'>('comments');

  protected readonly editingTitle = signal(false);
  protected readonly editingDesc = signal(false);
  protected readonly titleDraft = signal('');
  protected readonly descDraft = signal('');
  protected readonly commentDraft = signal('');
  protected readonly editingComment = signal<string | null>(null);
  protected readonly commentEditDraft = signal('');
  protected readonly addingLink = signal(false);
  protected readonly linkUrl = signal('');
  protected readonly linkName = signal('');
  protected readonly busy = signal(false);

  protected readonly key = computed(() => (this.task() ? taskKey(this.task()!) : ''));
  protected readonly myRole = computed(
    () => this.members().find((m) => m.userId === this.me())?.role,
  );
  protected readonly canManage = computed(
    () => this.myRole() === 'OWNER' || this.myRole() === 'ADMIN',
  );
  protected readonly appliedLabels = computed(() =>
    this.labels().filter((l) => this.taskLabelIds().has(l.id)),
  );

  private changed = false;

  constructor() {
    this.ref.backdropClick().subscribe(() => this.close());
    this.ref.keydownEvents().subscribe((e) => {
      if (e.key === 'Escape') this.close();
    });
    this.ref.disableClose = true;
    this.load();
  }

  private load(): void {
    const id = this.data.taskId;
    this.tasksApi.get(id).subscribe({
      next: (task) => {
        this.task.set(task);
        this.users.ensure([task.assigneeId, task.reporterId]);
        this.loadRelated(task);
      },
      error: () => {
        this.notFound.set(true);
        this.loading.set(false);
      },
    });
  }

  private loadRelated(task: Task): void {
    const safe = <T>(o: Observable<T>, fallback: T) => o.pipe(catchError(() => of(fallback)));
    const cached = this.workspace.project(task.projectId);
    if (cached) this.projectName.set(cached.name);

    forkJoin({
      project: cached ? of(null) : safe(this.projectsApi.get(task.projectId), null),
      members: safe(this.projectsApi.members(task.projectId), [] as ProjectMember[]),
      labels: safe(this.projectsApi.labels(task.projectId), [] as Label[]),
      taskLabels: safe(this.tasksApi.taskLabels(task.id), []),
      sprints: safe(this.sprintsApi.list(task.projectId), [] as Sprint[]),
      taskSprints: safe(this.sprintsApi.sprintsOfTask(task.id), []),
      comments: safe(this.tasksApi.comments(task.id), [] as Comment[]),
      attachments: safe(this.tasksApi.attachments(task.id), [] as Attachment[]),
      history: safe(this.tasksApi.history(task.id), [] as TaskHistory[]),
    }).subscribe((r) => {
      if (r.project) this.projectName.set(r.project.name);
      this.members.set(r.members);
      this.labels.set(r.labels);
      this.taskLabelIds.set(new Set(r.taskLabels.map((t) => t.labelId)));
      this.sprints.set(r.sprints);
      this.sprintId.set(r.taskSprints[0]?.sprintId ?? '');
      this.comments.set(r.comments);
      this.attachments.set(r.attachments);
      this.history.set(r.history);
      this.users.ensure([
        ...r.members.map((m) => m.userId),
        ...r.comments.map((c) => c.userId),
        ...r.history.map((h) => h.changedBy),
      ]);
      this.loading.set(false);
    });
  }

  protected close(): void {
    this.ref.close(this.changed);
  }

  // ---------- Task fields ----------
  protected update(patch: TaskInput): void {
    const before = this.task();
    if (!before) return;
    this.task.set({ ...before, ...patch } as Task);
    this.tasksApi.update(before, patch, this.me()).subscribe({
      next: (after) => {
        this.task.set(after);
        this.workspace.upsertTask(after);
        this.changed = true;
        this.refreshHistory();
      },
      error: (err) => {
        this.task.set(before);
        this.toast.error(err);
      },
    });
  }

  protected setStatus(v: string): void {
    this.update({ status: v as TaskStatus });
  }

  protected setPriority(v: string): void {
    if (v) this.update({ priority: v as TaskPriority });
  }

  protected setAssignee(v: string): void {
    if (v) this.update({ assigneeId: v });
  }

  protected setDate(field: 'dueDate' | 'startDate', v: string): void {
    const iso = fromDateInput(v);
    if (iso) this.update({ [field]: iso });
  }

  protected startTitleEdit(): void {
    this.titleDraft.set(this.task()?.title ?? '');
    this.editingTitle.set(true);
  }

  protected saveTitle(): void {
    const v = this.titleDraft().trim();
    this.editingTitle.set(false);
    if (v && v !== this.task()?.title) this.update({ title: v });
  }

  protected startDescEdit(): void {
    this.descDraft.set(this.task()?.description ?? '');
    this.editingDesc.set(true);
  }

  protected saveDesc(): void {
    const v = this.descDraft().trim();
    this.editingDesc.set(false);
    if (v !== (this.task()?.description ?? '')) this.update({ description: v });
  }

  protected deleteTask(): void {
    const t = this.task();
    if (!t) return;
    this.confirm
      .ask({
        title: 'Delete task?',
        message: `“${t.title}” will be permanently removed.`,
        confirmText: 'Delete',
        danger: true,
      })
      .subscribe((ok) => {
        if (!ok) return;
        this.tasksApi.delete(t.id).subscribe({
          next: () => {
            this.workspace.removeTask(t.id);
            this.toast.success('Task deleted');
            this.ref.close(true);
          },
          error: (e) => this.toast.error(e),
        });
      });
  }

  // ---------- Labels & sprint ----------
  protected toggleLabel(label: Label): void {
    const t = this.task();
    if (!t) return;
    const has = this.taskLabelIds().has(label.id);
    const req: Observable<unknown> = has
      ? this.tasksApi.removeLabel(t.id, label.id)
      : this.tasksApi.addLabel(t.id, label.id);
    this.taskLabelIds.update((s) => {
      const n = new Set(s);
      has ? n.delete(label.id) : n.add(label.id);
      return n;
    });
    req.subscribe({
      next: () => (this.changed = true),
      error: (e) => {
        this.toast.error(e);
        this.taskLabelIds.update((s) => {
          const n = new Set(s);
          has ? n.add(label.id) : n.delete(label.id);
          return n;
        });
      },
    });
  }

  protected setSprint(next: string): void {
    const t = this.task();
    const prev = this.sprintId();
    if (!t || next === prev) return;
    this.sprintId.set(next);
    const ops = [
      ...(prev ? [this.sprintsApi.removeTask(prev, t.id)] : []),
      ...(next ? [this.sprintsApi.addTask(next, t.id)] : []),
    ];
    forkJoin(ops).subscribe({
      next: () => {
        this.changed = true;
        this.toast.success(next ? 'Moved to sprint' : 'Removed from sprint');
      },
      error: (e) => {
        this.sprintId.set(prev);
        this.toast.error(e);
      },
    });
  }

  // ---------- Comments ----------
  protected addComment(): void {
    const t = this.task();
    const content = this.commentDraft().trim();
    if (!t || !content) return;
    this.busy.set(true);
    this.tasksApi.addComment(t.id, this.me(), content).subscribe({
      next: (c) => {
        this.comments.update((list) => [...list, c]);
        this.commentDraft.set('');
        this.busy.set(false);
        this.changed = true;
      },
      error: (e) => {
        this.busy.set(false);
        this.toast.error(e);
      },
    });
  }

  protected onCommentKey(e: KeyboardEvent): void {
    if (e.key === 'Enter' && (e.metaKey || e.ctrlKey)) {
      e.preventDefault();
      this.addComment();
    }
  }

  protected startCommentEdit(c: Comment): void {
    this.editingComment.set(c.id);
    this.commentEditDraft.set(c.content);
  }

  protected saveComment(c: Comment): void {
    const content = this.commentEditDraft().trim();
    this.editingComment.set(null);
    if (!content || content === c.content) return;
    this.tasksApi.editComment(c.id, content).subscribe({
      next: (u) => this.comments.update((l) => l.map((x) => (x.id === c.id ? { ...x, ...u } : x))),
      error: (e) => this.toast.error(e),
    });
  }

  protected deleteComment(c: Comment): void {
    this.confirm
      .ask({
        title: 'Delete comment?',
        message: 'This comment will be removed for everyone.',
        confirmText: 'Delete',
        danger: true,
      })
      .subscribe((ok) => {
        if (!ok) return;
        this.tasksApi.deleteComment(c.id).subscribe({
          next: () => {
            this.comments.update((l) => l.filter((x) => x.id !== c.id));
            this.changed = true;
          },
          error: (e) => this.toast.error(e),
        });
      });
  }

  // ---------- Attachments ----------
  protected addLink(): void {
    const t = this.task();
    let url = this.linkUrl().trim();
    if (!t || !url) return;
    if (!/^https?:\/\//i.test(url)) url = 'https://' + url;
    try {
      new URL(url);
    } catch {
      this.toast.error('Please enter a valid URL.');
      return;
    }
    const fileName = this.linkName().trim() || prettyUrl(url);
    this.tasksApi
      .addAttachment({ taskId: t.id, fileUrl: url, fileName, uploadedBy: this.me() })
      .subscribe({
        next: (a) => {
          this.attachments.update((l) => [...l, a]);
          this.linkUrl.set('');
          this.linkName.set('');
          this.addingLink.set(false);
          this.changed = true;
        },
        error: (e) => this.toast.error(e),
      });
  }

  protected removeAttachment(a: Attachment): void {
    this.tasksApi.deleteAttachment(a.id).subscribe({
      next: () => {
        this.attachments.update((l) => l.filter((x) => x.id !== a.id));
        this.changed = true;
      },
      error: (e) => this.toast.error(e),
    });
  }

  protected host(url: string): string {
    try {
      return new URL(url).hostname.replace(/^www\./, '');
    } catch {
      return url;
    }
  }

  protected favicon(url: string): string {
    return `https://www.google.com/s2/favicons?sz=32&domain=${encodeURIComponent(this.host(url))}`;
  }

  // ---------- History ----------
  private refreshHistory(): void {
    const t = this.task();
    if (!t) return;
    this.tasksApi.history(t.id).subscribe({ next: (h) => this.history.set(h), error: () => {} });
  }

  protected describe(h: TaskHistory): { verb: string; from?: string; to?: string } {
    const fmt = (v: string | null): string => {
      if (!v) return 'none';
      switch (h.field) {
        case 'status':
          return STATUS_META[v as TaskStatus]?.label ?? v;
        case 'priority':
          return PRIORITY_META[v as TaskPriority]?.label ?? v;
        case 'assigneeId':
          return this.users.name(v);
        case 'dueDate':
        case 'startDate':
          return shortDate(v);
        default:
          return v.length > 60 ? v.slice(0, 57) + '…' : v;
      }
    };
    switch (h.field) {
      case 'created':
        return { verb: 'created this task' };
      case 'title':
        return { verb: 'renamed the task to', to: fmt(h.newValue) };
      case 'description':
        return { verb: 'updated the description' };
      case 'status':
        return { verb: 'changed status', from: fmt(h.oldValue), to: fmt(h.newValue) };
      case 'priority':
        return { verb: 'changed priority', from: fmt(h.oldValue), to: fmt(h.newValue) };
      case 'assigneeId':
        return { verb: 'assigned to', to: fmt(h.newValue) };
      case 'dueDate':
        return { verb: 'set due date to', to: fmt(h.newValue) };
      case 'startDate':
        return { verb: 'set start date to', to: fmt(h.newValue) };
      default:
        return {
          verb: `updated ${h.field ?? 'the task'}`,
          from: h.oldValue ?? undefined,
          to: h.newValue ?? undefined,
        };
    }
  }
}

function prettyUrl(url: string): string {
  try {
    const u = new URL(url);
    const last = u.pathname.split('/').filter(Boolean).pop();
    return last ? decodeURIComponent(last) : u.hostname;
  } catch {
    return url;
  }
}
