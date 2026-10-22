import { Injectable, signal } from '@angular/core';
import { errorMessage } from '../http/api-client';

export type ToastKind = 'success' | 'error' | 'info';

export interface Toast {
  id: number;
  kind: ToastKind;
  message: string;
}

@Injectable({ providedIn: 'root' })
export class ToastService {
  readonly toasts = signal<Toast[]>([]);
  private seq = 0;

  success(message: string): void {
    this.push('success', message);
  }

  info(message: string): void {
    this.push('info', message);
  }

  error(errOrMessage: unknown): void {
    this.push(
      'error',
      typeof errOrMessage === 'string' ? errOrMessage : errorMessage(errOrMessage),
      6000,
    );
  }

  dismiss(id: number): void {
    this.toasts.update((list) => list.filter((t) => t.id !== id));
  }

  private push(kind: ToastKind, message: string, ttl = 3500): void {
    const id = ++this.seq;
    this.toasts.update((list) => [...list.slice(-3), { id, kind, message }]);
    setTimeout(() => this.dismiss(id), ttl);
  }
}
