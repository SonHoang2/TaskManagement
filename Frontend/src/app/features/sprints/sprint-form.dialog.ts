import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Sprint, SprintStatus } from '../../core/models';
import { SprintsService } from '../../core/services/sprints.service';
import { ToastService } from '../../core/ui/toast.service';

function isoDay(d: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}

@Component({
  selector: 'app-sprint-form',
  imports: [ReactiveFormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <form class="dialog" [formGroup]="form" (ngSubmit)="save()">
      <div class="dialog__header">
        <div class="grow">
          <h2 class="dialog__title">{{ data.sprint ? 'Edit sprint' : 'New sprint' }}</h2>
          <p class="muted small">A time-boxed batch of work, usually 1–2 weeks.</p>
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
      <div class="dialog__body">
        <div class="form-grid">
          <div class="field full">
            <label for="sf-name">Name</label>
            <input
              id="sf-name"
              class="input"
              formControlName="name"
              placeholder="Sprint 1"
              cdkFocusInitial
            />
            @if (form.controls.name.touched && form.controls.name.invalid) {
              <span class="field__error">Name is required.</span>
            }
          </div>
          <div class="field">
            <label for="sf-start">Start date</label>
            <input id="sf-start" class="input" type="date" formControlName="startDate" />
          </div>
          <div class="field">
            <label for="sf-end">End date</label>
            <input id="sf-end" class="input" type="date" formControlName="endDate" />
          </div>
          <div class="field full">
            <label for="sf-status">Status</label>
            <select id="sf-status" class="select" formControlName="status">
              <option value="PLANNED">Planned</option>
              <option value="ACTIVE">Active</option>
              <option value="COMPLETED">Completed</option>
            </select>
          </div>
        </div>
        @if (dateError()) {
          <p class="field__error" style="margin-top: 12px">
            End date must be after the start date.
          </p>
        }
      </div>
      <div class="dialog__footer">
        <button class="btn btn--ghost" type="button" (click)="ref.close()">Cancel</button>
        <button class="btn btn--primary" type="submit" [disabled]="saving()">
          @if (saving()) {
            <span class="spinner"></span>
          }
          {{ data.sprint ? 'Save' : 'Create sprint' }}
        </button>
      </div>
    </form>
  `,
})
export class SprintFormDialog {
  protected readonly data = inject<{ projectId: string; sprint?: Sprint; suggestedName?: string }>(
    MAT_DIALOG_DATA,
  );
  protected readonly ref = inject(MatDialogRef<SprintFormDialog, Sprint | undefined>);
  private readonly sprints = inject(SprintsService);
  private readonly toast = inject(ToastService);

  protected readonly saving = signal(false);
  protected readonly dateError = signal(false);

  private readonly today = new Date();
  protected readonly form = inject(NonNullableFormBuilder).group({
    name: [this.data.sprint?.name ?? this.data.suggestedName ?? '', Validators.required],
    startDate: [this.data.sprint?.startDate ?? isoDay(this.today)],
    endDate: [
      this.data.sprint?.endDate ?? isoDay(new Date(this.today.getTime() + 13 * 86_400_000)),
    ],
    status: [(this.data.sprint?.status ?? 'PLANNED') as SprintStatus],
  });

  protected save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    if (v.startDate && v.endDate && v.endDate < v.startDate) {
      this.dateError.set(true);
      return;
    }
    const body = {
      name: v.name.trim(),
      startDate: v.startDate || null,
      endDate: v.endDate || null,
      status: v.status,
    };
    this.saving.set(true);
    const req = this.data.sprint
      ? this.sprints.update(this.data.sprint.id, body)
      : this.sprints.create({ ...body, projectId: this.data.projectId });
    req.subscribe({
      next: (s) => {
        this.toast.success(this.data.sprint ? 'Sprint updated' : 'Sprint created');
        this.ref.close(s);
      },
      error: (e) => {
        this.saving.set(false);
        this.toast.error(e);
      },
    });
  }
}
