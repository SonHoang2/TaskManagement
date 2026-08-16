import { Injectable, inject } from '@angular/core';
import { HttpService } from '../../../core/services/http.service';
import { Observable } from 'rxjs';
import { HttpParams } from '@angular/common/http';
import {
  TaskComment,
  CreateCommentRequest,
  UpdateCommentRequest,
  CommentResponse,
  CommentListResponse,
} from '../models/task.model';
import type {
  PaginationParams,
  PaginatedResponse,
  JSendResponse,
} from '../../../shared/models/common.model';

@Injectable({
  providedIn: 'root',
})
export class TaskCommentService {
  private readonly httpService = inject(HttpService);
  private readonly basePath = '/task-service';

  // Comment CRUD Operations
  getComments(
    taskId?: string,
    pagination?: PaginationParams,
  ): Observable<JSendResponse<PaginatedResponse<TaskComment> | TaskComment[]>> {
    let params = new HttpParams();
    if (taskId) {
      params = params.set('taskId', taskId);
    }
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

    return this.httpService.get<JSendResponse<PaginatedResponse<TaskComment> | TaskComment[]>>(
      `${this.basePath}/task-comments`,
      params,
    );
  }

  getComment(commentId: string): Observable<JSendResponse<CommentResponse>> {
    return this.httpService.get<JSendResponse<CommentResponse>>(
      `${this.basePath}/task-comments/${commentId}`,
    );
  }

  createComment(data: CreateCommentRequest): Observable<JSendResponse<CommentResponse>> {
    return this.httpService.post<JSendResponse<CommentResponse>>(
      `${this.basePath}/task-comments`,
      data,
    );
  }

  updateComment(
    commentId: string,
    data: UpdateCommentRequest,
  ): Observable<JSendResponse<CommentResponse>> {
    return this.httpService.patch<JSendResponse<CommentResponse>>(
      `${this.basePath}/task-comments/${commentId}`,
      data,
    );
  }

  deleteComment(commentId: string): Observable<JSendResponse<{ message: string }>> {
    return this.httpService.delete<JSendResponse<{ message: string }>>(
      `${this.basePath}/task-comments/${commentId}`,
    );
  }
}
