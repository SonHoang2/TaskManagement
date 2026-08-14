import { ChangeDetectionStrategy, Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { ProjectService } from '../../services/project.service';
import { ProjectInvitation, InvitationResponse } from '../../models/project.model';

@Component({
  selector: 'app-my-invitations',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './my-invitations.component.html',
  styleUrls: ['./my-invitations.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class MyInvitationsComponent implements OnInit {
  private readonly projectService = inject(ProjectService);
  private readonly router = inject(Router);

  protected readonly invitations = signal<ProjectInvitation[]>([]);
  protected readonly isLoading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly isSubmitting = signal(false);

  ngOnInit(): void {
    this.loadMyInvitations();
  }

  protected loadMyInvitations(): void {
    this.isLoading.set(true);
    this.error.set(null);

    this.projectService.getMyInvitations().subscribe({
      next: (response) => {
        if (response.status === 'success' && response.data) {
          const invitations = Array.isArray(response.data) ? response.data : response.data.content;
          this.invitations.set(invitations);
        } else {
          this.error.set(response.message || 'Failed to load invitations');
        }
        this.isLoading.set(false);
      },
      error: (err) => {
        this.error.set('An error occurred while loading invitations');
        this.isLoading.set(false);
      }
    });
  }

  protected acceptInvitation(invitation: ProjectInvitation): void {
    this.respondToInvitation(invitation, 'ACCEPTED');
  }

  protected rejectInvitation(invitation: ProjectInvitation): void {
    if (confirm('Are you sure you want to reject this invitation?')) {
      this.respondToInvitation(invitation, 'REJECTED');
    }
  }

  protected respondToInvitation(invitation: ProjectInvitation, response: 'ACCEPTED' | 'REJECTED'): void {
    this.isSubmitting.set(true);
    this.error.set(null);

    const data: InvitationResponse = {
      status: response
    };

    this.projectService.respondToInvitation(invitation.id, data).subscribe({
      next: (res) => {
        if (res.status === 'success') {
          if (response === 'ACCEPTED' && res.data?.project?.id) {
            this.router.navigate(['/projects', res.data.project.id]);
          } else {
            this.loadMyInvitations();
          }
        } else {
          this.error.set(res.message || `Failed to ${response.toLowerCase()} invitation`);
          this.isSubmitting.set(false);
        }
      },
      error: (err) => {
        this.error.set(`An error occurred while ${response.toLowerCase()}ing invitation`);
        this.isSubmitting.set(false);
      }
    });
  }

  protected getStatusBadgeClass(status: string): string {
    switch (status) {
      case 'PENDING':
        return 'badge-pending';
      case 'ACCEPTED':
        return 'badge-accepted';
      case 'REJECTED':
        return 'badge-rejected';
      case 'EXPIRED':
        return 'badge-expired';
      default:
        return 'badge-default';
    }
  }

  protected getStatusLabel(status: string): string {
    switch (status) {
      case 'PENDING':
        return 'Pending';
      case 'ACCEPTED':
        return 'Accepted';
      case 'REJECTED':
        return 'Rejected';
      case 'EXPIRED':
        return 'Expired';
      default:
        return status;
    }
  }

  protected formatDate(dateString: string): string {
    return new Date(dateString).toLocaleDateString();
  }

  protected formatDateTime(dateString: string): string {
    return new Date(dateString).toLocaleString();
  }

  protected isInvitationActionable(invitation: ProjectInvitation): boolean {
    return invitation.status === 'PENDING';
  }

  protected isInvitationExpired(invitation: ProjectInvitation): boolean {
    return invitation.status === 'EXPIRED' || new Date(invitation.expiresAt) < new Date();
  }

  protected navigateToProject(projectId: string): void {
    this.router.navigate(['/projects', projectId]);
  }
}
