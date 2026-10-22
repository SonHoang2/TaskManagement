import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Invitation } from '../../core/models';
import { ProjectsService } from '../../core/services/projects.service';
import { UsersService } from '../../core/services/users.service';
import { WorkspaceService } from '../../core/services/workspace.service';
import { ToastService } from '../../core/ui/toast.service';
import { colorFor } from '../../core/utils/format';
import { AvatarComponent } from '../../shared/components/avatar.component';
import {
  EmptyStateComponent,
  SkeletonComponent,
} from '../../shared/components/empty-state.component';
import { AgoPipe } from '../../shared/pipes';

@Component({
  selector: 'app-invitations',
  imports: [AvatarComponent, EmptyStateComponent, SkeletonComponent, AgoPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './invitations.component.html',
  styleUrl: './invitations.component.scss',
})
export class InvitationsComponent {
  private readonly projects = inject(ProjectsService);
  private readonly workspace = inject(WorkspaceService);
  private readonly toast = inject(ToastService);
  private readonly router = inject(Router);
  protected readonly users = inject(UsersService);
  protected readonly colorFor = colorFor;

  protected readonly invitations = signal<Invitation[]>([]);
  protected readonly loading = signal(true);
  protected readonly busy = signal<string | null>(null);
  private readonly names = signal<Record<string, string>>({});

  protected readonly pending = computed(() =>
    this.invitations().filter((i) => i.status === 'PENDING'),
  );
  protected readonly history = computed(() =>
    this.invitations().filter((i) => i.status !== 'PENDING'),
  );

  constructor() {
    this.projects.myInvitations().subscribe({
      next: (list) => {
        const sorted = [...list].sort((a, b) => b.createdAt.localeCompare(a.createdAt));
        this.invitations.set(sorted);
        this.users.ensure(sorted.map((i) => i.invitedById));
        this.loading.set(false);
        this.resolveNames(sorted);
      },
      error: (e) => {
        this.loading.set(false);
        this.toast.error(e);
      },
    });
  }

  protected projectName(id: string): string {
    return this.workspace.project(id)?.name ?? this.names()[id] ?? 'Project';
  }

  /** Invitees can't GET a project they haven't joined, so look names up in the public listing. */
  private resolveNames(list: Invitation[]): void {
    const unknown = list.some((i) => !this.workspace.project(i.projectId));
    if (!unknown) return;
    this.projects.all({ size: 100, sortBy: 'updatedat', sortDirection: 'desc' }).subscribe({
      next: (page) => this.names.set(Object.fromEntries(page.content.map((p) => [p.id, p.name]))),
      error: () => {},
    });
  }

  protected decide(inv: Invitation, decision: 'ACCEPT' | 'REJECT'): void {
    this.busy.set(inv.id);
    this.projects.decide(inv.id, decision).subscribe({
      next: (updated) => {
        this.busy.set(null);
        this.invitations.update((l) => l.map((i) => (i.id === inv.id ? updated : i)));
        this.workspace.load(true);
        if (decision === 'ACCEPT') {
          this.toast.success(`You joined ${this.projectName(inv.projectId)}`);
          this.router.navigate(['/projects', inv.projectId]);
        } else {
          this.toast.info('Invitation declined');
        }
      },
      error: (e) => {
        this.busy.set(null);
        this.toast.error(e);
      },
    });
  }
}
