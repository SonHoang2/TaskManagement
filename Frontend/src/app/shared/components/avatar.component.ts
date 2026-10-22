import { ChangeDetectionStrategy, Component, computed, effect, inject, input } from '@angular/core';
import { UsersService } from '../../core/services/users.service';
import { colorFor, initials } from '../../core/utils/format';

/**
 * Round avatar. Pass `userId` to resolve the person through the user
 * directory, or `name`/`src` to render directly.
 */
@Component({
  selector: 'app-avatar',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    '[class]': '"avatar avatar--" + size()',
    '[style.background]': 'src() ? null : color()',
    '[attr.title]': 'showTitle() ? displayName() : null',
  },
  template: `
    @if (src()) {
      <img [src]="src()" [alt]="displayName()" referrerpolicy="no-referrer" />
    } @else if (unassigned()) {
      <span class="i">person</span>
    } @else {
      {{ letters() }}
    }
  `,
})
export class AvatarComponent {
  private readonly users = inject(UsersService);

  readonly userId = input<string | null | undefined>(null);
  readonly name = input<string | null | undefined>(null);
  readonly url = input<string | null | undefined>(null);
  readonly size = input<'xs' | 'sm' | 'md' | 'lg' | 'xl'>('sm');
  readonly showTitle = input(true);

  private readonly user = computed(() => this.users.get(this.userId()));
  protected readonly unassigned = computed(() => !this.userId() && !this.name());
  protected readonly displayName = computed(
    () => this.name() ?? this.user()?.fullName ?? (this.userId() ? '…' : 'Unassigned'),
  );
  protected readonly src = computed(() => this.url() ?? this.user()?.avatarUrl ?? null);
  protected readonly letters = computed(() => initials(this.displayName()));
  protected readonly color = computed(() =>
    this.unassigned() ? 'var(--surface-3)' : colorFor(this.userId() ?? this.name()),
  );

  constructor() {
    effect(() => {
      const id = this.userId();
      if (id && !this.name()) this.users.ensure([id]);
    });
  }
}

@Component({
  selector: 'app-avatar-stack',
  imports: [AvatarComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'avatar-stack' },
  template: `
    @for (id of visible(); track id) {
      <app-avatar [userId]="id" [size]="size()" />
    }
    @if (extra() > 0) {
      <span class="avatar avatar--{{ size() }} avatar--more">+{{ extra() }}</span>
    }
  `,
})
export class AvatarStackComponent {
  readonly ids = input<string[]>([]);
  readonly total = input<number | null>(null);
  readonly max = input(4);
  readonly size = input<'xs' | 'sm' | 'md'>('sm');

  protected readonly visible = computed(() => this.ids().slice(0, this.max()));
  protected readonly extra = computed(
    () => (this.total() ?? this.ids().length) - this.visible().length,
  );
}
