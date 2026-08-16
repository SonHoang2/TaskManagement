import { Injectable, inject } from '@angular/core';
import { HttpService } from '../../../core/services/http.service';
import { Observable } from 'rxjs';
import { HttpParams } from '@angular/common/http';
import {
  TaskAttachment,
  UploadAttachmentResponse,
  AttachmentListResponse,
} from '../models/task.model';
import type {
  PaginationParams,
  PaginatedResponse,
  JSendResponse,
} from '../../../shared/models/common.model';

@Injectable({
  providedIn: 'root',
})
export class TaskAttachmentService {
  private readonly httpService = inject(HttpService);
  private readonly basePath = '/task-service';

  // Attachment CRUD Operations
  getAttachments(
    taskId: string,
    pagination?: PaginationParams,
  ): Observable<JSendResponse<PaginatedResponse<TaskAttachment> | TaskAttachment[]>> {
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
      JSendResponse<PaginatedResponse<TaskAttachment> | TaskAttachment[]>
    >(`${this.basePath}/tasks/${taskId}/attachments`, params);
  }

  getAttachment(attachmentId: string): Observable<JSendResponse<UploadAttachmentResponse>> {
    return this.httpService.get<JSendResponse<UploadAttachmentResponse>>(
      `${this.basePath}/attachments/${attachmentId}`,
    );
  }

  uploadAttachment(
    taskId: string,
    userId: string,
    file: File,
  ): Observable<JSendResponse<UploadAttachmentResponse>> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('uploadedBy', userId);

    return this.httpService.postFormData<JSendResponse<UploadAttachmentResponse>>(
      `${this.basePath}/tasks/${taskId}/attachments`,
      formData,
    );
  }

  deleteAttachment(attachmentId: string): Observable<JSendResponse<{ message: string }>> {
    return this.httpService.delete<JSendResponse<{ message: string }>>(
      `${this.basePath}/attachments/${attachmentId}`,
    );
  }

  downloadAttachment(attachmentId: string): Observable<Blob> {
    return this.httpService.getBlob(`${this.basePath}/attachments/${attachmentId}/download`);
  }
}
