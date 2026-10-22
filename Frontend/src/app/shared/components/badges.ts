import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { MemberRole, SprintStatus, TaskPriority, TaskStatus } from '../../core/models';
import {
  PRIORITY_META,
  ROLE_META,
  SPRINT_META,
  STATUS_META,
  dueLabel,
  isOverdue,
  daysUntil,
} from '../../core/utils/format';

@Component({
  selector: 'app-status',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { '[class]': '"pill pill--" + meta().tone' },
  template: `<span class="i">{{ meta().icon }}</span
    >{{ meta().label }}`,
})
export class StatusBadgeComponent {
  readonly value = input.required<TaskStatus>();
  protected readonly meta = computed(() => STATUS_META[this.value()]);
}

@Component({
  selector: 'app-priority',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    '[class]': '"prio prio--" + (meta()?.tone ?? "none")',
    '[attr.title]': '(meta()?.label ?? "No") + " priority"',
  },
  template: `<span class="i">{{ meta()?.icon ?? 'remove' }}</span>
    @if (!iconOnly()) {
      <span>{{ meta()?.label ?? 'None' }}</span>
    }`,
})
export class PriorityBadgeComponent {
  readonly value = input<TaskPriority | null | undefined>(null);
  readonly iconOnly = input(false);
  protected readonly meta = computed(() => {
    const v = this.value();
    return v ? PRIORITY_META[v] : null;
  });
}

@Component({
  selector: 'app-sprint-status',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { '[class]': '"pill pill--" + meta().tone' },
  template: `<span class="i">{{ meta().icon }}</span
    >{{ meta().label }}`,
})
export class SprintStatusBadgeComponent {
  readonly value = input.required<SprintStatus>();
  protected readonly meta = computed(() => SPRINT_META[this.value()] ?? SPRINT_META.PLANNED);
}

@Component({
  selector: 'app-role',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { '[class]': '"pill pill--" + meta().tone' },
  template: `<span class="i">{{ meta().icon }}</span
    >{{ meta().label }}`,
})
export class RoleBadgeComponent {
  readonly value = input.required<MemberRole>();
  protected readonly meta = computed(() => ROLE_META[this.value()]);
}

@Component({
  selector: 'app-due',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    class: 'due',
    '[class.due--overdue]': 'overdue()',
    '[class.due--soon]': 'soon()',
    '[class.due--done]': 'status() === "DONE"',
  },
  template: `<span class="i">{{ overdue() ? 'event_busy' : 'event' }}</span
    >{{ label() }}`,
})
export class DueDateComponent {
  readonly date = input.required<string>();
  readonly status = input<TaskStatus>('TODO');
  protected readonly label = computed(() => dueLabel(this.date()));
  protected readonly overdue = computed(() =>
    isOverdue({ dueDate: this.date(), status: this.status() }),
  );
  protected readonly soon = computed(() => {
    if (this.status() === 'DONE') return false;
    const n = daysUntil(this.date());
    return n >= 0 && n <= 2;
  });
}
