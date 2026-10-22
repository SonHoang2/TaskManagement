import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { ApiClient } from '../http/api-client';
import { Page, Sprint, SprintInput, TaskSprint } from '../models';

@Injectable({ providedIn: 'root' })
export class SprintsService {
  private readonly api = inject(ApiClient);

  list(projectId: string): Observable<Sprint[]> {
    return this.api
      .get<{ page: Page<Sprint> }>('sprint-service', '/sprints', {
        projectId,
        size: 200,
        sort: 'createdAt,desc',
      })
      .pipe(map((d) => d.page.content));
  }

  create(body: SprintInput & { projectId: string }): Observable<Sprint> {
    return this.api
      .post<{ sprint: Sprint }>('sprint-service', '/sprints', body)
      .pipe(map((d) => d.sprint));
  }

  /** The backend replaces name/dates on PATCH, so always send the full sprint. */
  update(id: string, body: SprintInput): Observable<Sprint> {
    return this.api
      .patch<{ sprint: Sprint }>('sprint-service', `/sprints/${id}`, body)
      .pipe(map((d) => d.sprint));
  }

  delete(id: string): Observable<void> {
    return this.api.delete('sprint-service', `/sprints/${id}`);
  }

  tasksOfSprint(sprintId: string): Observable<TaskSprint[]> {
    return this.api
      .get<{ page: Page<TaskSprint> }>('sprint-service', '/task-sprints', { sprintId, size: 1000 })
      .pipe(map((d) => d.page.content));
  }

  sprintsOfTask(taskId: string): Observable<TaskSprint[]> {
    return this.api
      .get<{ page: Page<TaskSprint> }>('sprint-service', '/task-sprints', { taskId, size: 50 })
      .pipe(map((d) => d.page.content));
  }

  addTask(sprintId: string, taskId: string): Observable<TaskSprint> {
    return this.api
      .post<{ taskSprint: TaskSprint }>('sprint-service', '/task-sprints', { taskId, sprintId })
      .pipe(map((d) => d.taskSprint));
  }

  removeTask(sprintId: string, taskId: string): Observable<void> {
    return this.api.delete('sprint-service', `/task-sprints/${taskId}/${sprintId}`);
  }
}
