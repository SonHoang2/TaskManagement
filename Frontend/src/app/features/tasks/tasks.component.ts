import { Component, signal, computed, inject } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTableModule } from '@angular/material/table';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatMenuModule } from '@angular/material/menu';
import { MatPaginatorModule } from '@angular/material/paginator';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDividerModule } from '@angular/material/divider';
import { FormsModule } from '@angular/forms';

interface Task {
  id: number;
  title: string;
  description: string;
  status: 'todo' | 'in-progress' | 'completed';
  priority: 'low' | 'medium' | 'high';
  dueDate: string;
  assignee: string;
}

@Component({
  selector: 'app-tasks',
  standalone: true,
  imports: [
    MatCardModule,
    MatIconModule,
    MatButtonModule,
    MatTableModule,
    MatCheckboxModule,
    MatMenuModule,
    MatPaginatorModule,
    MatInputModule,
    MatSelectModule,
    MatFormFieldModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    MatDividerModule,
    FormsModule,
  ],
  templateUrl: './tasks.component.html',
  styleUrl: './tasks.component.scss',
})
export class TasksComponent {
  readonly isLoading = signal(false);
  readonly searchTerm = signal('');
  readonly statusFilter = signal('all');
  readonly priorityFilter = signal('all');

  readonly displayedColumns: string[] = [
    'select',
    'title',
    'status',
    'priority',
    'dueDate',
    'assignee',
    'actions',
  ];
  readonly tasks = signal<Task[]>([
    {
      id: 1,
      title: 'Design system review',
      description: 'Review and update the design system components',
      status: 'in-progress',
      priority: 'high',
      dueDate: '2024-08-20',
      assignee: 'John Doe',
    },
    {
      id: 2,
      title: 'API integration',
      description: 'Integrate the backend API with the frontend',
      status: 'todo',
      priority: 'high',
      dueDate: '2024-08-22',
      assignee: 'Jane Smith',
    },
    {
      id: 3,
      title: 'Unit tests',
      description: 'Write unit tests for core components',
      status: 'todo',
      priority: 'medium',
      dueDate: '2024-08-25',
      assignee: 'Bob Johnson',
    },
    {
      id: 4,
      title: 'Documentation',
      description: 'Update project documentation',
      status: 'completed',
      priority: 'low',
      dueDate: '2024-08-18',
      assignee: 'Alice Williams',
    },
    {
      id: 5,
      title: 'Performance optimization',
      description: 'Optimize application performance',
      status: 'in-progress',
      priority: 'medium',
      dueDate: '2024-08-28',
      assignee: 'Charlie Brown',
    },
  ]);

  readonly selectedTasks = signal<Set<number>>(new Set());

  readonly filteredTasks = computed(() => {
    return this.tasks().filter((task) => {
      const matchesSearch =
        task.title.toLowerCase().includes(this.searchTerm().toLowerCase()) ||
        task.description.toLowerCase().includes(this.searchTerm().toLowerCase());
      const matchesStatus = this.statusFilter() === 'all' || task.status === this.statusFilter();
      const matchesPriority =
        this.priorityFilter() === 'all' || task.priority === this.priorityFilter();
      return matchesSearch && matchesStatus && matchesPriority;
    });
  });

  readonly taskStats = computed(() => {
    const total = this.tasks().length;
    const completed = this.tasks().filter((t) => t.status === 'completed').length;
    const inProgress = this.tasks().filter((t) => t.status === 'in-progress').length;
    const todo = this.tasks().filter((t) => t.status === 'todo').length;
    return { total, completed, inProgress, todo };
  });

  getStatusColor(status: string): string {
    switch (status) {
      case 'completed':
        return 'primary';
      case 'in-progress':
        return 'accent';
      case 'todo':
        return 'warn';
      default:
        return '';
    }
  }

  getPriorityColor(priority: string): string {
    switch (priority) {
      case 'high':
        return 'warn';
      case 'medium':
        return 'accent';
      case 'low':
        return 'primary';
      default:
        return '';
    }
  }

  toggleTaskSelection(taskId: number) {
    const selected = new Set(this.selectedTasks());
    if (selected.has(taskId)) {
      selected.delete(taskId);
    } else {
      selected.add(taskId);
    }
    this.selectedTasks.set(selected);
  }

  isAllSelected() {
    return (
      this.filteredTasks().length > 0 && this.selectedTasks().size === this.filteredTasks().length
    );
  }

  toggleAllSelection() {
    if (this.isAllSelected()) {
      this.selectedTasks.set(new Set());
    } else {
      this.selectedTasks.set(new Set(this.filteredTasks().map((task) => task.id)));
    }
  }

  isTaskSelected(taskId: number): boolean {
    return this.selectedTasks().has(taskId);
  }

  onSearchChange(value: string) {
    this.searchTerm.set(value);
  }

  onStatusFilterChange(value: string) {
    this.statusFilter.set(value);
  }

  onPriorityFilterChange(value: string) {
    this.priorityFilter.set(value);
  }

  deleteTask(taskId: number) {
    const updatedTasks = this.tasks().filter((task) => task.id !== taskId);
    this.tasks.set(updatedTasks);
  }
}
