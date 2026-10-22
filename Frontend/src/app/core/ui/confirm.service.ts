import { Injectable, inject } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { Observable, map } from 'rxjs';
import { ConfirmDialogComponent, ConfirmOptions } from './confirm-dialog.component';

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
