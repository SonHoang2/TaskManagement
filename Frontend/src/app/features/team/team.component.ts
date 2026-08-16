import { Component, signal, computed } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatMenuModule } from '@angular/material/menu';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTabsModule } from '@angular/material/tabs';
import { MatDividerModule } from '@angular/material/divider';
import { FormsModule } from '@angular/forms';

interface TeamMember {
  id: number;
  name: string;
  email: string;
  role: string;
  department: string;
  avatar: string;
  status: 'active' | 'inactive' | 'pending';
  tasksAssigned: number;
  projects: string[];
  joinedDate: string;
}

@Component({
  selector: 'app-team',
  standalone: true,
  imports: [
    MatCardModule,
    MatIconModule,
    MatButtonModule,
    MatChipsModule,
    MatMenuModule,
    MatInputModule,
    MatFormFieldModule,
    MatProgressSpinnerModule,
    MatTabsModule,
    MatDividerModule,
    FormsModule,
  ],
  templateUrl: './team.component.html',
  styleUrl: './team.component.scss',
})
export class TeamComponent {
  readonly isLoading = signal(false);
  readonly searchTerm = signal('');
  readonly selectedTab = signal(0);

  readonly teamMembers = signal<TeamMember[]>([
    {
      id: 1,
      name: 'John Doe',
      email: 'john.doe@example.com',
      role: 'Senior Developer',
      department: 'Engineering',
      avatar: 'JD',
      status: 'active',
      tasksAssigned: 8,
      projects: ['Website Redesign', 'API Integration'],
      joinedDate: '2023-01-15',
    },
    {
      id: 2,
      name: 'Jane Smith',
      email: 'jane.smith@example.com',
      role: 'Product Manager',
      department: 'Product',
      avatar: 'JS',
      status: 'active',
      tasksAssigned: 12,
      projects: ['Mobile App Development', 'Marketing Campaign'],
      joinedDate: '2023-03-20',
    },
    {
      id: 3,
      name: 'Bob Johnson',
      email: 'bob.johnson@example.com',
      role: 'UI/UX Designer',
      department: 'Design',
      avatar: 'BJ',
      status: 'active',
      tasksAssigned: 6,
      projects: ['Website Redesign'],
      joinedDate: '2023-05-10',
    },
    {
      id: 4,
      name: 'Alice Williams',
      email: 'alice.williams@example.com',
      role: 'QA Engineer',
      department: 'Quality Assurance',
      avatar: 'AW',
      status: 'active',
      tasksAssigned: 4,
      projects: ['API Integration'],
      joinedDate: '2023-06-01',
    },
    {
      id: 5,
      name: 'Charlie Brown',
      email: 'charlie.brown@example.com',
      role: 'DevOps Engineer',
      department: 'Operations',
      avatar: 'CB',
      status: 'pending',
      tasksAssigned: 0,
      projects: [],
      joinedDate: '2024-08-01',
    },
  ]);

  readonly filteredMembers = computed(() => {
    return this.teamMembers().filter((member) => {
      const matchesSearch =
        member.name.toLowerCase().includes(this.searchTerm().toLowerCase()) ||
        member.email.toLowerCase().includes(this.searchTerm().toLowerCase()) ||
        member.role.toLowerCase().includes(this.searchTerm().toLowerCase());
      return matchesSearch;
    });
  });

  readonly activeMembers = computed(() => this.teamMembers().filter((m) => m.status === 'active'));
  readonly pendingMembers = computed(() =>
    this.teamMembers().filter((m) => m.status === 'pending'),
  );

  getStatusColor(status: string): string {
    switch (status) {
      case 'active':
        return 'primary';
      case 'pending':
        return 'warn';
      case 'inactive':
        return '';
      default:
        return '';
    }
  }

  getAvatarColor(name: string): string {
    const colors = ['#6366f1', '#8b5cf6', '#ec4899', '#10b981', '#f59e0b', '#ef4444'];
    const index = name.charCodeAt(0) % colors.length;
    return colors[index];
  }

  onSearchChange(value: string) {
    this.searchTerm.set(value);
  }

  onTabChange(index: number) {
    this.selectedTab.set(index);
  }

  inviteMember() {
    // Implement invite functionality
  }

  removeMember(memberId: number) {
    const updatedMembers = this.teamMembers().filter((member) => member.id !== memberId);
    this.teamMembers.set(updatedMembers);
  }

  getTeamStats() {
    const total = this.teamMembers().length;
    const active = this.teamMembers().filter((m) => m.status === 'active').length;
    const pending = this.teamMembers().filter((m) => m.status === 'pending').length;
    return { total, active, pending };
  }
}
