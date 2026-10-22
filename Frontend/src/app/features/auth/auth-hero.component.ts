import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
  selector: 'app-auth-hero',
  changeDetection: ChangeDetectionStrategy.OnPush,

  template: `
    <div class="hero__brand">
      <span class="mark"><span class="i fill">bolt</span></span
      >TaskFlow
    </div>
    <div>
      <h1>Plan, track and ship work — together.</h1>
      <p>Projects, kanban boards, sprints and a clear view of what's on your plate today.</p>
      <ul class="features">
        <li><span class="i">view_kanban</span>Drag-and-drop kanban boards</li>
        <li><span class="i">sprint</span>Sprint planning with progress tracking</li>
        <li><span class="i">forum</span>Comments, labels, links and full activity history</li>
      </ul>
    </div>
    <div class="hero__foot">© {{ year }} TaskFlow · Task Management Platform</div>
  `,
  styleUrl: './auth-hero.scss',
})
export class AuthHeroComponent {
  protected readonly year = new Date().getFullYear();
}
