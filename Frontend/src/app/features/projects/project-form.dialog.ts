import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Project } from '../../core/models';
import { ProjectsService } from '../../core/services/projects.service';
import { WorkspaceService } from '../../core/services/workspace.service';
import { ToastService } from '../../core/ui/toast.service';

@Component({
  selector: 'app-project-form',
  imports: [ReactiveFormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './project-form.dialog.html',
})
export class ProjectFormDialog {
  protected readonly data = inject<{ project?: Project }>(MAT_DIALOG_DATA);
  protected readonly ref = inject(MatDialogRef<ProjectFormDialog, Project | undefined>);
  private readonly projects = inject(ProjectsService);
  private readonly workspace = inject(WorkspaceService);
  private readonly toast = inject(ToastService);

  protected readonly saving = signal(false);
  protected readonly form = inject(NonNullableFormBuilder).group({
    name: [this.data.project?.name ?? '', [Validators.required, Validators.maxLength(200)]],
    description: [this.data.project?.description ?? ''],
  });

  protected save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    const body = { name: v.name.trim(), description: v.description.trim() || null };
    this.saving.set(true);
    const req = this.data.project
      ? this.projects.update(this.data.project.id, body)
      : this.projects.create(body);
    req.subscribe({
      next: (p) => {
        this.workspace.load(true);
        this.toast.success(this.data.project ? 'Project updated' : 'Project created');
        this.ref.close(p);
      },
      error: (e) => {
        this.saving.set(false);
        this.toast.error(e);
      },
    });
  }
}
