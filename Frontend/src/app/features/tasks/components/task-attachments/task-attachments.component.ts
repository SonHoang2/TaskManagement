import { ChangeDetectionStrategy, Component, inject, input, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatCardModule } from '@angular/material/card';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import type { TaskAttachment } from '../../models/task.model';
import { TaskAttachmentService } from '../../services/task-attachment.service';

@Component({
  selector: 'app-task-attachments',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatCardModule,
    MatTooltipModule,
    MatProgressBarModule,
  ],
  templateUrl: './task-attachments.component.html',
  styleUrl: './task-attachments.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TaskAttachmentsComponent implements OnInit {
  private readonly taskAttachmentService = inject(TaskAttachmentService);

  readonly taskId = input.required<string>();
  readonly currentUserId = input.required<string>();

  readonly isLoading = input<boolean>(false);

  protected readonly attachments = signal<TaskAttachment[]>([]);
  protected readonly isUploading = signal(false);
  protected readonly uploadProgress = signal(0);
  protected readonly selectedFile = signal<File | null>(null);

  ngOnInit(): void {
    this.loadAttachments();
  }

  protected loadAttachments(): void {
    this.taskAttachmentService.getAttachments(this.taskId()).subscribe({
      next: (response) => {
        if (response.status === 'success' && response.data) {
          if (Array.isArray(response.data)) {
            this.attachments.set(response.data);
          } else if ('attachments' in response.data) {
            this.attachments.set((response.data as any).attachments);
          } else {
            this.attachments.set([]);
          }
        }
      },
      error: (error) => {
        console.error('Error loading attachments:', error);
      },
    });
  }

  protected onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.selectedFile.set(input.files[0]);
    }
  }

  protected onUpload(): void {
    const file = this.selectedFile();
    if (!file) {
      return;
    }

    this.isUploading.set(true);
    this.uploadProgress.set(0);

    this.taskAttachmentService.uploadAttachment(this.taskId(), this.currentUserId(), file).subscribe({
      next: (response) => {
        if (response.status === 'success' && response.data?.attachment) {
          this.attachments.update(attachments => [...attachments, response.data!.attachment]);
          this.selectedFile.set(null);
          this.uploadProgress.set(100);
          setTimeout(() => {
            this.isUploading.set(false);
            this.uploadProgress.set(0);
          }, 500);
        }
      },
      error: (error) => {
        console.error('Error uploading attachment:', error);
        this.isUploading.set(false);
        this.uploadProgress.set(0);
      },
    });
  }

  protected onDownloadAttachment(attachment: TaskAttachment): void {
    this.taskAttachmentService.downloadAttachment(attachment.id).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = attachment.fileName;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);
      },
      error: (error) => {
        console.error('Error downloading attachment:', error);
      },
    });
  }

  protected onDeleteAttachment(attachmentId: string): void {
    if (!confirm('Are you sure you want to delete this attachment?')) {
      return;
    }

    this.taskAttachmentService.deleteAttachment(attachmentId).subscribe({
      next: () => {
        this.attachments.update(attachments => 
          attachments.filter(attachment => attachment.id !== attachmentId)
        );
      },
      error: (error) => {
        console.error('Error deleting attachment:', error);
      },
    });
  }

  protected formatFileSize(bytes: number): string {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i];
  }

  protected getFileIcon(fileName: string): string {
    const extension = fileName.split('.').pop()?.toLowerCase() || '';
    const iconMap: Record<string, string> = {
      pdf: 'picture_as_pdf',
      doc: 'description',
      docx: 'description',
      xls: 'table_chart',
      xlsx: 'table_chart',
      ppt: 'slideshow',
      pptx: 'slideshow',
      jpg: 'image',
      jpeg: 'image',
      png: 'image',
      gif: 'image',
      zip: 'folder_zip',
      rar: 'folder_zip',
      txt: 'text_snippet',
    };
    return iconMap[extension] || 'insert_drive_file';
  }

  protected canDeleteAttachment(attachment: TaskAttachment): boolean {
    return attachment.uploadedBy === this.currentUserId();
  }

  protected formatDate(dateString: string): string {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
    });
  }

  protected clearSelectedFile(): void {
    this.selectedFile.set(null);
  }
}
