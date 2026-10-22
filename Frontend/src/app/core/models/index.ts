// Types mirror the backend DTOs (see Backend/*-service/**/dto).

export interface JSend<T> {
  status: 'success' | 'fail' | 'error';
  data: T;
  message?: string;
  code?: number;
}

export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
  hasPrevious: boolean;
  numberOfElements: number;
}

export interface PageQuery {
  page?: number;
  size?: number;
  /** Spring `sort` param, e.g. `createdAt,desc` */
  sort?: string;
}

// ---------- Users ----------
export type UserRole = 'USER' | 'ADMIN';

export interface User {
  id: string;
  fullName: string;
  email: string;
  avatarUrl: string | null;
  role: UserRole;
  createdAt: string;
  updatedAt: string;
}

export interface LoginResponse {
  accessToken: string;
  tokenType: string;
  expiresInMs: number;
  user: User;
}

export interface UpdateUserRequest {
  fullName?: string;
  email?: string;
  password?: string;
  avatarUrl?: string;
}

// ---------- Projects ----------
export type MemberRole = 'OWNER' | 'ADMIN' | 'MEMBER';

export interface Project {
  id: string;
  name: string;
  description: string | null;
  ownerId: string;
  createdAt: string;
  updatedAt: string;
}

export interface ProjectSummary {
  id: string;
  name: string;
  description: string | null;
  owner: { id: string; fullName: string; avatarUrl: string | null };
  myRole: MemberRole | null;
  memberCount: number;
  members: { id: string; avatarUrl: string | null }[];
  taskStats: TaskStats;
  activeSprint: { id: string; name: string; endDate: string } | null;
  createdAt: string;
  updatedAt: string;
}

export interface ProjectMember {
  userId: string;
  role: MemberRole;
  joinedAt: string;
}

export type InvitationStatus = 'PENDING' | 'ACCEPTED' | 'REJECTED';

export interface Invitation {
  id: string;
  projectId: string;
  invitedById: string;
  inviteeId: string;
  status: InvitationStatus;
  createdAt: string;
  respondedAt: string | null;
}

export interface Label {
  id: string;
  projectId: string;
  name: string;
  color: string | null;
  createdAt: string;
}

// ---------- Tasks ----------
export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'DONE';
export type TaskPriority = 'LOW' | 'MEDIUM' | 'HIGH';

export interface Task {
  id: string;
  projectId: string;
  title: string;
  description: string | null;
  status: TaskStatus;
  priority: TaskPriority | null;
  assigneeId: string | null;
  reporterId: string | null;
  dueDate: string | null;
  startDate: string | null;
  parentTaskId: string | null;
  createdAt: string;
  updatedAt: string;
}

/** Returned by GET /tasks/project/{projectId} */
export interface TaskDetail extends Task {
  comments: Comment[];
  attachments: Attachment[];
  labels: TaskLabel[];
}

export interface TaskInput {
  projectId?: string;
  title?: string;
  description?: string | null;
  status?: TaskStatus;
  priority?: TaskPriority | null;
  assigneeId?: string | null;
  reporterId?: string | null;
  dueDate?: string | null;
  startDate?: string | null;
}

export interface TaskStats {
  total: number;
  todo: number;
  inProgress: number;
  done: number;
}

export interface Comment {
  id: string;
  taskId: string;
  userId: string;
  content: string;
  createdAt: string;
}

export interface Attachment {
  id: string;
  taskId: string;
  fileUrl: string;
  fileName: string | null;
  uploadedBy: string | null;
  createdAt: string;
}

export interface TaskLabel {
  taskId: string;
  labelId: string;
}

export interface TaskHistory {
  id: string;
  taskId: string;
  changedBy: string;
  field: string | null;
  oldValue: string | null;
  newValue: string | null;
  createdAt: string;
}

// ---------- Sprints ----------
export type SprintStatus = 'PLANNED' | 'ACTIVE' | 'COMPLETED';

export interface Sprint {
  id: string;
  projectId: string;
  name: string;
  startDate: string | null;
  endDate: string | null;
  status: SprintStatus;
  createdAt: string;
}

export interface SprintInput {
  projectId?: string;
  name: string;
  startDate?: string | null;
  endDate?: string | null;
  status?: SprintStatus;
}

export interface TaskSprint {
  taskId: string;
  sprintId: string;
}

// ---------- Notifications ----------
export type NotificationType =
  'TASK_ASSIGNED' | 'TASK_COMMENT' | 'COMMENT' | 'TASK_UPDATED' | 'PROJECT_INVITATION' | 'SYSTEM';

export interface AppNotification {
  id: string;
  userId: string;
  type: NotificationType;
  content: string | null;
  isRead: boolean;
  createdAt: string;
}

// ---------- Dashboard ----------
export interface PlatformDashboard {
  stats: {
    totalProjects: number;
    totalTasks: number;
    totalUsers: number;
    activeSprints: number;
    completedTasks: number;
    inProgressTasks: number;
    todoTasks: number;
  };
  recentProjects: {
    id: string;
    name: string;
    description: string;
    memberCount: number;
    taskCount: number;
    createdAt: string;
  }[];
  taskDistribution: { todo: number; inProgress: number; done: number };
}
