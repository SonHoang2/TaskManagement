import { Injectable, inject } from '@angular/core';
import { HttpService } from '../../../core/services/http.service';
import { Observable } from 'rxjs';
import { HttpParams } from '@angular/common/http';
import {
  TaskHistory,
  HistoryListResponse,
} from '../models/task.model';
import type {
  PaginationParams,
  PaginatedResponse,
  JSendResponse,
} from '../../../shared/models/common.model';

@Injectable({
  providedIn: 'root',
})
export class TaskHistoryService {
  private readonly httpService = inject(HttpService);
  private readonly basePath = '/task-service';

  // History Operations
  getHistory(
    taskId: string,
    pagination?: PaginationParams,
  ): Observable<JSendResponse<PaginatedResponse<TaskHistory> | TaskHistory[]>> {
    let params = new HttpParams().set('taskId', taskId);
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

    return this.httpService.get<
      JSendResponse<PaginatedResponse<TaskHistory> | TaskHistory[]>
    >(`${this.basePath}/task-histories`, params);
  }

  getTaskHistory(
    pagination?: PaginationParams,
  ): Observable<JSendResponse<PaginatedResponse<TaskHistory> | TaskHistory[]>> {
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

    return this.httpService.get<
      JSendResponse<PaginatedResponse<TaskHistory> | TaskHistory[]>
    >(`${this.basePath}/task-histories`, params);
  }

  getHistoryEntry(historyId: string): Observable<JSendResponse<TaskHistory>> {
    return this.httpService.get<JSendResponse<TaskHistory>>(
      `${this.basePath}/task-histories/${historyId}`,
    );
  }
}
