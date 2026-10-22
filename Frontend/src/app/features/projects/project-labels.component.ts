import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Label } from '../../core/models';
import { ProjectsService } from '../../core/services/projects.service';
import { ConfirmService } from '../../core/ui/confirm.service';
import { ToastService } from '../../core/ui/toast.service';
import { LABEL_COLORS } from '../../core/utils/format';
import { EmptyStateComponent } from '../../shared/components/empty-state.component';
import { ProjectContext } from './project-context';

@Component({
  selector: 'app-project-labels',
  imports: [FormsModule, EmptyStateComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './project-labels.component.html',
  styleUrl: './project-labels.component.scss',
})
export class ProjectLabelsComponent {
  protected readonly ctx = inject(ProjectContext);
  private readonly projects = inject(ProjectsService);
  private readonly confirm = inject(ConfirmService);
  private readonly toast = inject(ToastService);

  protected readonly colors = LABEL_COLORS;
  protected readonly name = signal('');
  protected readonly color = signal(LABEL_COLORS[0]);
  protected readonly editing = signal<Label | null>(null);
  protected readonly saving = signal(false);

  protected readonly sorted = computed(() =>
    [...this.ctx.labels()].sort((a, b) => a.name.localeCompare(b.name)),
  );
  protected readonly usage = computed(() => {
    const m = new Map<string, number>();
    for (const t of this.ctx.tasks())
      for (const l of t.labels ?? []) m.set(l.labelId, (m.get(l.labelId) ?? 0) + 1);
    return m;
  });

  protected edit(l: Label): void {
    this.editing.set(l);
    this.name.set(l.name);
    this.color.set(l.color || LABEL_COLORS[0]);
  }

  protected reset(): void {
    this.editing.set(null);
    this.name.set('');
    this.color.set(LABEL_COLORS[Math.floor(Math.random() * LABEL_COLORS.length)]);
  }

  protected save(): void {
    const name = this.name().trim();
    if (!name) return;
    if (
      this.ctx
        .labels()
        .some((l) => l.name.toLowerCase() === name.toLowerCase() && l.id !== this.editing()?.id)
    ) {
      this.toast.error('A label with that name already exists.');
      return;
    }
    this.saving.set(true);
    const editing = this.editing();
    const req = editing
      ? this.projects.updateLabel(editing.id, { name, color: this.color() })
      : this.projects.createLabel({ projectId: this.ctx.id(), name, color: this.color() });
    req.subscribe({
      next: (label) => {
        this.saving.set(false);
        this.ctx.labels.update((list) =>
          editing ? list.map((l) => (l.id === label.id ? label : l)) : [...list, label],
        );
        this.toast.success(editing ? 'Label updated' : 'Label created');
        this.reset();
      },
      error: (e) => {
        this.saving.set(false);
        this.toast.error(e);
      },
    });
  }

  protected remove(l: Label): void {
    this.confirm
      .ask({
        title: `Delete “${l.name}”?`,
        message: 'The label will be removed from every task in this project.',
        confirmText: 'Delete',
        danger: true,
      })
      .subscribe((ok) => {
        if (!ok) return;
        this.projects.deleteLabel(l.id).subscribe({
          next: () => {
            this.ctx.labels.update((list) => list.filter((x) => x.id !== l.id));
            if (this.editing()?.id === l.id) this.reset();
            this.toast.success('Label deleted');
          },
          error: (e) => this.toast.error(e),
        });
      });
  }
}
