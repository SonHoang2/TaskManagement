import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { PlatformDashboard, TaskDetail, TaskStatus } from '../../core/models';
import { DashboardService } from '../../core/services/dashboard.service';
import { WorkspaceService } from '../../core/services/workspace.service';
import { STATUS_META, colorFor, compareTasks, daysUntil, greeting } from '../../core/utils/format';
import { AvatarStackComponent } from '../../shared/components/avatar.component';
import { DueDateComponent, PriorityBadgeComponent } from '../../shared/components/badges';
import {
  EmptyStateComponent,
  SkeletonComponent,
} from '../../shared/components/empty-state.component';
import { DialogsService } from '../../shared/dialogs.service';
import { AgoPipe } from '../../shared/pipes';

@Component({
  selector: 'app-dashboard',
  imports: [
    RouterLink,
    AvatarStackComponent,
    PriorityBadgeComponent,
    DueDateComponent,
    EmptyStateComponent,
    SkeletonComponent,
    AgoPipe,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss',
})
export class DashboardComponent {
  protected readonly auth = inject(AuthService);
  protected readonly ws = inject(WorkspaceService);
  private readonly dashboardApi = inject(DashboardService);
  private readonly dialogs = inject(DialogsService);

  protected readonly statusMeta = STATUS_META;
  protected readonly colorFor = colorFor;
  protected readonly greeting = greeting();
  protected readonly today = new Date().toLocaleDateString('en-US', {
    weekday: 'long',
    month: 'long',
    day: 'numeric',
  });
  protected readonly platform = signal<PlatformDashboard | null>(null);

  protected readonly firstName = computed(
    () => this.auth.user()?.fullName?.split(' ')[0] ?? 'there',
  );

  private readonly mine = computed(() => this.ws.myTasks());
  private readonly open = computed(() => this.mine().filter((t) => t.status !== 'DONE'));

  protected readonly stats = computed(() => {
    const open = this.open();
    return {
      open: open.length,
      inProgress: open.filter((t) => t.status === 'IN_PROGRESS').length,
      dueSoon: open.filter(
        (t) => t.dueDate && daysUntil(t.dueDate) >= 0 && daysUntil(t.dueDate) <= 7,
      ).length,
      overdue: open.filter((t) => t.dueDate && daysUntil(t.dueDate) < 0).length,
      done: this.mine().length - open.length,
    };
  });

  protected readonly focus = computed(() => [...this.open()].sort(compareTasks).slice(0, 8));

  protected readonly distribution = computed(() => {
    const all = this.ws.tasks();
    const count = (s: TaskStatus) => all.filter((t) => t.status === s).length;
    const total = all.length;
    const rows = (['TODO', 'IN_PROGRESS', 'DONE'] as TaskStatus[]).map((s) => ({
      status: s,
      meta: STATUS_META[s],
      count: count(s),
      pct: total ? (count(s) / total) * 100 : 0,
    }));
    // SVG donut segments: circumference of r=42 is ~263.9
    const C = 2 * Math.PI * 42;
    let offset = 0;
    const segments = rows.map((r) => {
      const len = (r.pct / 100) * C;
      const seg = { ...r, dash: `${len} ${C - len}`, offset: -offset };
      offset += len;
      return seg;
    });
    return {
      total,
      rows,
      segments,
      donePct: total ? Math.round((count('DONE') / total) * 100) : 0,
    };
  });

  protected readonly recentProjects = computed(() => this.ws.projects().slice(0, 4));

  constructor() {
    this.ws.load();
    this.dashboardApi.platform().subscribe({ next: (d) => this.platform.set(d), error: () => {} });
  }

  protected openTask(t: TaskDetail): void {
    this.dialogs.taskDetail(t.id).subscribe((c) => c && this.ws.load(true));
  }

  protected newTask(): void {
    this.dialogs.taskForm().subscribe((t) => t && this.ws.load(true));
  }

  protected newProject(): void {
    this.dialogs.projectForm().subscribe();
  }

  protected projectName(id: string): string {
    return this.ws.project(id)?.name ?? '';
  }

  protected pct(done: number, total: number): number {
    return total ? Math.round((done / total) * 100) : 0;
  }

  protected memberIds(ids: { id: string }[] | null | undefined): string[] {
    return (ids ?? []).map((m) => m.id);
  }
}
