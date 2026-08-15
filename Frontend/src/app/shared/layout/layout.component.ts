import { Component, signal, inject, DestroyRef } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { SidebarComponent } from './sidebar/sidebar.component';
import { HeaderComponent } from './header/header.component';
import { MatButtonModule } from '@angular/material/button';

@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [RouterOutlet, SidebarComponent, HeaderComponent, MatButtonModule],
  templateUrl: './layout.component.html',
  styleUrl: './layout.component.scss',
})
export class LayoutComponent {
  protected readonly isSidebarOpen = signal(true);
  protected readonly isMobileSidebarOpen = signal(false);
  private readonly isMobile = signal(false);
  private readonly destroyRef = inject(DestroyRef);

  constructor() {
    this.checkScreenSize();

    const resizeHandler = () => this.checkScreenSize();
    window.addEventListener('resize', resizeHandler);

    this.destroyRef.onDestroy(() => {
      window.removeEventListener('resize', resizeHandler);
    });
  }

  private checkScreenSize() {
    const mobile = window.innerWidth < 768;
    this.isMobile.set(mobile);

    if (mobile) {
      this.isSidebarOpen.set(false);
    } else {
      this.isSidebarOpen.set(true);
      this.isMobileSidebarOpen.set(false);
    }
  }

  toggleSidebar() {
    if (this.isMobile()) {
      this.isMobileSidebarOpen.update((isOpen) => !isOpen);
    } else {
      this.isSidebarOpen.update((isOpen) => !isOpen);
    }
  }

  closeMobileSidebar() {
    this.isMobileSidebarOpen.set(false);
  }
}
