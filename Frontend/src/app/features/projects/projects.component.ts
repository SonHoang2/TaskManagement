import { Component, signal, computed } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatMenuModule } from '@angular/material/menu';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatDividerModule } from '@angular/material/divider';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { FormsModule } from '@angular/forms';

interface Project {
  id: number;
  name: string;
  description: string;
  status: 'active' | 'completed' | 'on-hold';
  progress: number;
  team: string[];
  dueDate: string;
  color: string;
}

@Component({
  selector: 'app-projects',
  standalone: true,
  imports: [
    MatCardModule,
    MatIconModule,
    MatButtonModule,
    MatChipsModule,
    MatMenuModule,
    MatInputModule,
    MatFormFieldModule,
    MatSelectModule,
    MatDividerModule,
    MatProgressSpinnerModule,
    FormsModule
  ],
  templateUrl: './projects.component.html',
  styleUrl: './projects.component.scss'
})
export class ProjectsComponent {
  readonly isLoading = signal(false);
  readonly searchTerm = signal('');
  readonly statusFilter = signal('all');

  readonly projects = signal<Project[]>([
    {
      id: 1,
      name: 'Website Redesign',
      description: 'Complete overhaul of the company website with modern design',
      status: 'active',
      progress: 65,
      team: ['John Doe', 'Jane Smith', 'Bob Johnson'],
      dueDate: '2024-09-15',
      color: '#6366f1'
    },
    {
      id: 2,
      name: 'Mobile App Development',
      description: 'Native mobile application for iOS and Android platforms',
      status: 'active',
      progress: 40,
      team: ['Alice Williams', 'Charlie Brown'],
      dueDate: '2024-10-20',
      color: '#8b5cf6'
    },
    {
      id: 3,
      name: 'Marketing Campaign',
      description: 'Q4 marketing campaign and social media strategy',
      status: 'active',
      progress: 25,
      team: ['Diana Prince', 'Eve Adams'],
      dueDate: '2024-12-01',
      color: '#ec4899'
    },
    {
      id: 4,
      name: 'API Integration',
      description: 'Integration with third-party APIs and services',
      status: 'completed',
      progress: 100,
      team: ['Frank Miller', 'Grace Lee'],
      dueDate: '2024-08-10',
      color: '#10b981'
    },
    {
      id: 5,
      name: 'Database Migration',
      description: 'Migration to new database infrastructure',
      status: 'on-hold',
      progress: 15,
      team: ['Henry Wilson'],
      dueDate: '2024-11-30',
      color: '#f59e0b'
    }
  ]);

  readonly filteredProjects = computed(() => {
    return this.projects().filter(project => {
      const matchesSearch = project.name.toLowerCase().includes(this.searchTerm().toLowerCase()) ||
                           project.description.toLowerCase().includes(this.searchTerm().toLowerCase());
      const matchesStatus = this.statusFilter() === 'all' || project.status === this.statusFilter();
      return matchesSearch && matchesStatus;
    });
  });

  getStatusColor(status: string): string {
    switch (status) {
      case 'active':
        return 'primary';
      case 'completed':
        return 'accent';
      case 'on-hold':
        return 'warn';
      default:
        return '';
    }
  }

  getProgressColor(progress: number): string {
    if (progress >= 75) return '#10b981';
    if (progress >= 50) return '#6366f1';
    if (progress >= 25) return '#f59e0b';
    return '#ef4444';
  }

  onSearchChange(value: string) {
    this.searchTerm.set(value);
  }

  onStatusFilterChange(value: string) {
    this.statusFilter.set(value);
  }

  deleteProject(projectId: number) {
    const updatedProjects = this.projects().filter(project => project.id !== projectId);
    this.projects.set(updatedProjects);
  }

  getProjectStats() {
    const total = this.projects().length;
    const active = this.projects().filter(p => p.status === 'active').length;
    const completed = this.projects().filter(p => p.status === 'completed').length;
    const onHold = this.projects().filter(p => p.status === 'on-hold').length;
    return { total, active, completed, onHold };
  }
}
