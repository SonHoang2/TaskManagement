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
  template: `
    @if (ctx.error()) {
      <div class="page">
        <app-empty-state icon="lock" title="Can't open this project" [text]="ctx.error()!">
          <a class="btn" routerLink="/projects"
            ><span class="i">arrow_back</span>Back to projects</a
          >
        </app-empty-state>
      </div>
    } @else {
      <div class="project-head">
        <div class="project-head__inner">
          <div class="eyebrow">
            <a routerLink="/projects">Projects</a><span class="i">chevron_right</span>
          </div>
          <div class="row" style="gap: 14px; align-items: flex-start; flex-wrap: wrap">
            <span class="project-icon" [style.background]="colorFor(ctx.id())">
              {{ ctx.project()?.name?.charAt(0) ?? '' }}
            </span>
            <div class="grow">
              @if (ctx.project(); as p) {
                <h1 class="truncate">{{ p.name }}</h1>
                <p class="muted truncate">{{ p.description || 'No description' }}</p>
              } @else {
                <div class="skeleton" style="width: 240px; height: 28px"></div>
                <div class="skeleton" style="width: 340px; height: 16px; margin-top: 8px"></div>
              }
            </div>
            <div class="row project-head__meta">
              <div
                class="progress-ring"
                [style.--pct]="ctx.stats().pct"
                [attr.title]="ctx.stats().pct + '% done'"
              >
                <span>{{ ctx.stats().pct }}%</span>
              </div>
              <a [routerLink]="['members']" class="row" style="gap: 6px">
                <app-avatar-stack [ids]="memberIds()" [max]="4" />
              </a>
              <button class="btn btn--primary" type="button" (click)="newTask()">
                <span class="i">add</span>New task
              </button>
            </div>
          </div>

          <nav class="tabs" style="margin-top: 18px">
            <a routerLink="board" routerLinkActive="active"
              ><span class="i">view_kanban</span>Board</a
            >
            <a routerLink="list" routerLinkActive="active"><span class="i">list</span>List</a>
            <a routerLink="sprints" routerLinkActive="active"
              ><span class="i">sprint</span>Sprints</a
            >
            <a routerLink="members" routerLinkActive="active"
              ><span class="i">group</span>Members
              <span class="count">{{ ctx.members().length }}</span></a
            >
            <a routerLink="labels" routerLinkActive="active"><span class="i">sell</span>Labels</a>
            <a routerLink="settings" routerLinkActive="active"
              ><span class="i">tune</span>Settings</a
            >
          </nav>
        </div>
      </div>
      <router-outlet />
    }
  `,
  styles: `
    .project-head {
      background: var(--surface);
      border-bottom: 1px solid var(--border);
      &__inner {
        padding: 20px 32px 0;
        @media (max-width: 768px) {
          padding: 16px 16px 0;
        }
      }
      h1 {
        font-size: 22px;
      }
      &__meta {
        gap: 14px;
      }
    }
    .tabs {
      border-bottom: 0;
      margin-bottom: -1px;
    }
    .project-icon {
      width: 44px;
      height: 44px;
      border-radius: 12px;
      flex-shrink: 0;
      display: grid;
      place-items: center;
      color: #fff;
      font-weight: 700;
      font-size: 19px;
      text-transform: uppercase;
      box-shadow: inset 0 -2px 0 rgba(0, 0, 0, 0.12);
    }
    .progress-ring {
      --pct: 0;
      width: 40px;
      height: 40px;
      border-radius: 50%;
      display: grid;
      place-items: center;
      background: conic-gradient(var(--done) calc(var(--pct) * 1%), var(--surface-3) 0);
      span {
        width: 32px;
        height: 32px;
        border-radius: 50%;
        display: grid;
        place-items: center;
        background: var(--surface);
        font-size: 10.5px;
        font-weight: 700;
      }
    }
  `,
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
