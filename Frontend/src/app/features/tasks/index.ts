export { TasksComponent } from './tasks.component';

// Components
export * from './components';

// Services
export { TaskService } from './services/task.service';
export { TaskCommentService } from './services/task-comment.service';
export { TaskAttachmentService } from './services/task-attachment.service';
export { TaskHistoryService } from './services/task-history.service';
export { TaskLabelService } from './services/task-label.service';

// Re-export shared types from shared module
export type {
  PaginationParams,
  PaginatedResponse,
  JSendResponse,
} from '../../shared/models/common.model';

// Export task-specific types
export type {
  Task,
  CreateTaskRequest,
  UpdateTaskRequest,
  TaskResponse,
  TaskListResponse,
  TaskComment,
  CreateCommentRequest,
  UpdateCommentRequest,
  CommentResponse,
  CommentListResponse,
  TaskAttachment,
  UploadAttachmentResponse,
  AttachmentListResponse,
  TaskHistory,
  TaskLabel,
  CreateTaskLabelRequest,
  UpdateTaskLabelRequest,
  TaskLabelResponse,
  TaskLabelListResponse,
  HistoryListResponse,
  TaskSearchParams,
  TaskStats,
  TaskDistribution,
} from './models/task.model';
