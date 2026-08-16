import { Injectable, inject } from '@angular/core';
import { HttpService } from '../../../core/services/http.service';
import { Observable } from 'rxjs';
import { HttpParams } from '@angular/common/http';
import {
  Task,
  CreateTaskRequest,
  UpdateTaskRequest,
  TaskResponse,
  TaskListResponse,
  TaskSearchParams,
  TaskStats,
  TaskDistribution,
} from '../models/task.model';
import type {
  PaginationParams,
  PaginatedResponse,
  PageWrapper,
  JSendResponse,
} from '../../../shared/models/common.model';

@Injectable({
  providedIn: 'root',
})
export class TaskService {
  private readonly httpService = inject(HttpService);
  private readonly basePath = '/task-service';

  // Task CRUD Operations
  getTasks(
    pagination: PaginationParams,
    search?: TaskSearchParams,
  ): Observable<JSendResponse<PageWrapper<Task>>> {
    let params = new HttpParams()
      .set('page', pagination.page.toString())
      .set('size', pagination.size.toString());

    if (pagination.sort) {
      params = params.set('sort', pagination.sort);
    }
    if (pagination.direction) {
      params = params.set('direction', pagination.direction);
    }

    if (search) {
      if (search.projectId) {
        params = params.set('projectId', search.projectId);
      }
      if (search.status) {
        params = params.set('status', search.status);
      }
      if (search.priority) {
        params = params.set('priority', search.priority);
      }
      if (search.assigneeId) {
        params = params.set('assigneeId', search.assigneeId);
      }
      if (search.search) {
        params = params.set('keyword', search.search);
      }
      if (search.dueDateFrom) {
        params = params.set('dueDateFrom', search.dueDateFrom);
      }
      if (search.dueDateTo) {
        params = params.set('dueDateTo', search.dueDateTo);
      }
    }

    return this.httpService.get<JSendResponse<PageWrapper<Task>>>(
      `${this.basePath}/tasks`,
      params,
    );
  }

  getTask(taskId: string): Observable<JSendResponse<TaskResponse>> {
    return this.httpService.get<JSendResponse<TaskResponse>>(`${this.basePath}/tasks/${taskId}`);
  }

  createTask(data: CreateTaskRequest): Observable<JSendResponse<TaskResponse>> {
    return this.httpService.post<JSendResponse<TaskResponse>>(`${this.basePath}/tasks`, data);
  }

  updateTask(taskId: string, data: UpdateTaskRequest): Observable<JSendResponse<TaskResponse>> {
    return this.httpService.patch<JSendResponse<TaskResponse>>(
      `${this.basePath}/tasks/${taskId}`,
      data,
    );
  }

  deleteTask(taskId: string): Observable<JSendResponse<{ message: string }>> {
    return this.httpService.delete<JSendResponse<{ message: string }>>(
      `${this.basePath}/tasks/${taskId}`,
    );
  }

  // Task Statistics
  getTaskStats(projectId: string): Observable<JSendResponse<TaskStats>> {
    return this.httpService.get<JSendResponse<TaskStats>>(
      `${this.basePath}/tasks/project/${projectId}/stats`,
    );
  }

  getTaskDistribution(
    pagination?: PaginationParams,
    search?: TaskSearchParams,
  ): Observable<JSendResponse<TaskDistribution>> {
    let params = new HttpParams();
    if (pagination) {
      params = params
        .set('page', pagination.page.toString())
        .set('size', pagination.size.toString());
      if (pagination.sort) {
        params = params.set('sort', pagination.sort);
      }
      if (pagination.direction) {
        params = params.set('direction', pagination.direction);
      }
    }

    if (search) {
      if (search.status) {
        params = params.set('status', search.status);
      }
      if (search.priority) {
        params = params.set('priority', search.priority);
      }
      if (search.assigneeId) {
        params = params.set('assigneeId', search.assigneeId);
      }
      if (search.search) {
        params = params.set('keyword', search.search);
      }
    }

    return this.httpService.get<JSendResponse<TaskDistribution>>(
      `${this.basePath}/tasks/distribution`,
      params,
    );
  }
}
