import { Component } from '@angular/core';

@Component({
  selector: 'app-team',
  standalone: true,
  template: `
    <div class="feature-placeholder">
      <h1>Team</h1>
      <p>Team management will be implemented in Phase 5</p>
    </div>
  `,
  styles: `
    .feature-placeholder {
      padding: 40px;
      text-align: center;
      h1 {
        color: #333;
        margin-bottom: 16px;
      }
      p {
        color: #666;
      }
    }
  `
})
export class TeamComponent {}
