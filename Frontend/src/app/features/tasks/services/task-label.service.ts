import { Injectable, inject } from '@angular/core';
import { HttpService } from '../../../core/services/http.service';
import { Observable } from 'rxjs';
import { HttpParams } from '@angular/common/http';
import {
  TaskLabel,
  CreateTaskLabelRequest,
  UpdateTaskLabelRequest,
  TaskLabelResponse,
  TaskLabelListResponse,
} from '../models/task.model';
import type {
  PaginationParams,
  PaginatedResponse,
  JSendResponse,
} from '../../../shared/models/common.model';

@Injectable({
  providedIn: 'root',
})
export class TaskLabelService {
  private readonly httpService = inject(HttpService);
  private readonly basePath = '/task-service';

  // Task Label CRUD Operations
  getTaskLabels(
    pagination?: PaginationParams,
  ): Observable<JSendResponse<PaginatedResponse<TaskLabel> | TaskLabel[]>> {
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

    return this.httpService.get<JSendResponse<PaginatedResponse<TaskLabel> | TaskLabel[]>>(
      `${this.basePath}/task-labels`,
      params,
    );
  }

  getTaskLabel(labelId: string): Observable<JSendResponse<TaskLabelResponse>> {
    return this.httpService.get<JSendResponse<TaskLabelResponse>>(
      `${this.basePath}/task-labels/${labelId}`,
    );
  }

  createTaskLabel(data: CreateTaskLabelRequest): Observable<JSendResponse<TaskLabelResponse>> {
    return this.httpService.post<JSendResponse<TaskLabelResponse>>(
      `${this.basePath}/task-labels`,
      data,
    );
  }

  updateTaskLabel(
    labelId: string,
    data: UpdateTaskLabelRequest,
  ): Observable<JSendResponse<TaskLabelResponse>> {
    return this.httpService.patch<JSendResponse<TaskLabelResponse>>(
      `${this.basePath}/task-labels/${labelId}`,
      data,
    );
  }

  deleteTaskLabel(labelId: string): Observable<JSendResponse<{ message: string }>> {
    return this.httpService.delete<JSendResponse<{ message: string }>>(
      `${this.basePath}/task-labels/${labelId}`,
    );
  }
}
