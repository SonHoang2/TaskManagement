import {
  ChangeDetectionStrategy,
  Component,
  effect,
  inject,
  input,
  untracked,
} from '@angular/core';
import { Title } from '@angular/platform-browser';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { colorFor } from '../../core/utils/format';
import { AvatarStackComponent } from '../../shared/components/avatar.component';
import { EmptyStateComponent } from '../../shared/components/empty-state.component';
import { DialogsService } from '../../shared/dialogs.service';
import { ProjectContext } from './project-context';

@Component({
  selector: 'app-project-detail',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, AvatarStackComponent, EmptyStateComponent],
  providers: [ProjectContext],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './project-detail.component.html',
  styleUrl: './project-detail.component.scss',
})
export class ProjectDetailComponent {
  protected readonly ctx = inject(ProjectContext);
  private readonly dialogs = inject(DialogsService);
  private readonly title = inject(Title);
  protected readonly colorFor = colorFor;

  readonly projectId = input.required<string>();

  protected memberIds(): string[] {
    return this.ctx.members().map((m) => m.userId);
  }

  constructor() {
    effect(() => {
      const id = this.projectId();
      untracked(() => this.ctx.load(id));
    });
    effect(() => {
      const p = this.ctx.project();
      if (p) this.title.setTitle(`${p.name} · TaskFlow`);
    });
  }

  protected newTask(): void {
    this.dialogs
      .taskForm({ projectId: this.ctx.id(), sprintId: this.ctx.activeSprint()?.id })
      .subscribe((t) => t && this.ctx.reloadTasks());
  }
}
