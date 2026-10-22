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
  template: `
    <div class="page" style="padding-top: 24px; max-width: 860px">
      <section class="card">
        <header class="card__header">
          <div class="grow">
            <h2>Labels</h2>
            <p class="subtle small">Group and filter tasks — e.g. bug, design, backend.</p>
          </div>
        </header>

        <form class="label-form" (ngSubmit)="save()">
          <span class="label-chip preview" [style.--c]="color()">{{
            name().trim() || 'Preview'
          }}</span>
          <input
            class="input input--sm grow"
            name="name"
            placeholder="Label name"
            maxlength="50"
            [ngModel]="name()"
            (ngModelChange)="name.set($event)"
          />
          <div class="swatches" role="radiogroup" aria-label="Color">
            @for (c of colors; track c) {
              <button
                type="button"
                class="swatch"
                [class.on]="c === color()"
                [style.background]="c"
                (click)="color.set(c)"
                [attr.aria-label]="c"
                role="radio"
                [attr.aria-checked]="c === color()"
              ></button>
            }
          </div>
          <button
            class="btn btn--primary btn--sm"
            type="submit"
            [disabled]="!name().trim() || saving()"
          >
            {{ editing() ? 'Save' : 'Add label' }}
          </button>
          @if (editing()) {
            <button class="btn btn--ghost btn--sm" type="button" (click)="reset()">Cancel</button>
          }
        </form>

        @if (sorted().length) {
          <ul class="labels">
            @for (l of sorted(); track l.id) {
              <li [class.editing]="editing()?.id === l.id">
                <span class="label-chip" [style.--c]="l.color || '#64748b'">{{ l.name }}</span>
                <span class="subtle small">{{ usage().get(l.id) ?? 0 }} tasks</span>
                <span class="spacer"></span>
                <button
                  class="btn btn--ghost btn--icon btn--sm"
                  type="button"
                  (click)="edit(l)"
                  aria-label="Edit label"
                >
                  <span class="i">edit</span>
                </button>
                <button
                  class="btn btn--danger-ghost btn--icon btn--sm"
                  type="button"
                  (click)="remove(l)"
                  aria-label="Delete label"
                >
                  <span class="i">delete</span>
                </button>
              </li>
            }
          </ul>
        } @else {
          <app-empty-state
            icon="sell"
            title="No labels yet"
            text="Create your first label above."
            [compact]="true"
          />
        }
      </section>
    </div>
  `,
  styles: `
    .label-form {
      display: flex;
      align-items: center;
      gap: 10px;
      flex-wrap: wrap;
      padding: 16px 20px;
      border-bottom: 1px solid var(--border);
      background: var(--surface-2);
    }
    .label-form .input {
      min-width: 180px;
    }
    .preview {
      min-width: 72px;
    }
    .swatches {
      display: flex;
      gap: 4px;
    }
    .swatch {
      width: 20px;
      height: 20px;
      border-radius: 50%;
      border: 2px solid var(--surface);
      box-shadow: 0 0 0 1px var(--border);
      cursor: pointer;
      padding: 0;
      &.on {
        box-shadow: 0 0 0 2px var(--text);
      }
    }
    .labels {
      list-style: none;
      margin: 0;
      padding: 4px 0;
      li {
        display: flex;
        align-items: center;
        gap: 12px;
        padding: 8px 12px 8px 20px;
      }
      li + li {
        border-top: 1px solid var(--border);
      }
      li.editing {
        background: var(--primary-soft);
      }
    }
  `,
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
