import { Injectable, signal } from '@angular/core';

export type ThemePref = 'light' | 'dark' | 'system';

const KEY = 'tm.theme';

@Injectable({ providedIn: 'root' })
export class ThemeService {
  readonly pref = signal<ThemePref>(readPref());
  readonly isDark = signal(false);
  private readonly media = window.matchMedia('(prefers-color-scheme: dark)');

  constructor() {
    this.media.addEventListener('change', () => this.apply());
    this.apply();
  }

  set(pref: ThemePref): void {
    this.pref.set(pref);
    try {
      localStorage.setItem(KEY, pref);
    } catch {
      /* ignore */
    }
    this.apply();
  }

  toggle(): void {
    this.set(this.isDark() ? 'light' : 'dark');
  }

  private apply(): void {
    const pref = this.pref();
    const dark = pref === 'dark' || (pref === 'system' && this.media.matches);
    this.isDark.set(dark);
    const root = document.documentElement;
    root.dataset['theme'] = dark ? 'dark' : 'light';
    root.style.colorScheme = dark ? 'dark' : 'light';
  }
}

function readPref(): ThemePref {
  try {
    const v = localStorage.getItem(KEY);
    if (v === 'light' || v === 'dark' || v === 'system') return v;
  } catch {
    /* ignore */
  }
  return 'system';
}
