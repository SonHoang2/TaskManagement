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
  template: `
    <form class="dialog" [formGroup]="form" (ngSubmit)="save()">
      <div class="dialog__header">
        <div class="grow">
          <h2 class="dialog__title">{{ data.project ? 'Edit project' : 'Create a project' }}</h2>
          <p class="muted small">Projects hold your team's tasks, sprints and labels.</p>
        </div>
        <button
          class="btn btn--ghost btn--icon btn--sm"
          type="button"
          (click)="ref.close()"
          aria-label="Close"
        >
          <span class="i">close</span>
        </button>
      </div>
      <div class="dialog__body stack" style="gap: 16px">
        <div class="field">
          <label for="pf-name">Name</label>
          <input
            id="pf-name"
            class="input"
            formControlName="name"
            placeholder="e.g. Website redesign"
            maxlength="200"
            cdkFocusInitial
          />
          @if (form.controls.name.touched && form.controls.name.invalid) {
            <span class="field__error">Give your project a name.</span>
          }
        </div>
        <div class="field">
          <label for="pf-desc">Description <span class="subtle">(optional)</span></label>
          <textarea
            id="pf-desc"
            class="textarea"
            formControlName="description"
            rows="3"
            maxlength="2000"
            placeholder="What is this project about?"
          ></textarea>
        </div>
      </div>
      <div class="dialog__footer">
        <button class="btn btn--ghost" type="button" (click)="ref.close()">Cancel</button>
        <button class="btn btn--primary" type="submit" [disabled]="saving()">
          @if (saving()) {
            <span class="spinner"></span>
          }
          {{ data.project ? 'Save' : 'Create project' }}
        </button>
      </div>
    </form>
  `,
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
