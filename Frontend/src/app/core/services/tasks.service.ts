import { Injectable, inject } from '@angular/core';
import { Observable, forkJoin, map, of, switchMap, catchError } from 'rxjs';
import { ApiClient, pageParams } from '../http/api-client';
import {
  Attachment,
  Comment,
  Page,
  PageQuery,
  Task,
  TaskDetail,
  TaskHistory,
  TaskInput,
  TaskLabel,
} from '../models';

/** Task fields we keep an audit trail for. */
const TRACKED: (keyof TaskInput)[] = [
  'title',
  'description',
  'status',
  'priority',
  'assigneeId',
  'dueDate',
  'startDate',
];

@Injectable({ providedIn: 'root' })
export class TasksService {
  private readonly api = inject(ApiClient);

  // ---- Tasks ----
  byProject(projectId: string, q: PageQuery = {}): Observable<Page<TaskDetail>> {
    return this.api
      .get<{ page: Page<TaskDetail> }>(
        'task-service',
        `/tasks/project/${projectId}`,
        pageParams({ size: 500, sort: 'createdAt,asc', ...q }),
      )
      .pipe(map((d) => d.page));
  }

  search(keyword: string, q: PageQuery = {}): Observable<Page<Task>> {
    return this.api
      .get<{ page: Page<Task> }>('task-service', '/tasks', pageParams(q, { keyword }))
      .pipe(map((d) => d.page));
  }

  get(id: string): Observable<Task> {
    return this.api.get<{ task: Task }>('task-service', `/tasks/${id}`).pipe(map((d) => d.task));
  }

  create(body: TaskInput, userId: string): Observable<Task> {
    return this.api.post<{ task: Task }>('task-service', '/tasks', body).pipe(
      map((d) => d.task),
      switchMap((task) =>
        this.log(task.id, userId, 'created', null, task.title).pipe(map(() => task)),
      ),
    );
  }

  /** PATCHes the task and records one history entry per changed field. */
  update(before: Task, patch: TaskInput, userId: string): Observable<Task> {
    return this.api.patch<{ task: Task }>('task-service', `/tasks/${before.id}`, patch).pipe(
      map((d) => d.task),
      switchMap((after) => {
        const entries = TRACKED.filter((f) => f in patch && norm(before[f]) !== norm(after[f])).map(
          (f) => this.log(before.id, userId, f, norm(before[f]), norm(after[f])),
        );
        return entries.length ? forkJoin(entries).pipe(map(() => after)) : of(after);
      }),
    );
  }

  delete(id: string): Observable<void> {
    return this.api.delete('task-service', `/tasks/${id}`);
  }

  stats(
    projectId: string,
  ): Observable<{ total: number; todo: number; inProgress: number; done: number }> {
    return this.api
      .get<{ stats: { total: number; todo: number; inProgress: number; done: number } }>(
        'task-service',
        `/tasks/project/${projectId}/stats`,
      )
      .pipe(map((d) => d.stats));
  }

  // ---- Comments ----
  comments(taskId: string): Observable<Comment[]> {
    return this.api
      .get<{ page: Page<Comment> }>('task-service', '/task-comments', {
        taskId,
        size: 200,
        sort: 'createdAt,asc',
      })
      .pipe(map((d) => d.page.content));
  }

  addComment(taskId: string, userId: string, content: string): Observable<Comment> {
    return this.api
      .post<{ comment: Comment }>('task-service', '/task-comments', { taskId, userId, content })
      .pipe(map((d) => d.comment));
  }

  editComment(id: string, content: string): Observable<Comment> {
    return this.api
      .patch<{ comment: Comment }>('task-service', `/task-comments/${id}`, { content })
      .pipe(map((d) => d.comment));
  }

  deleteComment(id: string): Observable<void> {
    return this.api.delete('task-service', `/task-comments/${id}`);
  }

  // ---- Attachments (links) ----
  attachments(taskId: string): Observable<Attachment[]> {
    return this.api
      .get<{ page: Page<Attachment> }>('task-service', '/task-attachments', {
        taskId,
        size: 100,
        sort: 'createdAt,asc',
      })
      .pipe(map((d) => d.page.content));
  }

  addAttachment(body: {
    taskId: string;
    fileUrl: string;
    fileName: string;
    uploadedBy: string;
  }): Observable<Attachment> {
    return this.api
      .post<{ attachment: Attachment }>('task-service', '/task-attachments', body)
      .pipe(map((d) => d.attachment));
  }

  deleteAttachment(id: string): Observable<void> {
    return this.api.delete('task-service', `/task-attachments/${id}`);
  }

  // ---- Labels on tasks ----
  taskLabels(taskId: string): Observable<TaskLabel[]> {
    return this.api
      .get<{ page: Page<TaskLabel> }>('task-service', '/task-labels', { taskId, size: 100 })
      .pipe(map((d) => d.page.content));
  }

  addLabel(taskId: string, labelId: string): Observable<TaskLabel> {
    return this.api
      .post<{ taskLabel: TaskLabel }>('task-service', '/task-labels', { taskId, labelId })
      .pipe(map((d) => d.taskLabel));
  }

  removeLabel(taskId: string, labelId: string): Observable<void> {
    return this.api.delete('task-service', `/task-labels/${taskId}/${labelId}`);
  }

  // ---- History ----
  history(taskId: string): Observable<TaskHistory[]> {
    return this.api
      .get<{ page: Page<TaskHistory> }>('task-service', '/task-histories', {
        taskId,
        size: 100,
        sort: 'createdAt,desc',
      })
      .pipe(map((d) => d.page.content));
  }

  private log(
    taskId: string,
    changedBy: string,
    field: string,
    oldValue: string | null,
    newValue: string | null,
  ): Observable<unknown> {
    // The audit trail is best-effort: never fail the user's action because of it.
    return this.api
      .post('task-service', '/task-histories', {
        taskId,
        changedBy,
        field,
        oldValue: clip(oldValue),
        newValue: clip(newValue),
      })
      .pipe(catchError(() => of(null)));
  }
}

function norm(v: unknown): string | null {
  if (v === undefined || v === null || v === '') return null;
  return String(v);
}

function clip(v: string | null): string | null {
  return v && v.length > 500 ? v.slice(0, 497) + '…' : v;
}
