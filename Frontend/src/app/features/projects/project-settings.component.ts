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
  template: `
    <div class="page stack" style="padding-top: 24px; max-width: 760px; gap: 20px">
      @if (!ctx.canManage() && ctx.myRole()) {
        <div class="alert alert--info">
          <span class="i">info</span>Only the project owner or an admin can change these settings.
        </div>
      }

      <form class="card" [formGroup]="form" (ngSubmit)="save()">
        <header class="card__header"><h2>General</h2></header>
        <div class="card__body stack" style="gap: 16px">
          <div class="field">
            <label for="ps-name">Project name</label>
            <input id="ps-name" class="input" formControlName="name" maxlength="200" />
            @if (form.controls.name.touched && form.controls.name.invalid) {
              <span class="field__error">Name is required.</span>
            }
          </div>
          <div class="field">
            <label for="ps-desc">Description</label>
            <textarea
              id="ps-desc"
              class="textarea"
              formControlName="description"
              rows="4"
              maxlength="2000"
            ></textarea>
          </div>
        </div>
        <footer class="card__footer">
          <button
            class="btn btn--primary"
            type="submit"
            [disabled]="!ctx.canManage() || form.pristine || saving()"
          >
            @if (saving()) {
              <span class="spinner"></span>
            }
            Save changes
          </button>
        </footer>
      </form>

      <section class="card danger-zone">
        <header class="card__header"><h2>Danger zone</h2></header>
        <div class="card__body row" style="gap: 16px; flex-wrap: wrap">
          <div class="grow">
            <strong>Delete this project</strong>
            <p class="muted small">
              Removes the project and its memberships. This cannot be undone.
            </p>
          </div>
          <button
            class="btn btn--danger"
            type="button"
            (click)="remove()"
            [disabled]="!ctx.canManage()"
          >
            <span class="i">delete_forever</span>Delete project
          </button>
        </div>
      </section>
    </div>
  `,
  styles: `
    .danger-zone {
      border-color: color-mix(in srgb, var(--danger) 35%, var(--border));
      .card__header h2 {
        color: var(--danger);
      }
    }
  `,
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
