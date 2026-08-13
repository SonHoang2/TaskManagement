import { Component } from '@angular/core';

@Component({
  selector: 'app-settings',
  standalone: true,
  template: `
    <div class="feature-placeholder">
      <h1>Settings</h1>
      <p>Settings will be implemented in Phase 7</p>
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
export class SettingsComponent {}
