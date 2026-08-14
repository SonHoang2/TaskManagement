import { ChangeDetectionStrategy, Component, inject, signal, input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ProjectService } from '../../services/project.service';
import { ProjectInvitation, CreateInvitationRequest } from '../../models/project.model';

@Component({
  selector: 'app-project-invitations',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './project-invitations.component.html',
  styleUrls: ['./project-invitations.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProjectInvitationsComponent implements OnInit {
  private readonly projectService = inject(ProjectService);
  private readonly fb = inject(FormBuilder);

  readonly projectId = input.required<string>();

  protected readonly invitations = signal<ProjectInvitation[]>([]);
  protected readonly isLoading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly showAddForm = signal(false);
  protected readonly isSubmitting = signal(false);

  protected readonly inviteForm: FormGroup = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
  });

  ngOnInit(): void {
    this.loadInvitations();
  }

  protected loadInvitations(): void {
    this.isLoading.set(true);
    this.error.set(null);

    this.projectService.getProjectInvitations(this.projectId()).subscribe({
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
      },
    });
  }

  protected toggleAddForm(): void {
    this.showAddForm.update((show) => !show);
    if (!this.showAddForm()) {
      this.inviteForm.reset();
    }
  }

  protected onSendInvitation(): void {
    if (this.inviteForm.invalid) {
      this.markFormGroupTouched(this.inviteForm);
      return;
    }

    this.isSubmitting.set(true);
    this.error.set(null);

    const data: CreateInvitationRequest = {
      email: this.inviteForm.value.email,
    };

    this.projectService.createInvitation(this.projectId(), data).subscribe({
      next: (response) => {
        if (response.status === 'success') {
          this.inviteForm.reset();
          this.showAddForm.set(false);
          this.loadInvitations();
        } else {
          this.error.set(response.message || 'Failed to send invitation');
        }
        this.isSubmitting.set(false);
      },
      error: (err) => {
        this.error.set('An error occurred while sending invitation');
        this.isSubmitting.set(false);
      },
    });
  }

  protected onCancelInvitation(invitation: ProjectInvitation): void {
    if (!confirm(`Are you sure you want to cancel the invitation to ${invitation.invitedEmail}?`)) {
      return;
    }

    this.isSubmitting.set(true);
    this.error.set(null);

    this.projectService.cancelInvitation(this.projectId(), invitation.id).subscribe({
      next: (response) => {
        if (response.status === 'success') {
          this.loadInvitations();
        } else {
          this.error.set(response.message || 'Failed to cancel invitation');
        }
        this.isSubmitting.set(false);
      },
      error: (err) => {
        this.error.set('An error occurred while cancelling invitation');
        this.isSubmitting.set(false);
      },
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

  protected isInvitationCancellable(invitation: ProjectInvitation): boolean {
    return invitation.status === 'PENDING';
  }

  protected markFormGroupTouched(formGroup: FormGroup): void {
    Object.keys(formGroup.controls).forEach((key) => {
      const control = formGroup.get(key);
      control?.markAsTouched();
    });
  }

  protected getErrorMessage(controlName: string): string {
    const control = this.inviteForm.get(controlName);
    if (!control || !control.errors || !control.touched) return '';

    if (control.errors['required']) {
      return 'Email is required';
    }
    if (control.errors['email']) {
      return 'Please enter a valid email address';
    }

    return 'Invalid input';
  }
}
