---
paths:
  - "Frontend/**"
---

# Frontend (Angular 21) rules

## Components

- Standalone only. Don't write `standalone: true`, because it's the default.
- Use `changeDetection: ChangeDetectionStrategy.OnPush` with separate `templateUrl` + `styleUrl` (`.html` / `.scss`) files. Inline templates are fine for tiny shared components.
- Inject with `inject()` into `private readonly` / `protected readonly` fields, never constructor parameters.
- State lives in `signal()`, derived state in `computed()`. Use `set` / `update`, never `mutate`. Wrap side effects in `effect()` with `untracked()` around the action.
- Use `input()` / `output()`, not `@Input` / `@Output`. Host bindings go in the `host: {}` object, not `@HostBinding` / `@HostListener`.
- Templates use `@if` / `@for (…; track …)` / `@switch`, plus `[class.x]` / `[style.x]` instead of `ngClass` / `ngStyle`.
- Use Reactive forms and Angular Material (`MatDialog`, menus, tooltips). Dialogs are named `x-form.dialog.ts`.
- Accessibility: pass AXE and WCAG AA, which covers labels, focus handling in dialogs and contrast that works in both themes.

## Data access

- Every HTTP call goes through `core/http/api-client.ts` → `ApiClient.get/post/patch/delete(service, path)`. It builds `/<service>/api/v1<path>`, unwraps the JSend `data` and turns errors into `ApiError`. Don't call `HttpClient` directly.
- One service per backend service in `core/services/*.service.ts` (`providedIn: 'root'`). Pick the named key out of `data`, e.g. `.pipe(map(d => d.task))`.
- Types in `core/models/index.ts` mirror the backend DTOs field for field. Update them whenever a DTO changes.
- Report failures with `toast.error(err)` (it accepts an `ApiError` directly) and successes with `toast.success(msg)`. Ask for confirmation through `ConfirmService` before anything destructive.

## Routing & styles

- Lazy load every feature: `loadComponent: () => import(...)` in `app.routes.ts`, with `title: '<Page> · TaskFlow'`. Authenticated pages go under the `ShellComponent` children with `authGuard`.
- Use the design tokens in `src/styles/_tokens.scss` instead of hard-coded colors. Component styles must stay under the 6 kB budget.
- Prettier: 100 cols, single quotes. Run `npm run format`, then `npm run build` before you call frontend work done.
