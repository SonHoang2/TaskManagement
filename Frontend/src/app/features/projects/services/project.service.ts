import { Injectable, inject } from '@angular/core';
import { HttpService } from '../../../core/services/http.service';
import { Observable } from 'rxjs';
import { HttpParams } from '@angular/common/http';
import {
  Project,
  CreateProjectRequest,
  CreateProjectResponse,
  GetProjectResponse,
  UpdateProjectRequest,
  UpdateProjectResponse,
  ProjectMember,
  AddMemberRequest,
  UpdateMemberRoleRequest,
  ProjectInvitation,
  CreateInvitationRequest,
  InvitationResponse,
  ProjectLabel,
  CreateLabelRequest,
  UpdateLabelRequest,
  InviteMemberRequest,
  DecideInvitationRequest,
  SearchParams,
} from '../models/project.model';
import type {
  PaginationParams,
  PaginatedResponse,
  JSendResponse,
} from '../../../shared/models/common.model';
import type { Task } from '../../tasks/models/task.model';

// Re-export types for convenience
export type {
  Project,
  CreateProjectRequest,
  CreateProjectResponse,
  GetProjectResponse,
  UpdateProjectRequest,
  UpdateProjectResponse,
  ProjectMember,
  AddMemberRequest,
  UpdateMemberRoleRequest,
  ProjectInvitation,
  CreateInvitationRequest,
  InvitationResponse,
  ProjectLabel,
  CreateLabelRequest,
  UpdateLabelRequest,
  InviteMemberRequest,
  DecideInvitationRequest,
  SearchParams,
  Task,
};

@Injectable({
  providedIn: 'root',
})
export class ProjectService {
  private readonly httpService = inject(HttpService);
  private readonly basePath = '/project-service';

  // Project CRUD Operations
  getProjects(
    pagination: PaginationParams,
    search?: SearchParams,
  ): Observable<JSendResponse<PaginatedResponse<Project>>> {
    let params = new HttpParams()
      .set('page', pagination.page.toString())
      .set('size', pagination.size.toString());

    if (pagination.sort) {
      params = params.set('sort', pagination.sort);
    }
    if (pagination.direction) {
      params = params.set('direction', pagination.direction);
    }
    if (search?.search) {
      params = params.set('search', search.search);
    }

    return this.httpService.get<JSendResponse<PaginatedResponse<Project>>>(
      `${this.basePath}/projects`,
      params,
    );
  }

  getProject(projectId: string): Observable<JSendResponse<GetProjectResponse>> {
    return this.httpService.get<JSendResponse<GetProjectResponse>>(
      `${this.basePath}/projects/${projectId}`,
    );
  }

  getMyProjects(
    pagination: PaginationParams,
    search?: SearchParams,
  ): Observable<JSendResponse<PaginatedResponse<Project>>> {
    let params = new HttpParams()
      .set('page', pagination.page.toString())
      .set('size', pagination.size.toString());

    if (pagination.sort) {
      params = params.set('sortBy', pagination.sort);
    }
    if (pagination.direction) {
      params = params.set('sortDirection', pagination.direction);
    }
    if (search?.search) {
      params = params.set('search', search.search);
    }

    return this.httpService.get<JSendResponse<PaginatedResponse<Project>>>(
      `${this.basePath}/projects/me`,
      params,
    );
  }

  createProject(data: CreateProjectRequest): Observable<JSendResponse<CreateProjectResponse>> {
    return this.httpService.post<JSendResponse<CreateProjectResponse>>(
      `${this.basePath}/projects`,
      data,
    );
  }

  updateProject(
    projectId: string,
    data: UpdateProjectRequest,
  ): Observable<JSendResponse<UpdateProjectResponse>> {
    return this.httpService.put<JSendResponse<UpdateProjectResponse>>(
      `${this.basePath}/projects/${projectId}`,
      data,
    );
  }

  deleteProject(projectId: string): Observable<JSendResponse<{ message: string }>> {
    return this.httpService.delete<JSendResponse<{ message: string }>>(
      `${this.basePath}/projects/${projectId}`,
    );
  }

  // Project Members Operations
  getProjectMembers(
    projectId: string,
    pagination?: PaginationParams,
  ): Observable<JSendResponse<PaginatedResponse<ProjectMember> | ProjectMember[]>> {
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

    return this.httpService.get<JSendResponse<PaginatedResponse<ProjectMember> | ProjectMember[]>>(
      `${this.basePath}/projects/${projectId}/members`,
      params,
    );
  }

  addMember(projectId: string, data: AddMemberRequest): Observable<JSendResponse<ProjectMember>> {
    return this.httpService.post<JSendResponse<ProjectMember>>(
      `${this.basePath}/projects/${projectId}/members`,
      data,
    );
  }

