import { ChangeDetectionStrategy, Component, inject, input, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatCardModule } from '@angular/material/card';
import { MatTooltipModule } from '@angular/material/tooltip';
import type { TaskComment, CreateCommentRequest, UpdateCommentRequest } from '../../models/task.model';
import { TaskCommentService } from '../../services/task-comment.service';

@Component({
  selector: 'app-task-comments',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatCardModule,
    MatTooltipModule,
  ],
  templateUrl: './task-comments.component.html',
  styleUrl: './task-comments.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TaskCommentsComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly taskCommentService = inject(TaskCommentService);

  readonly taskId = input.required<string>();
  readonly currentUserId = input.required<string>();

  readonly isLoading = input<boolean>(false);

  protected readonly comments = signal<TaskComment[]>([]);
  protected readonly isSubmitting = signal(false);
  protected readonly editingCommentId = signal<string | null>(null);

  protected readonly commentForm: FormGroup = this.fb.group({
    content: ['', [Validators.required, Validators.minLength(1), Validators.maxLength(1000)]],
  });

  protected readonly editForm: FormGroup = this.fb.group({
    content: ['', [Validators.required, Validators.minLength(1), Validators.maxLength(1000)]],
  });

  ngOnInit(): void {
    this.loadComments();
  }

  protected loadComments(): void {
    this.taskCommentService.getComments(this.taskId()).subscribe({
      next: (response) => {
        if (response.status === 'success' && response.data) {
          if (Array.isArray(response.data)) {
            this.comments.set(response.data);
          } else if ('comments' in response.data) {
            this.comments.set((response.data as any).comments);
          } else if ('content' in response.data) {
            this.comments.set((response.data as any).content);
          } else {
            this.comments.set([]);
          }
        }
      },
      error: (error) => {
        console.error('Error loading comments:', error);
      },
    });
  }

  protected onSubmitComment(): void {
    if (this.commentForm.invalid) {
      this.commentForm.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);

    const request: CreateCommentRequest = {
      taskId: this.taskId(),
      userId: this.currentUserId(),
      content: this.commentForm.value.content,
    };

    this.taskCommentService.createComment(request).subscribe({
      next: (response) => {
        if (response.status === 'success' && response.data?.comment) {
          this.comments.update(comments => [...comments, response.data!.comment]);
          this.commentForm.reset();
        }
        this.isSubmitting.set(false);
      },
      error: (error) => {
        console.error('Error creating comment:', error);
        this.isSubmitting.set(false);
      },
    });
  }

  protected onStartEdit(comment: TaskComment): void {
    this.editingCommentId.set(comment.id);
    this.editForm.patchValue({ content: comment.content });
  }

  protected onCancelEdit(): void {
    this.editingCommentId.set(null);
    this.editForm.reset();
  }

  protected onSubmitEdit(commentId: string): void {
    if (this.editForm.invalid) {
      this.editForm.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);

    const request: UpdateCommentRequest = {
      content: this.editForm.value.content,
      userId: this.currentUserId(),
    };

    this.taskCommentService.updateComment(commentId, request).subscribe({
      next: (response) => {
        if (response.status === 'success' && response.data?.comment) {
          this.comments.update(comments =>
            comments.map(comment =>
              comment.id === commentId ? response.data!.comment : comment
            )
          );
          this.editingCommentId.set(null);
          this.editForm.reset();
        }
        this.isSubmitting.set(false);
      },
      error: (error) => {
        console.error('Error updating comment:', error);
        this.isSubmitting.set(false);
      },
    });
  }

  protected onDeleteComment(commentId: string): void {
    this.taskCommentService.deleteComment(commentId).subscribe({
      next: () => {
        this.comments.update(comments => comments.filter(comment => comment.id !== commentId));
      },
      error: (error) => {
        console.error('Error deleting comment:', error);
      },
    });
  }

  protected formatDate(dateString: string): string {
    const date = new Date(dateString);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMs / 3600000);
    const diffDays = Math.floor(diffMs / 86400000);

    if (diffMins < 1) return 'Just now';
    if (diffMins < 60) return `${diffMins}m ago`;
    if (diffHours < 24) return `${diffHours}h ago`;
    if (diffDays < 7) return `${diffDays}d ago`;

    return date.toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
    });
  }

  protected canEditComment(comment: TaskComment): boolean {
    return comment.userId === this.currentUserId();
  }

  protected canDeleteComment(comment: TaskComment): boolean {
    return comment.userId === this.currentUserId();
  }
}
