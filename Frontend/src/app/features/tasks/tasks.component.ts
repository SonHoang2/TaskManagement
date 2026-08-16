import { Component, signal, computed, inject, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDividerModule } from '@angular/material/divider';
import { FormsModule } from '@angular/forms';
import { TaskService } from './services/task.service';
import { TaskFormComponent } from './components/task-form';
import { TaskDetailComponent } from './components/task-detail';
import type { Task, TaskSearchParams } from './models/task.model';
import type { PaginationParams } from '../../shared/models/common.model';

@Component({
  selector: 'app-tasks',
  standalone: true,
  imports: [
    MatCardModule,
    MatIconModule,
    MatButtonModule,
    MatMenuModule,
    MatInputModule,
    MatSelectModule,
    MatFormFieldModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    MatDividerModule,
    MatDialogModule,
    FormsModule,
  ],
  templateUrl: './tasks.component.html',
  styleUrl: './tasks.component.scss',
})
export class TasksComponent implements OnInit {
  private readonly taskService = inject(TaskService);
  private readonly dialog = inject(MatDialog);
  private readonly route = inject(ActivatedRoute);

  readonly isLoading = signal(false);
  readonly searchTerm = signal('');
  readonly currentPage = signal(0);
  readonly pageSize = signal(10);
  readonly totalElements = signal(0);
  readonly totalPages = signal(0);

  readonly tasks = signal<Task[]>([]);

  readonly resultsRange = computed(() => {
    const start = this.currentPage() * this.pageSize() + 1;
    const end = Math.min((this.currentPage() + 1) * this.pageSize(), this.totalElements());
    return { start, end };
  });

  ngOnInit(): void {
    this.loadTasks();
  }

  loadTasks(): void {
    this.isLoading.set(true);
    const pagination: PaginationParams = {
      page: this.currentPage(),
      size: this.pageSize(),
    };

    const search: TaskSearchParams = {};
    if (this.searchTerm()) {
      search.search = this.searchTerm();
    }

    this.taskService.getTasks(pagination, search).subscribe({
      next: (response) => {
        console.log('Tasks response:', response);
        if (response.status === 'success' && response.data?.page) {
          const { page } = response.data;
          this.tasks.set(page.content);
          this.totalElements.set(page.totalElements);
          this.totalPages.set(page.totalPages);
          console.log('Tasks loaded:', page.content.length);
        } else {
          console.log('Response status:', response.status);
          console.log('Response message:', response.message);
          // Handle case where response might be successful but no data
          if (response.status === 'success') {
            this.tasks.set([]);
            this.totalElements.set(0);
            this.totalPages.set(0);
          }
        }
        this.isLoading.set(false);
      },
      error: (error) => {
        console.error('Error loading tasks:', error);
        this.isLoading.set(false);
      },
    });
  }

  getStatusColor(status: string): string {
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

  getPriorityColor(priority: string): string {
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

  formatStatus(status: string): string {
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

  formatPriority(priority: string): string {
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

  formatDate(dateString: string): string {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    });
  }

  onSearchChange(value: string) {
    this.searchTerm.set(value);
    this.currentPage.set(0);
    this.loadTasks();
  }

  clearFilters(): void {
    this.searchTerm.set('');
    this.currentPage.set(0);
    this.loadTasks();
  }

  deleteTask(taskId: string) {
    if (!confirm('Are you sure you want to delete this task?')) return;

    this.taskService.deleteTask(taskId).subscribe({
      next: () => {
        this.tasks.set(this.tasks().filter((task) => task.id !== taskId));
        this.totalElements.update((total) => total - 1);
      },
      error: (error) => {
        console.error('Error deleting task:', error);
      },
    });
  }

  createTask(): void {
    const projectId = this.route.snapshot.paramMap.get('projectId') || this.tasks()[0]?.projectId;
    if (!projectId) {
      console.error('Cannot create task: projectId is unavailable');
      return;
    }

    const dialogRef = this.dialog.open(TaskFormComponent, {
      width: '600px',
      data: { projectId },
    });

    dialogRef.afterClosed().subscribe((result) => {
      if (!result) return;
      this.taskService.createTask(result).subscribe({
        next: (response) => {
          if (response.status === 'success') this.loadTasks();
        },
        error: (error) => console.error('Error creating task:', error),
      });
    });
  }

  editTask(task: Task): void {
    const dialogRef = this.dialog.open(TaskFormComponent, {
      width: '600px',
      data: { task, projectId: task.projectId },
    });

    dialogRef.afterClosed().subscribe((result) => {
      if (!result) return;
      this.taskService.updateTask(task.id, result).subscribe({
        next: (response) => {
          if (response.status === 'success') this.loadTasks();
        },
        error: (error) => console.error('Error updating task:', error),
      });
    });
  }

  viewTaskDetails(task: Task): void {
    const dialogRef = this.dialog.open(TaskDetailComponent, {
      width: '900px',
      maxHeight: '90vh',
      data: { taskId: task.id, currentUserId: task.assigneeId || '' },
    });

    dialogRef.componentRef?.setInput('taskId', task.id);
    dialogRef.componentRef?.setInput('currentUserId', task.assigneeId || '');
    dialogRef.afterClosed().subscribe(() => this.loadTasks());
  }

  goToPage(page: number): void {
    if (page >= 0 && page < this.totalPages()) {
      this.currentPage.set(page);
      this.loadTasks();
    }
  }

  onPageInputChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    const pageNumber = parseInt(input.value, 10);

    if (!isNaN(pageNumber) && pageNumber >= 1 && pageNumber <= this.totalPages()) {
      this.currentPage.set(pageNumber - 1);
      this.loadTasks();
    } else {
      input.value = (this.currentPage() + 1).toString();
    }
  }

  onPageSizeChange(size: number): void {
    this.pageSize.set(size);
    this.currentPage.set(0);
    this.loadTasks();
  }
}
