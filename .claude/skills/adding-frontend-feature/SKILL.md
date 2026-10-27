---
name: adding-frontend-feature
description: Use when adding or changing a page, dialog, component or API call in the Angular app under Frontend/ (TaskFlow UI), or wiring a new backend endpoint into the UI.
---

# Adding a frontend feature

## Overview

Data flows `ApiClient` → `core/services/<x>.service.ts` → feature component signals → template. Conventions are in `.claude/rules/frontend.md`. This skill is the order of work. The Angular dev server runs on port 3000 (`.claude/launch.json`, name `frontend`) and talks to the gateway at `environment.apiGatewayUrl`.

## Checklist

1. **Model**: add or adjust the interface in `core/models/index.ts`, mirroring the backend DTO exactly (UUID → `string`, `Instant` → ISO `string`, enum → string-literal union).
2. **Service method** in `core/services/<backend-service>.service.ts`:
   ```ts
   create(body: SprintInput & { projectId: string }): Observable<Sprint> {
     return this.api
       .post<{ sprint: Sprint }>('sprint-service', '/sprints', body)
       .pipe(map((d) => d.sprint)); // key = the backend's Map.of("sprint", …)
   }
   ```
   Lists unwrap `d.page.content` and pass `pageParams(q)` or explicit `size`/`sort`.
3. **Component** in `features/<area>/<name>.component.{ts,html,scss}`: OnPush, `inject()`, `signal` for loading/data, `computed` for derived state. Subscribe in a method, set signals in `next`, and call `toast.error(err)` in `error`. Project-scoped pages read shared state from `ProjectContext` instead of refetching.
4. **Dialog** (forms): `<name>-form.dialog.{ts,html}` opened through `MatDialog` / `DialogsService`, with Reactive forms. Map `ApiError.fieldErrors` to control errors on 400.
5. **Route**: lazy `loadComponent` in `app.routes.ts` under the shell children, with `title: '<Page> · TaskFlow'`. If it's a top-level page, add it to the nav items array in `layout/shell.component.ts` and to `this.nav(...)` in `layout/command-palette.component.ts`.
6. **States**: render loading (`SkeletonComponent`), empty (`EmptyStateComponent`) and error for every list.
7. **Verify**: `npm run format && npm run build` in `Frontend/`, then open the page through the `frontend` preview and check the console and network for failed calls (the gateway must be running for real data).

## Common mistakes

| Mistake | Fix |
|---|---|
| `HttpClient` injected in a component | Go through `ApiClient` so JSend unwrapping, URLs and `ApiError` stay consistent |
| `.pipe(map(d => d))` without the key | `undefined` everywhere. Unwrap the named key the controller used |
| Calling `dashboard-service` with `api.get` | It isn't JSend. Use `api.getRaw` |
| `*ngIf` / `ngClass` / constructor DI copied from old snippets | `@if`, `[class.x]`, `inject()` |
| Hard-coded hex colors | Tokens from `src/styles/_tokens.scss` (dark theme depends on them) |
| Component SCSS over 6 kB | The production build warns, and fails at 8 kB. Move shared styles to `src/styles/_components.scss` |
