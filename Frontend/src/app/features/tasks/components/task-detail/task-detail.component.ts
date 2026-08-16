import { ChangeDetectionStrategy, Component, inject, input, OnInit, signal, computed } from '@angular/core';
import { take } from 'rxjs/operators';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTabsModule } from '@angular/material/tabs';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import type { Task, TaskHistory } from '../../models/task.model';
import { TaskService } from '../../services/task.service';
import { TaskHistoryService } from '../../services/task-history.service';
import { TaskFormComponent } from '../task-form';
import { TaskCommentsComponent } from '../task-comments';
import { TaskAttachmentsComponent } from '../task-attachments';

@Component({
  selector: 'app-task-detail',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatTabsModule,
    MatChipsModule,
    MatDividerModule,
    MatDialogModule,
    TaskCommentsComponent,
    TaskAttachmentsComponent,
  ],
  templateUrl: './task-detail.component.html',
  styleUrl: './task-detail.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TaskDetailComponent implements OnInit {
  private readonly taskService = inject(TaskService);
  private readonly taskHistoryService = inject(TaskHistoryService);
  private readonly dialog = inject(MatDialog);

  readonly taskId = input.required<string>();
  readonly currentUserId = input.required<string>();
  readonly availableUsers = input<{ id: string; name: string; email: string }[]>([]);

  protected readonly task = signal<Task | null>(null);
  protected readonly isLoading = signal(false);
  protected readonly history = signal<TaskHistory[]>([]);
  protected readonly isLoadingHistory = signal(false);
  protected readonly selectedTab = signal(0);

  ngOnInit(): void {
    this.loadTask();
    this.loadHistory();
  }

  protected loadTask(): void {
    this.isLoading.set(true);
    this.taskService.getTask(this.taskId()).subscribe({
      next: (response) => {
        if (response.status === 'success' && response.data) {
          this.task.set(response.data.task);
        }
        this.isLoading.set(false);
      },
      error: (error) => {
        console.error('Error loading task:', error);
        this.isLoading.set(false);
      },
    });
  }

  protected loadHistory(): void {
    this.isLoadingHistory.set(true);
    this.taskHistoryService.getHistory(this.taskId()).subscribe({
      next: (response) => {
        if (response.status === 'success' && response.data) {
          if (Array.isArray(response.data)) {
            this.history.set(response.data);
          } else if ('history' in response.data) {
            this.history.set((response.data as any).history);
          } else {
            this.history.set([]);
          }
        }
        this.isLoadingHistory.set(false);
      },
      error: (error) => {
        console.error('Error loading history:', error);
        this.isLoadingHistory.set(false);
      },
    });
  }

  protected onEditTask(): void {
    if (!this.task()) return;

    const dialogRef = this.dialog.open(TaskFormComponent, {
      width: '600px',
      data: {
        task: this.task(),
        projectId: this.task()!.projectId,
        availableUsers: this.availableUsers(),
      },
    });

    dialogRef.afterClosed().subscribe((result) => {
      if (result) {
        this.taskService.updateTask(this.taskId(), result).subscribe({
          next: (response) => {
            if (response.status === 'success' && response.data?.task) {
              this.task.set(response.data.task);
              this.loadHistory();
            }
          },
          error: (error) => {
            console.error('Error updating task:', error);
          },
        });
      }
    });
  }

  protected getStatusColor(status: string): string {
    switch (status) {
      case 'DONE':
        return 'primary';
      case 'IN_PROGRESS':
        return 'accent';
      case 'TODO':
        return 'warn';
      default:
        return '';
    }
  }

  protected getPriorityColor(priority: string): string {
    switch (priority) {
      case 'HIGH':
        return 'warn';
      case 'MEDIUM':
        return 'accent';
      case 'LOW':
        return 'primary';
      default:
        return '';
    }
  }

  protected formatStatus(status: string): string {
    switch (status) {
      case 'TODO':
        return 'To Do';
      case 'IN_PROGRESS':
        return 'In Progress';
      case 'DONE':
        return 'Done';
      default:
        return status;
    }
  }

  protected formatPriority(priority: string): string {
    switch (priority) {
      case 'LOW':
        return 'Low';
      case 'MEDIUM':
        return 'Medium';
      case 'HIGH':
        return 'High';
      default:
        return priority;
    }
  }

  protected formatDate(dateString: string): string {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  }

  protected formatAction(action: string): string {
    return action.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, l => l.toUpperCase());
  }

  protected getActionIcon(action: string): string {
    const iconMap: Record<string, string> = {
      CREATED: 'add_circle',
      UPDATED: 'edit',
      DELETED: 'delete',
      ASSIGNED: 'person_add',
      UNASSIGNED: 'person_remove',
      STATUS_CHANGED: 'swap_horiz',
      PRIORITY_CHANGED: 'flag',
      COMMENT_ADDED: 'comment',
      ATTACHMENT_ADDED: 'attach_file',
      ATTACHMENT_REMOVED: 'attachment',
    };
    return iconMap[action] || 'history';
  }

  protected getActionColor(action: string): string {
    const colorMap: Record<string, string> = {
      CREATED: 'primary',
      UPDATED: 'accent',
      DELETED: 'warn',
      ASSIGNED: 'primary',
      UNASSIGNED: 'warn',
      STATUS_CHANGED: 'accent',
      PRIORITY_CHANGED: 'warn',
      COMMENT_ADDED: 'primary',
      ATTACHMENT_ADDED: 'accent',
      ATTACHMENT_REMOVED: 'warn',
    };
    return colorMap[action] || '';
  }
}
