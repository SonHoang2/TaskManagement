import { ChangeDetectionStrategy, Component, inject, signal, input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ProjectService } from '../../services/project.service';
import {
  ProjectMember,
  AddMemberRequest,
  UpdateMemberRoleRequest,
} from '../../models/project.model';

@Component({
  selector: 'app-project-members',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './project-members.component.html',
  styleUrls: ['./project-members.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProjectMembersComponent implements OnInit {
  private readonly projectService = inject(ProjectService);
  private readonly fb = inject(FormBuilder);

  readonly projectId = input.required<string>();

  protected readonly members = signal<ProjectMember[]>([]);
  protected readonly isLoading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly showAddForm = signal(false);
  protected readonly isSubmitting = signal(false);

  protected readonly addMemberForm: FormGroup = this.fb.group({
    userId: ['', [Validators.required]],
    role: ['MEMBER', [Validators.required]],
  });

  protected readonly roleOptions = [
    { value: 'ADMIN', label: 'Admin' },
    { value: 'MEMBER', label: 'Member' },
  ];

  ngOnInit(): void {
    this.loadMembers();
  }

  protected loadMembers(): void {
    this.isLoading.set(true);
    this.error.set(null);

    this.projectService.getProjectMembers(this.projectId()).subscribe({
      next: (response) => {
        if (response.status === 'success' && response.data) {
          const members = Array.isArray(response.data) ? response.data : response.data.content;
          this.members.set(members);
        } else {
          this.error.set(response.message || 'Failed to load members');
        }
        this.isLoading.set(false);
      },
      error: (err) => {
        this.error.set('An error occurred while loading members');
        this.isLoading.set(false);
      },
    });
  }

  protected toggleAddForm(): void {
    this.showAddForm.update((show) => !show);
    if (!this.showAddForm()) {
      this.addMemberForm.reset();
    }
  }

  protected onAddMember(): void {
    if (this.addMemberForm.invalid) {
      this.markFormGroupTouched(this.addMemberForm);
      return;
    }

    this.isSubmitting.set(true);
    this.error.set(null);

    const data: AddMemberRequest = {
      userId: this.addMemberForm.value.userId,
      role: this.addMemberForm.value.role,
    };

    this.projectService.addMember(this.projectId(), data).subscribe({
      next: (response) => {
        if (response.status === 'success') {
          this.addMemberForm.reset();
          this.showAddForm.set(false);
          this.loadMembers();
        } else {
          this.error.set(response.message || 'Failed to add member');
        }
        this.isSubmitting.set(false);
      },
      error: (err) => {
        this.error.set('An error occurred while adding member');
        this.isSubmitting.set(false);
      },
    });
  }

  protected onUpdateRole(member: ProjectMember, newRole: 'ADMIN' | 'MEMBER'): void {
    if (member.role === newRole) return;

    this.isSubmitting.set(true);
    this.error.set(null);

    const data: UpdateMemberRoleRequest = {
      role: newRole,
    };

    this.projectService.updateMemberRole(this.projectId(), member.id, data).subscribe({
      next: (response) => {
        if (response.status === 'success') {
          this.loadMembers();
        } else {
          this.error.set(response.message || 'Failed to update member role');
        }
        this.isSubmitting.set(false);
      },
      error: (err) => {
        this.error.set('An error occurred while updating member role');
        this.isSubmitting.set(false);
      },
    });
  }

  protected onRemoveMember(member: ProjectMember): void {
    if (
      !confirm(
        `Are you sure you want to remove ${member.user?.name || member.userId} from this project?`,
      )
    ) {
      return;
    }

    this.isSubmitting.set(true);
    this.error.set(null);

    this.projectService.removeMember(this.projectId(), member.id).subscribe({
      next: (response) => {
        if (response.status === 'success') {
          this.loadMembers();
        } else {
          this.error.set(response.message || 'Failed to remove member');
        }
        this.isSubmitting.set(false);
      },
      error: (err) => {
        this.error.set('An error occurred while removing member');
        this.isSubmitting.set(false);
      },
    });
  }

  protected getRoleBadgeClass(role: string): string {
    switch (role) {
      case 'OWNER':
        return 'badge-owner';
      case 'ADMIN':
        return 'badge-admin';
      case 'MEMBER':
        return 'badge-member';
      default:
        return 'badge-default';
    }
  }

  protected getRoleLabel(role: string): string {
    switch (role) {
      case 'OWNER':
        return 'Owner';
      case 'ADMIN':
        return 'Admin';
      case 'MEMBER':
        return 'Member';
      default:
        return role;
    }
  }

  protected formatDate(dateString: string): string {
    return new Date(dateString).toLocaleDateString();
  }

  protected markFormGroupTouched(formGroup: FormGroup): void {
    Object.keys(formGroup.controls).forEach((key) => {
      const control = formGroup.get(key);
      control?.markAsTouched();
    });
  }

  protected getErrorMessage(controlName: string): string {
    const control = this.addMemberForm.get(controlName);
    if (!control || !control.errors || !control.touched) return '';

    if (control.errors['required']) {
      return `${this.getFieldLabel(controlName)} is required`;
    }

    return 'Invalid input';
  }

  protected getFieldLabel(controlName: string): string {
    const labels: Record<string, string> = {
      userId: 'User ID',
      role: 'Role',
    };
    return labels[controlName] || controlName;
  }
}
