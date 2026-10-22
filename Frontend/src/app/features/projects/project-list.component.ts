import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Subject, debounceTime, startWith, switchMap, tap, catchError, of } from 'rxjs';
import { Page, ProjectSummary } from '../../core/models';
import { ProjectSort, ProjectsService } from '../../core/services/projects.service';
import { ToastService } from '../../core/ui/toast.service';
import { colorFor, percent } from '../../core/utils/format';
import { AvatarComponent, AvatarStackComponent } from '../../shared/components/avatar.component';
import { RoleBadgeComponent } from '../../shared/components/badges';
import { EmptyStateComponent } from '../../shared/components/empty-state.component';
import { DialogsService } from '../../shared/dialogs.service';
import { AgoPipe } from '../../shared/pipes';

const PAGE_SIZE = 12;

@Component({
  selector: 'app-project-list',
  imports: [
    FormsModule,
    RouterLink,
    AvatarComponent,
    AvatarStackComponent,
    RoleBadgeComponent,
    EmptyStateComponent,
    AgoPipe,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './project-list.component.html',
  styleUrl: './project-list.component.scss',
})
export class ProjectListComponent {
  private readonly projects = inject(ProjectsService);
  private readonly dialogs = inject(DialogsService);
  private readonly toast = inject(ToastService);
  private readonly router = inject(Router);
  protected readonly colorFor = colorFor;

  protected readonly search = signal('');
  protected readonly sort = signal<ProjectSort>('updatedat');
  protected readonly view = signal<'grid' | 'list'>('grid');
  protected readonly page = signal(0);
  protected readonly data = signal<Page<ProjectSummary> | null>(null);
  protected readonly loading = signal(true);

  private readonly reload$ = new Subject<void>();

  constructor() {
    this.reload$
      .pipe(
        startWith(undefined),
        debounceTime(200),
        tap(() => this.loading.set(true)),
        switchMap(() =>
          this.projects
            .mine({
              page: this.page(),
              size: PAGE_SIZE,
              search: this.search().trim() || undefined,
              sortBy: this.sort(),
              sortDirection: 'desc',
            })
            .pipe(
              catchError((e) => {
                this.toast.error(e);
                return of(null);
              }),
            ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((page) => {
        this.data.set(page);
        this.loading.set(false);
      });
  }

  protected refresh(resetPage = true): void {
    if (resetPage) this.page.set(0);
    this.reload$.next();
  }

  protected goTo(page: number): void {
    this.page.set(page);
    this.reload$.next();
  }

  protected create(): void {
    this.dialogs.projectForm().subscribe((p) => p && this.router.navigate(['/projects', p.id]));
  }

  protected pct(p: ProjectSummary): number {
    return percent(p.taskStats?.done ?? 0, p.taskStats?.total ?? 0);
  }

  protected memberIds(p: ProjectSummary): string[] {
    return (p.members ?? []).map((m) => m.id);
  }
}
