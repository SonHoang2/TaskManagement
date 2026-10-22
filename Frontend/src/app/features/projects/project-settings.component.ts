import { ChangeDetectionStrategy, Component, effect, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { ProjectsService } from '../../core/services/projects.service';
import { WorkspaceService } from '../../core/services/workspace.service';
import { ConfirmService } from '../../core/ui/confirm.service';
import { ToastService } from '../../core/ui/toast.service';
import { ProjectContext } from './project-context';

@Component({
  selector: 'app-project-settings',
  imports: [ReactiveFormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './project-settings.component.html',
  styleUrl: './project-settings.component.scss',
})
export class ProjectSettingsComponent {
  protected readonly ctx = inject(ProjectContext);
  private readonly projects = inject(ProjectsService);
  private readonly workspace = inject(WorkspaceService);
  private readonly confirm = inject(ConfirmService);
  private readonly toast = inject(ToastService);
  private readonly router = inject(Router);

  protected readonly saving = signal(false);
  protected readonly form = inject(NonNullableFormBuilder).group({
    name: ['', [Validators.required, Validators.maxLength(200)]],
    description: [''],
  });

  constructor() {
    effect(() => {
      const p = this.ctx.project();
      if (p) this.form.reset({ name: p.name, description: p.description ?? '' });
    });
    effect(() => (this.ctx.canManage() ? this.form.enable() : this.form.disable()));
  }

  protected save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { name, description } = this.form.getRawValue();
    this.saving.set(true);
    this.projects
      .update(this.ctx.id(), { name: name.trim(), description: description.trim() || null })
      .subscribe({
        next: (p) => {
          this.saving.set(false);
          this.ctx.project.set(p);
          this.workspace.invalidate();
          this.workspace.load();
          this.toast.success('Project updated');
        },
        error: (e) => {
          this.saving.set(false);
          this.toast.error(e);
        },
      });
  }

  protected remove(): void {
    const p = this.ctx.project();
    if (!p) return;
    this.confirm
      .ask({
        title: `Delete “${p.name}”?`,
        message: 'All members will lose access to this project. This cannot be undone.',
        confirmText: 'Delete project',
        danger: true,
      })
      .subscribe((ok) => {
        if (!ok) return;
        this.projects.delete(p.id).subscribe({
          next: () => {
            this.toast.success('Project deleted');
            this.workspace.load(true);
            this.router.navigate(['/projects']);
          },
          error: (e) => this.toast.error(e),
        });
      });
  }
}
