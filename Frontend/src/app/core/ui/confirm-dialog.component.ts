import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

export interface ConfirmOptions {
  title: string;
  message: string;
  confirmText?: string;
  danger?: boolean;
}

@Component({
  selector: 'app-confirm-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './confirm-dialog.component.html',
})
export class ConfirmDialogComponent {
  protected readonly data = inject<ConfirmOptions>(MAT_DIALOG_DATA);
  protected readonly ref = inject(MatDialogRef<ConfirmDialogComponent, boolean>);
}
