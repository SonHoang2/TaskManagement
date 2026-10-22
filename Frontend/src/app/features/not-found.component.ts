import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { EmptyStateComponent } from '../shared/components/empty-state.component';

@Component({
  selector: 'app-not-found',
  imports: [RouterLink, EmptyStateComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="page">
      <app-empty-state
        icon="explore_off"
        title="Page not found"
        text="The page you're looking for doesn't exist or was moved."
      >
        <a class="btn btn--primary" routerLink="/dashboard"
          ><span class="i">home</span>Back to dashboard</a
        >
      </app-empty-state>
    </div>
  `,
})
export class NotFoundComponent {}
