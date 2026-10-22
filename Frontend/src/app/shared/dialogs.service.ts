import { Injectable, inject } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { Observable, from, switchMap } from 'rxjs';
import { Project, Task, TaskStatus } from '../core/models';

export interface TaskFormData {
  /** Pre-selected project; when omitted the form lets the user pick one. */
  projectId?: string;
  status?: TaskStatus;
  sprintId?: string;
  task?: Task;
}

export interface TaskDetailData {
  taskId: string;
}

/** Opens the app-wide dialogs, lazily loading their code. */
@Injectable({ providedIn: 'root' })
export class DialogsService {
  private readonly dialog = inject(MatDialog);

  taskForm(data: TaskFormData = {}): Observable<Task | undefined> {
    return from(import('../features/tasks/task-form.dialog')).pipe(
      switchMap(({ TaskFormDialog }) =>
        this.dialog.open(TaskFormDialog, { data, width: '640px' }).afterClosed(),
      ),
    );
  }

  /** Emits true when anything about the task changed (or it was deleted). */
  taskDetail(taskId: string): Observable<boolean> {
    return from(import('../features/tasks/task-detail.dialog')).pipe(
      switchMap(({ TaskDetailDialog }) =>
        this.dialog
          .open(TaskDetailDialog, {
            data: { taskId } satisfies TaskDetailData,
            width: '920px',
            maxWidth: '100vw',
            panelClass: 'side-panel',
            autoFocus: false,
          })
          .afterClosed(),
      ),
    );
  }

  projectForm(project?: Project): Observable<Project | undefined> {
    return from(import('../features/projects/project-form.dialog')).pipe(
      switchMap(({ ProjectFormDialog }) =>
        this.dialog.open(ProjectFormDialog, { data: { project }, width: '520px' }).afterClosed(),
      ),
    );
  }

  commandPalette(): void {
    if (this.dialog.openDialogs.some((d) => d.id === 'palette')) return;
    import('../layout/command-palette.component').then(({ CommandPaletteComponent }) =>
      this.dialog.open(CommandPaletteComponent, {
        id: 'palette',
        width: '620px',
        position: { top: '12vh' },
        autoFocus: 'first-tabbable',
        panelClass: 'palette-panel',
      }),
    );
  }
}
