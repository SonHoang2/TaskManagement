import { ChangeDetectionStrategy, Component, inject, input, OnInit, signal, computed } from '@angular/core';
import { take } from 'rxjs/operators';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { CdkDragDrop, DragDropModule, moveItemInArray, transferArrayItem } from '@angular/cdk/drag-drop';
import { ActivatedRoute } from '@angular/router';
import type { Task } from '../../models/task.model';
import { TaskService } from '../../services/task.service';
import { TaskCardComponent } from '../task-card';
import { TaskFormComponent } from '../task-form';
import { TaskDetailComponent } from '../task-detail';

@Component({
  selector: 'app-kanban-board',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatDialogModule,
    DragDropModule,
    TaskCardComponent,
  ],
  templateUrl: './kanban-board.component.html',
  styleUrl: './kanban-board.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class KanbanBoardComponent implements OnInit {
  private readonly taskService = inject(TaskService);
  private readonly dialog = inject(MatDialog);
  private readonly route = inject(ActivatedRoute);

  readonly projectId = input<string>('');
  readonly currentUserId = input<string>('');
  readonly availableUsers = input<{ id: string; name: string; email: string }[]>([]);

  protected readonly routeProjectId = signal<string>('');

  protected readonly isLoading = signal(false);
  protected readonly allTasks = signal<Task[]>([]);

  protected readonly todoTasks = signal<Task[]>([]);
  protected readonly inProgressTasks = signal<Task[]>([]);
  protected readonly doneTasks = signal<Task[]>([]);

  protected readonly columns = computed(() => [
    { id: 'TODO', title: 'To Do', tasks: this.todoTasks(), color: '#f44336' },
    { id: 'IN_PROGRESS', title: 'In Progress', tasks: this.inProgressTasks(), color: '#00bcd4' },
    { id: 'DONE', title: 'Done', tasks: this.doneTasks(), color: '#4caf50' },
  ]);

  ngOnInit(): void {
    // Get projectId from route if not provided via input
    this.route.paramMap.subscribe(params => {
      const routeProjectId = params.get('projectId');
      if (routeProjectId) {
        this.routeProjectId.set(routeProjectId);
      }
    });
    
    this.loadTasks();
  }

  protected onDrop(event: CdkDragDrop<Task[]>): void {
    const previousIndex = event.previousIndex;
    const currentIndex = event.currentIndex;
    const previousContainer = event.previousContainer.data;
    const currentContainer = event.container.data;

    if (previousContainer === currentContainer) {
      moveItemInArray(previousContainer, previousIndex, currentIndex);
    } else {
      transferArrayItem(previousContainer, currentContainer, previousIndex, currentIndex);
      const movedTask = currentContainer[currentIndex];
      this.updateTaskStatus(movedTask.id, this.getColumnStatusFromId(event.container.id));
    }

    // Sync allTasks with column arrays
    this.syncAllTasks();
  }

  private syncAllTasks(): void {
    this.allTasks.set([
      ...this.todoTasks(),
      ...this.inProgressTasks(),
      ...this.doneTasks(),
    ]);
  }

  private getColumnStatusFromId(columnId: string): 'TODO' | 'IN_PROGRESS' | 'DONE' {
    switch (columnId) {
      case 'TODO':
        return 'TODO';
      case 'IN_PROGRESS':
        return 'IN_PROGRESS';
      case 'DONE':
        return 'DONE';
      default:
        return 'TODO';
    }
  }

  protected loadTasks(): void {
    const effectiveProjectId = this.projectId() || this.routeProjectId();
    if (!effectiveProjectId) {
      console.error('No projectId provided');
      return;
    }

    this.isLoading.set(true);
    this.taskService.getTasks({ page: 0, size: 100 }, { projectId: effectiveProjectId }).subscribe({
      next: (response) => {
        if (response.status === 'success' && response.data?.page) {
          const tasks = response.data.page.content;
          this.allTasks.set(tasks);
          this.todoTasks.set(tasks.filter(task => task.status === 'TODO'));
          this.inProgressTasks.set(tasks.filter(task => task.status === 'IN_PROGRESS'));
          this.doneTasks.set(tasks.filter(task => task.status === 'DONE'));
        }
        this.isLoading.set(false);
      },
      error: (error) => {
        console.error('Error loading tasks:', error);
        this.isLoading.set(false);
      },
    });
  }

  protected updateTaskStatus(taskId: string, newStatus: 'TODO' | 'IN_PROGRESS' | 'DONE'): void {
    this.taskService.updateTask(taskId, { status: newStatus }).subscribe({
      next: (response) => {
        if (response.status === 'success' && response.data?.task) {
          this.allTasks.update(tasks =>
            tasks.map(task => task.id === taskId ? response.data!.task : task)
          );
        }
      },
      error: (error) => {
        console.error('Error updating task status:', error);
        this.loadTasks(); // Reload to revert UI state
      },
    });
  }

  protected onCreateTask(): void {
    const effectiveProjectId = this.projectId() || this.routeProjectId();
    if (!effectiveProjectId) {
      console.error('No projectId provided');
      return;
    }

    import('../task-form').then(({ TaskFormComponent }) => {
      const dialogRef = this.dialog.open(TaskFormComponent, {
        width: '600px',
        data: {
          projectId: effectiveProjectId,
          availableUsers: this.availableUsers(),
        },
      });

      dialogRef.afterClosed().subscribe((result) => {
        if (result) {
          this.taskService.createTask(result).subscribe({
            next: (response) => {
              if (response.status === 'success' && response.data?.task) {
                this.allTasks.update(tasks => [...tasks, response.data!.task]);
              }
            },
            error: (error) => {
              console.error('Error creating task:', error);
            },
          });
        }
      });
    });
  }

  protected onEditTask(task: Task): void {
    const effectiveProjectId = this.projectId() || this.routeProjectId();
    if (!effectiveProjectId) {
      console.error('No projectId provided');
      return;
    }

    import('../task-form').then(({ TaskFormComponent }) => {
      const dialogRef = this.dialog.open(TaskFormComponent, {
        width: '600px',
        data: {
          task,
          projectId: effectiveProjectId,
          availableUsers: this.availableUsers(),
        },
      });

      dialogRef.afterClosed().subscribe((result) => {
        if (result) {
          this.taskService.updateTask(task.id, result).subscribe({
            next: (response) => {
              if (response.status === 'success' && response.data?.task) {
                this.allTasks.update(tasks =>
                  tasks.map(t => t.id === task.id ? response.data!.task : t)
                );
              }
            },
            error: (error) => {
              console.error('Error updating task:', error);
            },
          });
        }
      });
    });
  }

  protected onViewTaskDetails(task: Task): void {
    import('../task-detail').then(({ TaskDetailComponent }) => {
      const dialogRef = this.dialog.open(TaskDetailComponent, {
        width: '900px',
        maxHeight: '90vh',
        data: {
          taskId: task.id,
          currentUserId: this.currentUserId(),
          availableUsers: this.availableUsers(),
        },
      });

      dialogRef.afterClosed().subscribe(() => {
        this.loadTasks(); // Reload to reflect any changes
      });
    });
  }

  protected onDeleteTask(taskId: string): void {
    if (!confirm('Are you sure you want to delete this task?')) {
      return;
    }

    this.taskService.deleteTask(taskId).subscribe({
      next: () => {
        this.allTasks.update(tasks => tasks.filter(task => task.id !== taskId));
      },
      error: (error) => {
        console.error('Error deleting task:', error);
      },
    });
  }

  protected getColumnTaskCount(columnId: string): number {
    switch (columnId) {
      case 'TODO':
        return this.todoTasks().length;
      case 'IN_PROGRESS':
        return this.inProgressTasks().length;
      case 'DONE':
        return this.doneTasks().length;
      default:
        return 0;
    }
  }
}
