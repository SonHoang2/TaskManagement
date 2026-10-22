import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
  selector: 'app-auth-hero',
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './auth-hero.component.html',
  styleUrl: './auth-hero.component.scss',
})
export class AuthHeroComponent {
  protected readonly year = new Date().getFullYear();
}
