export { ProjectsComponent } from './projects.component';
export { ProjectService } from './services/project.service';

// Export project-specific types only
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
} from './models/project.model';

// Re-export shared types from shared module
export type {
  PaginationParams,
  PaginatedResponse,
  JSendResponse,
} from '../../shared/models/common.model';

// Components
export { ProjectListComponent } from './components/project-list/project-list.component';
export { ProjectDetailComponent } from './components/project-detail/project-detail.component';
export { ProjectFormComponent } from './components/project-form/project-form.component';
export { ProjectMembersComponent } from './components/project-members/project-members.component';
export { ProjectInvitationsComponent } from './components/project-invitations/project-invitations.component';
export { MyInvitationsComponent } from './components/my-invitations/my-invitations.component';
