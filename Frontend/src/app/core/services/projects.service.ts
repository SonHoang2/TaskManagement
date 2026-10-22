import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { ApiClient, pageParams } from '../http/api-client';
import {
  Invitation,
  Label,
  Page,
  PageQuery,
  Project,
  ProjectMember,
  ProjectSummary,
} from '../models';

export type ProjectSort = 'createdat' | 'updatedat' | 'membercount' | 'taskcount';

export interface ProjectListQuery extends PageQuery {
  search?: string;
  sortBy?: ProjectSort;
  sortDirection?: 'asc' | 'desc';
}

@Injectable({ providedIn: 'root' })
export class ProjectsService {
  private readonly api = inject(ApiClient);

  /** Projects the current user is a member of. */
  mine(q: ProjectListQuery = {}): Observable<Page<ProjectSummary>> {
    return this.api.get<Page<ProjectSummary>>(
      'project-service',
      '/projects/me',
      pageParams(q, { search: q.search, sortBy: q.sortBy, sortDirection: q.sortDirection }),
    );
  }

  /** Every project on the platform (used to resolve names for invitations). */
  all(q: ProjectListQuery = {}): Observable<Page<ProjectSummary>> {
    return this.api.get<Page<ProjectSummary>>(
      'project-service',
      '/projects',
      pageParams(q, { search: q.search, sortBy: q.sortBy, sortDirection: q.sortDirection }),
    );
  }

  get(id: string): Observable<Project> {
    return this.api
      .get<{ project: Project }>('project-service', `/projects/${id}`)
      .pipe(map((d) => d.project));
  }

  create(body: { name: string; description?: string | null }): Observable<Project> {
    return this.api
      .post<{ project: Project }>('project-service', '/projects', body)
      .pipe(map((d) => d.project));
  }

  update(id: string, body: { name: string; description?: string | null }): Observable<Project> {
    return this.api
      .patch<{ project: Project }>('project-service', `/projects/${id}`, body)
      .pipe(map((d) => d.project));
  }

  delete(id: string): Observable<void> {
    return this.api.delete('project-service', `/projects/${id}`);
  }

  // ---- Members & invitations ----
  members(projectId: string): Observable<ProjectMember[]> {
    return this.api
      .get<{ members: ProjectMember[] }>('project-service', `/projects/${projectId}/members`)
      .pipe(map((d) => d.members));
  }

  invitations(projectId: string): Observable<Invitation[]> {
    return this.api
      .get<{ invitations: Invitation[] }>('project-service', `/projects/${projectId}/invitations`)
      .pipe(map((d) => d.invitations));
  }

  myInvitations(): Observable<Invitation[]> {
    return this.api
      .get<{ invitations: Invitation[] }>('project-service', '/projects/invitations/me')
      .pipe(map((d) => d.invitations));
  }

  invite(projectId: string, userId: string): Observable<Invitation> {
    return this.api
      .post<{ invitation: Invitation }>('project-service', `/projects/${projectId}/invites`, {
        userId,
      })
      .pipe(map((d) => d.invitation));
  }

  decide(invitationId: string, decision: 'ACCEPT' | 'REJECT'): Observable<Invitation> {
    return this.api
      .patch<{ invitation: Invitation }>('project-service', `/projects/invites/${invitationId}`, {
        decision,
      })
      .pipe(map((d) => d.invitation));
  }

  // ---- Labels ----
  labels(projectId: string): Observable<Label[]> {
    return this.api
      .get<{ page: Page<Label> }>('project-service', '/labels', {
        projectId,
        size: 200,
        sort: 'name,asc',
      })
      .pipe(map((d) => d.page.content));
  }

  createLabel(body: { projectId: string; name: string; color: string }): Observable<Label> {
    return this.api
      .post<{ label: Label }>('project-service', '/labels', body)
      .pipe(map((d) => d.label));
  }

  updateLabel(id: string, body: { name?: string; color?: string }): Observable<Label> {
    return this.api
      .patch<{ label: Label }>('project-service', `/labels/${id}`, body)
      .pipe(map((d) => d.label));
  }

  deleteLabel(id: string): Observable<void> {
    return this.api.delete('project-service', `/labels/${id}`);
  }
}
