import { ChangeDetectionStrategy, Component, input, output, inject } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { CommonModule } from '@angular/common';
import type { Task } from '../../models/task.model';

@Component({
  selector: 'app-task-card',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatIconModule,
    MatButtonModule,
    MatMenuModule,
    MatChipsModule,
    MatDividerModule,
  ],
  templateUrl: './task-card.component.html',
  styleUrl: './task-card.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TaskCardComponent {
  readonly task = input.required<Task>();
  readonly isDragging = input<boolean>(false);
  readonly showDragHandle = input<boolean>(false);

  readonly edit = output<Task>();
  readonly viewDetails = output<Task>();
  readonly delete = output<string>();

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
        return 'Completed';
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
    });
  }

  protected onEdit(): void {
    this.edit.emit(this.task());
  }

  protected onViewDetails(): void {
    this.viewDetails.emit(this.task());
  }

  protected onDelete(): void {
    this.delete.emit(this.task().id);
  }
}
