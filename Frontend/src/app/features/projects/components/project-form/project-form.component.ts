import { ChangeDetectionStrategy, Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, ActivatedRoute } from '@angular/router';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ProjectService } from '../../services/project.service';
import {
  Project,
  CreateProjectRequest,
  CreateProjectResponse,
  GetProjectResponse,
  UpdateProjectRequest,
} from '../../models/project.model';

@Component({
  selector: 'app-project-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './project-form.component.html',
  styleUrls: ['./project-form.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProjectFormComponent implements OnInit {
  private readonly projectService = inject(ProjectService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly fb = inject(FormBuilder);

  protected readonly projectForm: FormGroup = this.fb.group({
    name: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(100)]],
    description: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(500)]],
  });

  protected readonly isEditMode = signal(false);
  protected readonly projectId = signal<string | null>(null);
  protected readonly isLoading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly isSubmitting = signal(false);

  ngOnInit(): void {
    const projectId = this.route.snapshot.paramMap.get('id');
    if (projectId && projectId !== 'new') {
      this.isEditMode.set(true);
      this.projectId.set(projectId);
      this.loadProject(projectId);
    }
  }

  protected loadProject(projectId: string): void {
    this.isLoading.set(true);
    this.error.set(null);

    this.projectService.getProject(projectId).subscribe({
      next: (response) => {
        if (response.status === 'success' && response.data) {
          const projectData = response.data.project || response.data;
          this.projectForm.patchValue({
            name: projectData.name,
            description: projectData.description,
          });
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

  protected onSubmit(): void {
    if (this.projectForm.invalid) {
      this.markFormGroupTouched(this.projectForm);
      return;
    }

    this.isSubmitting.set(true);
    this.error.set(null);

    const formData: CreateProjectRequest | UpdateProjectRequest = {
      name: this.projectForm.value.name,
      description: this.projectForm.value.description,
    };

    if (this.isEditMode()) {
      this.updateProject(formData as UpdateProjectRequest);
    } else {
      this.createProject(formData as CreateProjectRequest);
    }
  }

  protected createProject(data: CreateProjectRequest): void {
    this.projectService.createProject(data).subscribe({
      next: (response) => {
        if (response.status === 'success' && response.data) {
          // Navigate to the projects list or project detail if ID is available
          if (response.data.project?.id) {
            this.router.navigate(['/projects', response.data.project.id]);
          } else {
            this.router.navigate(['/projects']);
          }
        } else {
          this.error.set(response.message || 'Failed to create project');
          this.isSubmitting.set(false);
        }
      },
      error: (err) => {
        this.error.set('An error occurred while creating project');
        this.isSubmitting.set(false);
      },
    });
  }

  protected updateProject(data: UpdateProjectRequest): void {
    const projectId = this.projectId();
    if (!projectId) return;

    this.projectService.updateProject(projectId, data).subscribe({
      next: (response) => {
        if (response.status === 'success' && response.data) {
          this.router.navigate(['/projects', projectId]);
        } else {
          this.error.set(response.message || 'Failed to update project');
          this.isSubmitting.set(false);
        }
      },
      error: (err) => {
        this.error.set('An error occurred while updating project');
        this.isSubmitting.set(false);
      },
    });
  }

  protected cancel(): void {
    if (this.isEditMode()) {
      const projectId = this.projectId();
      if (projectId) {
        this.router.navigate(['/projects', projectId]);
      } else {
        this.router.navigate(['/projects']);
      }
    } else {
      this.router.navigate(['/projects']);
    }
  }

  protected markFormGroupTouched(formGroup: FormGroup): void {
    Object.keys(formGroup.controls).forEach((key) => {
      const control = formGroup.get(key);
      control?.markAsTouched();
    });
  }

  protected getErrorMessage(controlName: string): string {
    const control = this.projectForm.get(controlName);
    if (!control || !control.errors || !control.touched) return '';

    if (control.errors['required']) {
      return `${this.getFieldLabel(controlName)} is required`;
    }
    if (control.errors['minlength']) {
      return `${this.getFieldLabel(controlName)} must be at least ${control.errors['minlength'].requiredLength} characters`;
    }
    if (control.errors['maxlength']) {
      return `${this.getFieldLabel(controlName)} must not exceed ${control.errors['maxlength'].requiredLength} characters`;
    }

    return 'Invalid input';
  }

  protected getFieldLabel(controlName: string): string {
    const labels: Record<string, string> = {
      name: 'Project name',
      description: 'Description',
    };
    return labels[controlName] || controlName;
  }

  protected getTitle(): string {
    return this.isEditMode() ? 'Edit Project' : 'Create New Project';
  }
}
