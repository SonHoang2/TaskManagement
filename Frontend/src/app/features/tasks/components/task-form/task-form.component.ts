import { ChangeDetectionStrategy, Component, inject, output, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import type { Task, CreateTaskRequest, UpdateTaskRequest } from '../../models/task.model';

@Component({
  selector: 'app-task-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './task-form.component.html',
  styleUrl: './task-form.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TaskFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly dialogRef = inject(MatDialogRef<TaskFormComponent>, { optional: true });
  readonly data = inject(MAT_DIALOG_DATA, { optional: true }) || {};

  // Giá trị tĩnh, truyền vào 1 lần lúc mở dialog -> không cần signal
  readonly task: Task | null = this.data.task || null;
  readonly projectId: string = this.data.projectId || '';
  readonly availableUsers: Array<{ id: string; name: string; email: string }> =
    this.data.availableUsers || [];
  readonly isLoading: boolean = this.data.isLoading || false;

  readonly save = output<CreateTaskRequest | UpdateTaskRequest>();
  readonly cancel = output<void>();

  protected readonly taskForm: FormGroup = this.fb.group({
    title: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(200)]],
    description: ['', [Validators.maxLength(1000)]],
    status: ['TODO', Validators.required],
    priority: ['MEDIUM', Validators.required],
    assigneeId: [''],
    dueDate: [null],
  });

  protected readonly statusOptions = [
    { value: 'TODO', label: 'To Do' },
    { value: 'IN_PROGRESS', label: 'In Progress' },
    { value: 'DONE', label: 'Done' },
  ];

  protected readonly priorityOptions = [
    { value: 'LOW', label: 'Low' },
    { value: 'MEDIUM', label: 'Medium' },
    { value: 'HIGH', label: 'High' },
  ];

  // Không còn là signal/computed, chỉ là boolean thường
  protected readonly isEditMode: boolean = this.task !== null;

  ngOnInit(): void {
    if (this.task) {
      this.populateForm(this.task);
    }
  }

  private populateForm(task: Task): void {
    this.taskForm.patchValue({
      title: task.title,
      description: task.description || '',
      status: task.status,
      priority: task.priority,
      assigneeId: task.assigneeId || '',
      dueDate: task.dueDate ? new Date(task.dueDate) : null,
    });
  }

  protected onSubmit(): void {
    if (this.taskForm.invalid) {
      this.taskForm.markAllAsTouched();
      return;
    }

    const formValue = this.taskForm.value;

    if (this.task) {
      const updateRequest: UpdateTaskRequest = {
        title: formValue.title,
        description: formValue.description || undefined,
        status: formValue.status,
        priority: formValue.priority,
        assigneeId: formValue.assigneeId || undefined,
        dueDate: formValue.dueDate ? formValue.dueDate.toISOString() : undefined,
      };
      this.save.emit(updateRequest);
      if (this.dialogRef) {
        this.dialogRef.close(updateRequest);
      }
    } else {
      const createRequest: CreateTaskRequest = {
        projectId: this.projectId,
        title: formValue.title,
        description: formValue.description || undefined,
        status: formValue.status,
        priority: formValue.priority,
        assigneeId: formValue.assigneeId || undefined,
        dueDate: formValue.dueDate ? formValue.dueDate.toISOString() : undefined,
      };
      this.save.emit(createRequest);
      if (this.dialogRef) {
        this.dialogRef.close(createRequest);
      }
    }
  }

  protected onCancel(): void {
    this.cancel.emit();
    if (this.dialogRef) {
      this.dialogRef.close();
    }
  }

  protected getErrorMessage(controlName: string): string {
    const control = this.taskForm.get(controlName);
    if (!control || !control.errors || !control.touched) {
      return '';
    }

    const errors = control.errors;
    if (errors['required']) {
      return 'This field is required';
    }
    if (errors['minlength']) {
      return `Minimum length is ${errors['minlength'].requiredLength} characters`;
    }
    if (errors['maxlength']) {
      return `Maximum length is ${errors['maxlength'].requiredLength} characters`;
    }
    return 'Invalid value';
  }
}
