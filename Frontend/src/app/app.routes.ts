import { Routes } from '@angular/router';
import { authGuard, guestGuard } from './core/auth/guards';
import { ShellComponent } from './layout/shell.component';

export const routes: Routes = [
  {
    path: 'login',
    canActivate: [guestGuard],
    title: 'Sign in · TaskFlow',
    loadComponent: () => import('./features/auth/login.component').then((m) => m.LoginComponent),
  },
  {
    path: 'signup',
    canActivate: [guestGuard],
    title: 'Create account · TaskFlow',
    loadComponent: () => import('./features/auth/signup.component').then((m) => m.SignupComponent),
  },
  {
    path: '',
    component: ShellComponent,
    canActivate: [authGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      {
        path: 'dashboard',
        title: 'Dashboard · TaskFlow',
        loadComponent: () =>
          import('./features/dashboard/dashboard.component').then((m) => m.DashboardComponent),
      },
      {
        path: 'my-tasks',
        title: 'My tasks · TaskFlow',
        loadComponent: () =>
          import('./features/tasks/my-tasks.component').then((m) => m.MyTasksComponent),
      },
      {
        path: 'projects',
        title: 'Projects · TaskFlow',
        loadComponent: () =>
          import('./features/projects/project-list.component').then((m) => m.ProjectListComponent),
      },
      {
        path: 'projects/:projectId',
        loadComponent: () =>
          import('./features/projects/project-detail.component').then(
            (m) => m.ProjectDetailComponent,
          ),
        children: [
          { path: '', pathMatch: 'full', redirectTo: 'board' },
          {
            path: 'board',
            loadComponent: () =>
              import('./features/board/board.component').then((m) => m.BoardComponent),
          },
          {
            path: 'list',
            loadComponent: () =>
              import('./features/projects/project-tasks.component').then(
                (m) => m.ProjectTasksComponent,
              ),
          },
          {
            path: 'sprints',
            loadComponent: () =>
              import('./features/sprints/sprints.component').then((m) => m.SprintsComponent),
          },
          {
            path: 'members',
            loadComponent: () =>
              import('./features/projects/project-members.component').then(
                (m) => m.ProjectMembersComponent,
              ),
          },
          {
            path: 'labels',
            loadComponent: () =>
              import('./features/projects/project-labels.component').then(
                (m) => m.ProjectLabelsComponent,
              ),
          },
          {
            path: 'settings',
            loadComponent: () =>
              import('./features/projects/project-settings.component').then(
                (m) => m.ProjectSettingsComponent,
              ),
          },
        ],
      },
      {
        path: 'invitations',
        title: 'Invitations · TaskFlow',
        loadComponent: () =>
          import('./features/invitations/invitations.component').then(
            (m) => m.InvitationsComponent,
          ),
      },
      {
        path: 'notifications',
        title: 'Notifications · TaskFlow',
        loadComponent: () =>
          import('./features/notifications/notifications.component').then(
            (m) => m.NotificationsComponent,
          ),
      },
      {
        path: 'team',
        title: 'Team · TaskFlow',
        loadComponent: () => import('./features/team/team.component').then((m) => m.TeamComponent),
      },
      {
        path: 'settings',
        title: 'Settings · TaskFlow',
        loadComponent: () =>
          import('./features/settings/settings.component').then((m) => m.SettingsComponent),
      },
      {
        path: '**',
        title: 'Not found · TaskFlow',
        loadComponent: () =>
          import('./features/not-found.component').then((m) => m.NotFoundComponent),
      },
    ],
  },
];
