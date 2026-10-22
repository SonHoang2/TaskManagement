import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Sprint, SprintStatus } from '../../core/models';
import { SprintsService } from '../../core/services/sprints.service';
import { ToastService } from '../../core/ui/toast.service';
import { localDay } from '../../core/utils/format';

@Component({
  selector: 'app-sprint-form',
  imports: [ReactiveFormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './sprint-form.dialog.html',
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
    startDate: [this.data.sprint?.startDate ?? localDay(this.today)],
    endDate: [
      this.data.sprint?.endDate ?? localDay(new Date(this.today.getTime() + 13 * 86_400_000)),
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
