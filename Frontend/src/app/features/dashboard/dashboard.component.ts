import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="dashboard-placeholder">
      <h1>Dashboard</h1>
      <p>Dashboard will be implemented in Phase 6</p>
    </div>
  `,
  styles: `
    .dashboard-placeholder {
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
  `,
})
export class DashboardComponent {}
