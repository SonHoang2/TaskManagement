import { Component } from '@angular/core';

@Component({
  selector: 'app-tasks',
  standalone: true,
  template: `
    <div class="feature-placeholder">
      <h1>Tasks</h1>
      <p>Tasks management will be implemented in Phase 3</p>
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
export class TasksComponent {}
