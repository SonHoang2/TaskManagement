# TaskFlow — Frontend

Angular 21 single-page app for the Task Management microservices. Every request goes through the
API gateway as `/<service>/api/v1/...` (see `src/app/core/http/api-client.ts`).

## Run

```bash
npm install
npm start          # http://localhost:3000 (gateway expected at http://localhost:8765)
npm run build      # production build -> dist/Frontend
```

The gateway URL lives in `src/environments/environment*.ts`. The gateway's CORS config already allows
`http://localhost:3000`.

## Features

| Area          | What you can do                                                                                   | Backend                       |
| ------------- | ------------------------------------------------------------------------------------------------- | ----------------------------- |
| Auth          | Sign in / sign up, session expiry handling, redirect back after login                             | user-service `/auth`          |
| Dashboard     | Personal stats, "my focus" list, status donut across projects, recent projects, platform totals    | workspace data + dashboard    |
| My tasks      | Tasks across all projects: assigned / created / all, filters, group by status · due date · project | task-service                  |
| Projects      | Grid/list, search, sort, pagination, create / edit / delete                                        | project-service               |
| Board         | Kanban with drag & drop, quick add, filters (assignee, priority, label, sprint)                    | task-service                  |
| Task details  | Inline edit, status/priority/assignee/dates, labels, sprint, links, comments, activity history     | task-service, sprint-service  |
| Sprints       | Create/edit/start/complete/delete, progress, backlog → sprint planning                             | sprint-service                |
| Members       | Member list with roles, search users and send invitations, invitation status                      | project-service, user-service |
| Labels        | Create/rename/recolor/delete project labels                                                         | project-service               |
| Invitations   | Accept / decline invitations                                                                        | project-service               |
| Notifications | Bell with unread badge (polls every 60s), full page with read/unread/delete                        | notification-service          |
| Team          | Teammates across your projects, user directory                                                      | project-service, user-service |
| Settings      | Profile, avatar, password, light/dark/system theme                                                  | user-service                  |

`Ctrl/⌘ + K` opens a command palette to jump to tasks, projects and pages.

## Structure

```
src/app
├── core        # API client, auth, models, data services, toast/confirm/theme, formatting helpers
├── shared      # Avatar, badges, empty states, pipes, DialogsService
├── layout      # Shell (sidebar + topbar), notification bell, command palette
└── features    # auth, dashboard, tasks, projects, board, sprints, invitations, notifications, team, settings
src/styles      # Design tokens (light/dark) and shared component styles
```

## Notes on backend behaviour

- Task history is written by the frontend (`POST /task-histories`) whenever a tracked field changes,
  because task-service does not record it itself.
- The backend's PATCH endpoints skip `null` fields, so a task can't be un-assigned and a due date can't
  be cleared from the UI.
- Attachments are links (`fileUrl` + `fileName`); the backend has no file upload endpoint.
- There are no endpoints to remove a member or change a member's role, so the Members tab is read-only
  apart from invitations.
