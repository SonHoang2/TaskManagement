import { ChangeDetectionStrategy, Component, Injectable, inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialog, MatDialogRef } from '@angular/material/dialog';
import { Observable, map } from 'rxjs';

export interface ConfirmOptions {
  title: string;
  message: string;
  confirmText?: string;
  danger?: boolean;
}

@Component({
  selector: 'app-confirm-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="dialog dialog--sm">
      <div class="confirm">
        <div class="confirm__icon" [class.confirm__icon--danger]="data.danger">
          <span class="i">{{ data.danger ? 'warning' : 'help' }}</span>
        </div>
        <div>
          <h2 class="dialog__title">{{ data.title }}</h2>
          <p class="muted">{{ data.message }}</p>
        </div>
      </div>
      <div class="dialog__footer">
        <button class="btn btn--ghost" type="button" (click)="ref.close(false)">Cancel</button>
        <button
          class="btn"
          [class.btn--danger]="data.danger"
          [class.btn--primary]="!data.danger"
          type="button"
          (click)="ref.close(true)"
          cdkFocusInitial
        >
          {{ data.confirmText ?? 'Confirm' }}
        </button>
      </div>
    </div>
  `,
})
export class ConfirmDialogComponent {
  protected readonly data = inject<ConfirmOptions>(MAT_DIALOG_DATA);
  protected readonly ref = inject(MatDialogRef<ConfirmDialogComponent, boolean>);
}

@Injectable({ providedIn: 'root' })
export class ConfirmService {
  private readonly dialog = inject(MatDialog);

  ask(opts: ConfirmOptions): Observable<boolean> {
    return this.dialog
      .open(ConfirmDialogComponent, { data: opts, width: '440px', autoFocus: 'dialog' })
      .afterClosed()
      .pipe(map((v) => v === true));
  }
}
