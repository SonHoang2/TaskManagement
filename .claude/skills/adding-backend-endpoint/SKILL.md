---
name: adding-backend-endpoint
description: Use when adding or changing a REST endpoint, resource or field in one of the Spring Boot services under Backend/ (user, project, task, sprint, notification, dashboard), including new DTO fields the Angular frontend must see.
---

# Adding a backend endpoint

## Overview

One endpoint touches five layers: DTO → service → controller → tests → consumers (Postman, frontend model). Skipping the last two is the usual way contracts drift. The conventions themselves are in `.claude/rules/backend.md`. This skill is the order of work.

## Checklist

1. **Find the feature package**: `Backend/<svc>/src/main/java/com/sonhoang2/<pkg>/<feature>/`. Read the sibling controller and `*ServiceImpl` first and copy their shape.
2. **DTOs** in `dto/`: Lombok `@Data`, jakarta validation with messages (`@NotBlank(message = "title is required")`). Responses use `@Builder`. Never expose an entity.
3. **Repository**: derived queries first. Use `@Query` only for aggregates (see `countByStatusGroupedByProjectIdIn`). Add an `@Index` on the entity for any new filter column.
4. **Service interface + impl**: the method takes `UUID userId`. Its first line is the access check (`projectAccessGuard.requireMember(...)` / `requireTaskMember(...)`). Throw `com.sonhoang2.common.exception.ResourceNotFoundException` for missing rows.
5. **Controller**: follow the pattern below.
6. **Tests**: write them alongside, in `src/test/java/...`. In `*ServiceImplTest`, cover the happy path, 403 (guard throws `AccessDeniedException`) and 404. In `*ControllerTest`, cover the status code and JSend shape. Run `./mvnw -pl <svc> -am test -Dtest='!*ApplicationTests' -Dsurefire.failIfNoSpecifiedTests=false` from `Backend/`.
7. **Consumers**:
   - Add a request YAML in `postman/collections/<service>/` (copy a sibling file).
   - Update the TS type in `Frontend/src/app/core/models/index.ts` and the method in `core/services/<x>.service.ts`.
   - If another service reads this DTO through Feign, update its copy in that service's `common/dto`.

## Controller pattern

```java
@GetMapping("/project/{projectId}/stats")
public ResponseEntity<JSendResponse<Map<String, TaskStats>>> getTaskStatsByProjectId(
        @PathVariable UUID projectId,
        @RequestHeader("X-User-Id") UUID userId) {
    TaskStats stats = taskService.getTaskStatsByProjectId(projectId, userId);
    return ResponseEntity.ok(JSendResponse.success(Map.of("stats", stats)));   // named key, frontend reads d.stats
}
```

## Common mistakes

| Mistake | Consequence |
|---|---|
| Throwing `<svc>.common.exception.ResourceNotFoundException` in user/project/task-service | Returns 500 instead of 404 (not handled by common-lib's handler) |
| Returning the DTO without a `Map.of("key", …)` wrapper | Frontend `map(d => d.key)` yields `undefined` |
| Checking access in the controller or not at all | Any logged-in user reads other projects' data |
| Literal `/api/v1` in `@RequestMapping` | Path becomes `/api/v1/api/v1/...` |
| Using `PUT` with `ModelMapper` skip-null | Nulls can't clear fields. Use `PATCH`, and an explicit setter where clearing matters |
| Forgetting the frontend model | Silent `undefined` in templates. TypeScript can't catch it |
