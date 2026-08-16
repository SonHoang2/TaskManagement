import { ChangeDetectionStrategy, Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { ProjectService } from '../../services/project.service';
import { Project, GetProjectResponse } from '../../models/project.model';
import { ProjectMembersComponent } from '../project-members/project-members.component';
import { ProjectInvitationsComponent } from '../project-invitations/project-invitations.component';

@Component({
  selector: 'app-project-detail',
  standalone: true,
  imports: [CommonModule, ProjectMembersComponent, ProjectInvitationsComponent],
  templateUrl: './project-detail.component.html',
  styleUrls: ['./project-detail.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProjectDetailComponent implements OnInit {
  private readonly projectService = inject(ProjectService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  protected readonly project = signal<Project | null>(null);
  protected readonly isLoading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly showMembers = signal(false);
  protected readonly showInvitations = signal(false);

  ngOnInit(): void {
    const projectId = this.route.snapshot.paramMap.get('id');
    if (projectId) {
      this.loadProject(projectId);

      // Check if we should show members section
      const showMembers = this.route.snapshot.queryParamMap.get('showMembers');
      if (showMembers === 'true') {
        this.showMembers.set(true);
      }
    } else {
      this.error.set('Project ID not provided');
      this.isLoading.set(false);
    }
  }

  protected loadProject(projectId: string): void {
    this.isLoading.set(true);
    this.error.set(null);

    this.projectService.getProject(projectId).subscribe({
      next: (response) => {
        if (response.status === 'success' && response.data) {
          const projectData = response.data.project || response.data;
          this.project.set(projectData);
        } else {
          this.error.set(response.message || 'Failed to load project');
        }
        this.isLoading.set(false);
      },
      error: (err) => {
        this.error.set('An error occurred while loading project');
        this.isLoading.set(false);
      },
    });
  }

  protected editProject(): void {
    const projectId = this.project()?.id;
    if (projectId) {
      this.router.navigate(['/projects', projectId, 'edit']);
    }
  }

  protected deleteProject(): void {
    const projectId = this.project()?.id;
    if (
      projectId &&
      confirm('Are you sure you want to delete this project? This action cannot be undone.')
    ) {
      this.isLoading.set(true);
      this.projectService.deleteProject(projectId).subscribe({
        next: (response) => {
          if (response.status === 'success') {
            this.router.navigate(['/projects']);
          } else {
            this.error.set(response.message || 'Failed to delete project');
            this.isLoading.set(false);
          }
        },
        error: (err) => {
          this.error.set('An error occurred while deleting project');
          this.isLoading.set(false);
        },
      });
    }
  }

  protected toggleMembers(): void {
    this.showMembers.update((show) => !show);
  }

  protected toggleInvitations(): void {
    this.showInvitations.update((show) => !show);
  }

  protected navigateToTasks(): void {
    const projectId = this.project()?.id;
    if (projectId) {
      this.router.navigate(['/projects', projectId, 'tasks']);
    }
  }

  protected navigateToSprints(): void {
    const projectId = this.project()?.id;
    if (projectId) {
      this.router.navigate(['/projects', projectId, 'sprints']);
    }
  }

  protected formatDate(dateString: string): string {
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
    });
  }

  protected goBack(): void {
    this.router.navigate(['/projects']);
  }
}
