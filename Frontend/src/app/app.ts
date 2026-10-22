import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ThemeService } from './core/ui/theme.service';
import { ToastService } from './core/ui/toast.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <router-outlet />
    <div class="toasts" aria-live="polite">
      @for (t of toast.toasts(); track t.id) {
        <div class="toast toast--{{ t.kind }}" role="status">
          <span class="i fill">{{
            t.kind === 'success' ? 'check_circle' : t.kind === 'error' ? 'error' : 'info'
          }}</span>
          <span>{{ t.message }}</span>
          <button type="button" (click)="toast.dismiss(t.id)" aria-label="Dismiss">
            <span class="i">close</span>
          </button>
        </div>
      }
    </div>
  `,
})
export class App {
  protected readonly toast = inject(ToastService);
  // Instantiated eagerly so the saved theme is applied on startup.
  private readonly theme = inject(ThemeService);
}
