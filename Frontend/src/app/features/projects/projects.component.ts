import { Component } from '@angular/core';

@Component({
  selector: 'app-projects',
  standalone: true,
  template: `
    <div class="feature-placeholder">
      <h1>Projects</h1>
      <p>Projects management will be implemented in Phase 4</p>
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
export class ProjectsComponent {}