  updateMemberRole(
    projectId: string,
    memberId: string,
    data: UpdateMemberRoleRequest,
  ): Observable<JSendResponse<ProjectMember>> {
    return this.httpService.put<JSendResponse<ProjectMember>>(
      `${this.basePath}/projects/${projectId}/members/${memberId}`,
      data,
    );
  }

  removeMember(
    projectId: string,
    memberId: string,
  ): Observable<JSendResponse<{ message: string }>> {
    return this.httpService.delete<JSendResponse<{ message: string }>>(
      `${this.basePath}/projects/${projectId}/members/${memberId}`,
    );
  }

  // Project Invitations Operations
  getProjectInvitations(
    projectId: string,
    pagination?: PaginationParams,
  ): Observable<JSendResponse<PaginatedResponse<ProjectInvitation> | ProjectInvitation[]>> {
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
      JSendResponse<PaginatedResponse<ProjectInvitation> | ProjectInvitation[]>
    >(`${this.basePath}/projects/${projectId}/invitations`, params);
  }

  inviteMember(
    projectId: string,
    data: InviteMemberRequest,
  ): Observable<JSendResponse<ProjectInvitation>> {
    return this.httpService.post<JSendResponse<ProjectInvitation>>(
      `${this.basePath}/projects/${projectId}/invites`,
      data,
    );
  }

  decideInvitation(
    invitationId: string,
    data: DecideInvitationRequest,
  ): Observable<JSendResponse<ProjectInvitation>> {
    return this.httpService.patch<JSendResponse<ProjectInvitation>>(
      `${this.basePath}/projects/invites/${invitationId}`,
      data,
    );
  }

  createInvitation(
    projectId: string,
    data: CreateInvitationRequest,
  ): Observable<JSendResponse<ProjectInvitation>> {
    return this.httpService.post<JSendResponse<ProjectInvitation>>(
      `${this.basePath}/projects/${projectId}/invitations`,
      data,
    );
  }

  respondToInvitation(
    invitationId: string,
    data: InvitationResponse,
  ): Observable<JSendResponse<ProjectInvitation>> {
    return this.httpService.post<JSendResponse<ProjectInvitation>>(
      `${this.basePath}/invitations/${invitationId}/respond`,
      data,
    );
  }

  getMyInvitations(
    pagination?: PaginationParams,
  ): Observable<JSendResponse<PaginatedResponse<ProjectInvitation> | ProjectInvitation[]>> {
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
      JSendResponse<PaginatedResponse<ProjectInvitation> | ProjectInvitation[]>
    >(`${this.basePath}/invitations/my`, params);
  }

  cancelInvitation(
    projectId: string,
    invitationId: string,
  ): Observable<JSendResponse<{ message: string }>> {
    return this.httpService.delete<JSendResponse<{ message: string }>>(
      `${this.basePath}/projects/${projectId}/invitations/${invitationId}`,
    );
  }

  // Project Labels Operations
  getProjectLabels(
    projectId: string,
    pagination?: PaginationParams,
  ): Observable<JSendResponse<PaginatedResponse<ProjectLabel> | ProjectLabel[]>> {
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

    return this.httpService.get<JSendResponse<PaginatedResponse<ProjectLabel> | ProjectLabel[]>>(
      `${this.basePath}/projects/${projectId}/labels`,
      params,
    );
  }

  createLabel(
    projectId: string,
    data: CreateLabelRequest,
  ): Observable<JSendResponse<ProjectLabel>> {
    return this.httpService.post<JSendResponse<ProjectLabel>>(
      `${this.basePath}/projects/${projectId}/labels`,
      data,
    );
  }

  updateLabel(
    projectId: string,
    labelId: string,
    data: UpdateLabelRequest,
  ): Observable<JSendResponse<ProjectLabel>> {
    return this.httpService.put<JSendResponse<ProjectLabel>>(
      `${this.basePath}/projects/${projectId}/labels/${labelId}`,
      data,
    );
  }

  deleteLabel(projectId: string, labelId: string): Observable<JSendResponse<{ message: string }>> {
    return this.httpService.delete<JSendResponse<{ message: string }>>(
      `${this.basePath}/projects/${projectId}/labels/${labelId}`,
    );
  }

  // Project Tasks Operations
  getProjectTasks(
    projectId: string,
    pagination: PaginationParams,
  ): Observable<JSendResponse<PaginatedResponse<Task>>> {
    let params = new HttpParams()
      .set('page', pagination.page.toString())
      .set('size', pagination.size.toString());

    if (pagination.sort) {
      params = params.set('sort', pagination.sort);
    }
    if (pagination.direction) {
      params = params.set('direction', pagination.direction);
    }

    return this.httpService.get<JSendResponse<PaginatedResponse<Task>>>(
      `${this.basePath}/projects/${projectId}/tasks`,
      params,
    );
  }
}
