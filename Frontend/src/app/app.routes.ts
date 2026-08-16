import { Routes } from '@angular/router';
import { LoginComponent, SignupComponent } from './features/auth';
import { authGuard, guestGuard } from './features/auth';
import { DashboardComponent } from './features/dashboard/dashboard.component';
import { LayoutComponent } from './shared/layout';

export const routes: Routes = [
  {
    path: 'login',
    component: LoginComponent,
    canActivate: [guestGuard],
  },
  {
    path: 'signup',
    component: SignupComponent,
    canActivate: [guestGuard],
  },
  {
    path: '',
    component: LayoutComponent,
    canActivate: [authGuard],
    children: [
      {
        path: 'dashboard',
        component: DashboardComponent,
      },
      {
        path: 'tasks',
        loadComponent: () =>
          import('./features/tasks/tasks.component').then((m) => m.TasksComponent),
      },
      {
        path: 'projects',
        loadComponent: () =>
          import('./features/projects/projects.component').then((m) => m.ProjectsComponent),
      },
      {
        path: 'projects/new',
        loadComponent: () =>
          import('./features/projects/components/project-form/project-form.component').then(
            (m) => m.ProjectFormComponent,
          ),
      },
      {
        path: 'projects/:id',
        loadComponent: () =>
          import('./features/projects/components/project-detail/project-detail.component').then(
            (m) => m.ProjectDetailComponent,
          ),
      },
      {
        path: 'projects/:id/edit',
        loadComponent: () =>
          import('./features/projects/components/project-form/project-form.component').then(
            (m) => m.ProjectFormComponent,
          ),
      },
      {
        path: 'team',
        loadComponent: () => import('./features/team/team.component').then((m) => m.TeamComponent),
      },
      {
        path: 'settings',
        loadComponent: () =>
          import('./features/settings/settings.component').then((m) => m.SettingsComponent),
      },
      {
        path: '',
        redirectTo: '/dashboard',
        pathMatch: 'full',
      },
    ],
  },
  {
    path: '**',
    redirectTo: '/login',
  },
];
