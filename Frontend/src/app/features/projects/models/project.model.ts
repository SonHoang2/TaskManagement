export interface Project {
  id: string;
  name: string;
  description: string;
  ownerId: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateProjectRequest {
  name: string;
  description: string;
}

export interface CreateProjectResponse {
  project: Project;
}

export interface GetProjectResponse {
  project: Project;
}

export interface UpdateProjectResponse {
  project: Project;
}

export interface UpdateProjectRequest {
  name?: string;
  description?: string;
}

export interface ProjectMember {
  id: string;
  projectId: string;
  userId: string;
  role: 'OWNER' | 'ADMIN' | 'MEMBER';
  joinedAt: string;
  user?: {
    id: string;
    name: string;
    email: string;
  };
}

export interface AddMemberRequest {
  userId: string;
  role: 'ADMIN' | 'MEMBER';
}

export interface UpdateMemberRoleRequest {
  role: 'ADMIN' | 'MEMBER';
}

export interface ProjectInvitation {
  id: string;
  projectId: string;
  project?: {
    id: string;
    name: string;
  };
  invitedBy: string;
  invitedByUser?: {
    id: string;
    name: string;
    email: string;
  };
  invitedEmail: string;
  status: 'PENDING' | 'ACCEPTED' | 'REJECTED' | 'EXPIRED';
  createdAt: string;
  expiresAt: string;
  respondedAt?: string;
}

export interface CreateInvitationRequest {
  email: string;
}

export interface InvitationResponse {
  status: 'ACCEPTED' | 'REJECTED';
}

export interface ProjectLabel {
  id: string;
  projectId: string;
  name: string;
  color: string;
  createdAt: string;
}

export interface CreateLabelRequest {
  name: string;
  color: string;
}

export interface UpdateLabelRequest {
  name?: string;
  color?: string;
}

export interface InviteMemberRequest {
  userId: string;
}

export interface DecideInvitationRequest {
  decision: 'ACCEPT' | 'REJECT';
}

export interface SearchParams {
  search?: string;
}
