import { Component, signal, inject } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatGridListModule } from '@angular/material/grid-list';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

interface StatCard {
  title: string;
  value: string;
  icon: string;
  color: string;
  trend: string;
}

interface RecentActivity {
  id: number;
  title: string;
  description: string;
  time: string;
  type: 'task' | 'project' | 'team';
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    MatCardModule,
    MatIconModule,
    MatButtonModule,
    MatGridListModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent {
  readonly isLoading = signal(false);

  readonly statCards: StatCard[] = [
    {
      title: 'Total Tasks',
      value: '24',
      icon: 'task_alt',
      color: 'primary',
      trend: '+12% from last week'
    },
    {
      title: 'Projects',
      value: '8',
      icon: 'folder',
      color: 'accent',
      trend: '+2 new this month'
    },
    {
      title: 'Team Members',
      value: '12',
      icon: 'people',
      color: 'warn',
      trend: '+3 new members'
    },
    {
      title: 'Completed',
      value: '18',
      icon: 'check_circle',
      color: 'success',
      trend: '75% completion rate'
    }
  ];

  readonly recentActivities: RecentActivity[] = [
    {
      id: 1,
      title: 'Task Completed',
      description: 'Design review task has been completed',
      time: '2 hours ago',
      type: 'task'
    },
    {
      id: 2,
      title: 'New Project',
      description: 'Website redesign project has been created',
      time: '5 hours ago',
      type: 'project'
    },
    {
      id: 3,
      title: 'Team Update',
      description: 'John joined the marketing team',
      time: '1 day ago',
      type: 'team'
    },
    {
      id: 4,
      title: 'Task Assigned',
      description: 'You have been assigned to API integration',
      time: '2 days ago',
      type: 'task'
    }
  ];

  getActivityIcon(type: string): string {
    switch (type) {
      case 'task':
        return 'task_alt';
      case 'project':
        return 'folder';
      case 'team':
        return 'person_add';
      default:
        return 'info';
    }
  }

  getActivityColor(type: string): string {
    switch (type) {
      case 'task':
        return 'primary';
      case 'project':
        return 'accent';
      case 'team':
        return 'warn';
      default:
        return '';
    }
  }
}
