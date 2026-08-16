export interface Task {
  id: string;
  projectId: string;
  title: string;
  description?: string;
  status: 'TODO' | 'IN_PROGRESS' | 'DONE';
  priority: 'LOW' | 'MEDIUM' | 'HIGH';
  assigneeId?: string;
  assignee?: {
    id: string;
    name: string;
    email: string;
  };
  dueDate?: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateTaskRequest {
  projectId: string;
  title: string;
  description?: string;
  status?: 'TODO' | 'IN_PROGRESS' | 'DONE';
  priority?: 'LOW' | 'MEDIUM' | 'HIGH';
  assigneeId?: string;
  dueDate?: string;
}

export interface UpdateTaskRequest {
  title?: string;
  description?: string;
  status?: 'TODO' | 'IN_PROGRESS' | 'DONE';
  priority?: 'LOW' | 'MEDIUM' | 'HIGH';
  assigneeId?: string;
  dueDate?: string;
}

export interface TaskResponse {
  task: Task;
}

export interface TaskListResponse {
  tasks: Task[];
}

export interface TaskComment {
  id: string;
  taskId: string;
  userId: string;
  content: string;
  createdAt: string;
  updatedAt: string;
  user?: {
    id: string;
    name: string;
    email: string;
  };
}

export interface CreateCommentRequest {
  taskId: string;
  userId: string;
  content: string;
}

export interface UpdateCommentRequest {
  content: string;
  userId?: string;
}

export interface CommentResponse {
  comment: TaskComment;
}

export interface CommentListResponse {
  comments: TaskComment[];
}

export interface TaskAttachment {
  id: string;
  taskId: string;
  fileName: string;
  fileSize: number;
  mimeType: string;
  fileUrl: string;
  uploadedBy: string;
  uploadedAt: string;
  uploader?: {
    id: string;
    name: string;
    email: string;
  };
}

export interface UploadAttachmentResponse {
  attachment: TaskAttachment;
}

export interface AttachmentListResponse {
  attachments: TaskAttachment[];
}

export interface TaskHistory {
  id: string;
  taskId: string;
  userId: string;
  action:
    | 'CREATED'
    | 'UPDATED'
    | 'DELETED'
    | 'ASSIGNED'
    | 'UNASSIGNED'
    | 'STATUS_CHANGED'
    | 'PRIORITY_CHANGED'
    | 'COMMENT_ADDED'
    | 'ATTACHMENT_ADDED'
    | 'ATTACHMENT_REMOVED';
  field?: string;
  oldValue?: string;
  newValue?: string;
  description?: string;
  createdAt: string;
  user?: {
    id: string;
    name: string;
    email: string;
  };
}

export interface TaskLabel {
  id: string;
  taskId: string;
  changedBy: string;
  field: string;
  oldValue?: string;
  newValue?: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateTaskLabelRequest {
  taskId: string;
  changedBy: string;
  field: string;
  oldValue?: string;
  newValue?: string;
}

export interface UpdateTaskLabelRequest {
  field?: string;
  oldValue?: string;
  newValue?: string;
}

export interface TaskLabelResponse {
  label: TaskLabel;
}

export interface TaskLabelListResponse {
  labels: TaskLabel[];
}

export interface HistoryListResponse {
  history: TaskHistory[];
}

export interface TaskSearchParams {
  projectId?: string;
  status?: 'TODO' | 'IN_PROGRESS' | 'DONE';
  priority?: 'LOW' | 'MEDIUM' | 'HIGH';
  assigneeId?: string;
  search?: string; // mapped to 'keyword' in API
  dueDateFrom?: string;
  dueDateTo?: string;
}

export interface TaskStats {
  total: number;
  byStatus: {
    TODO: number;
    IN_PROGRESS: number;
    DONE: number;
  };
  byPriority: {
    LOW: number;
    MEDIUM: number;
    HIGH: number;
  };
  overdue: number;
  completedThisWeek: number;
}

export interface TaskDistribution {
  byAssignee: {
    assigneeId: string;
    assigneeName: string;
    count: number;
  }[];
  byStatus: {
    status: 'TODO' | 'IN_PROGRESS' | 'DONE';
    count: number;
  }[];
  byPriority: {
    priority: 'LOW' | 'MEDIUM' | 'HIGH';
    count: number;
  }[];
}
