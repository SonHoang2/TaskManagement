import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { MatDialogRef } from '@angular/material/dialog';
import { WorkspaceService } from '../core/services/workspace.service';
import { ThemeService } from '../core/ui/theme.service';
import { STATUS_META, colorFor, taskKey } from '../core/utils/format';
import { DialogsService } from '../shared/dialogs.service';

interface Item {
  id: string;
  group: 'Tasks' | 'Projects' | 'Go to' | 'Actions';
  label: string;
  hint?: string;
  icon: string;
  color?: string;
  run: () => void;
}

@Component({
  selector: 'app-command-palette',
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './command-palette.component.html',
})
export class CommandPaletteComponent {
  private readonly ref = inject(MatDialogRef<CommandPaletteComponent>);
  private readonly router = inject(Router);
  private readonly workspace = inject(WorkspaceService);
  private readonly dialogs = inject(DialogsService);
  private readonly theme = inject(ThemeService);

  protected readonly query = signal('');
  protected readonly active = signal(0);

  private readonly commands: Item[] = [
    this.nav('Dashboard', '/dashboard', 'space_dashboard'),
    this.nav('My tasks', '/my-tasks', 'task_alt'),
    this.nav('Projects', '/projects', 'folder_open'),
    this.nav('Team', '/team', 'group'),
    this.nav('Invitations', '/invitations', 'mail'),
    this.nav('Notifications', '/notifications', 'notifications'),
    this.nav('Settings', '/settings', 'settings'),
    {
      id: 'new-task',
      group: 'Actions',
      label: 'Create task',
      icon: 'add_task',
      run: () => this.dialogs.taskForm().subscribe(),
    },
    {
      id: 'new-project',
      group: 'Actions',
      label: 'Create project',
      icon: 'create_new_folder',
      run: () =>
        this.dialogs.projectForm().subscribe((p) => p && this.router.navigate(['/projects', p.id])),
    },
    {
      id: 'theme',
      group: 'Actions',
      label: 'Toggle dark mode',
      icon: 'contrast',
      run: () => this.theme.toggle(),
    },
  ];

  protected readonly results = computed<Item[]>(() => {
    const q = this.query().trim().toLowerCase();
    const match = (s: string) => !q || s.toLowerCase().includes(q);

    const projects: Item[] = this.workspace
      .projects()
      .filter((p) => match(p.name))
      .slice(0, q ? 6 : 4)
      .map((p) => ({
        id: 'p' + p.id,
        group: 'Projects',
        label: p.name,
        hint: `${p.taskStats?.total ?? 0} tasks`,
        icon: 'folder',
        color: colorFor(p.id),
        run: () => this.router.navigate(['/projects', p.id]),
      }));

    const tasks: Item[] = q
      ? this.workspace
          .tasks()
          .filter((t) => match(t.title) || taskKey(t).toLowerCase() === q)
          .slice(0, 8)
          .map((t) => ({
            id: 't' + t.id,
            group: 'Tasks',
            label: t.title,
            hint: this.workspace.project(t.projectId)?.name,
            icon: STATUS_META[t.status].icon,
            run: () => this.dialogs.taskDetail(t.id).subscribe(),
          }))
      : [];

    const commands = this.commands.filter((c) => match(c.label));
    return [...tasks, ...projects, ...commands];
  });

  constructor() {
    this.workspace.load();
  }

  protected onKey(e: KeyboardEvent): void {
    const n = this.results().length;
    if (e.key === 'ArrowDown') {
      e.preventDefault();
      this.active.update((i) => (i + 1) % Math.max(n, 1));
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      this.active.update((i) => (i - 1 + n) % Math.max(n, 1));
    } else if (e.key === 'Enter') {
      const item = this.results()[this.active()];
      if (item) this.select(item);
    }
  }

  protected select(item: Item): void {
    this.ref.close();
    item.run();
  }

  private nav(label: string, path: string, icon: string): Item {
    return { id: path, group: 'Go to', label, icon, run: () => this.router.navigateByUrl(path) };
  }
}
