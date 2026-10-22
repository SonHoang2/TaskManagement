import { ChangeDetectionStrategy, Component, input } from '@angular/core';

@Component({
  selector: 'app-empty-state',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'empty', '[class.empty--compact]': 'compact()' },
  template: `
    <div class="empty__icon">
      <span class="i">{{ icon() }}</span>
    </div>
    <h3>{{ title() }}</h3>
    @if (text()) {
      <p>{{ text() }}</p>
    }
    <div class="empty__actions"><ng-content /></div>
  `,
})
export class EmptyStateComponent {
  readonly icon = input('inbox');
  readonly title = input.required<string>();
  readonly text = input<string>('');
  readonly compact = input(false);
}

@Component({
  selector: 'app-skeleton',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'skeleton-list' },
  template: `
    @for (r of rows(); track $index) {
      <div class="skeleton" [style.height.px]="height()"></div>
    }
  `,
})
export class SkeletonComponent {
  readonly count = input(3);
  readonly height = input(56);
  protected rows(): number[] {
    return Array.from({ length: this.count() });
  }
}
