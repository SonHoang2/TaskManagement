---
name: reviewing-changes
description: Use when asked to review a diff, branch, commit or PR in this repository, or before committing backend/frontend changes here, to catch bugs and violations of this project's microservice conventions.
---

# Reviewing changes in TaskManagement

## Overview

Find real bugs, not style nits. Every finding names `file:line`, a concrete failing scenario (input/state → wrong result) and a fix. Drop anything speculative. Report pre-existing bugs you trip over too, labelled as pre-existing.

## Procedure

1. Get the diff (`git diff master...HEAD`, or the commit/PR given). If you were given a commit, it may not be checked out, so read files at that revision (`git show <sha>:<path>`).
2. Read each touched file **in full** plus its direct callers. Run independent reads in parallel and keep exploration short.
3. Walk the checklists below. Then run the area's checks: `./mvnw -pl <svc> -am test -Dtest='!*ApplicationTests' -Dsurefire.failIfNoSpecifiedTests=false` in `Backend/`, and `npm run build` in `Frontend/`.
4. Report findings ordered by severity: **Bug → Security → Contract drift → Convention**. Say plainly if there are none.

## General checklist

Logic errors · unhandled edge cases (empty page, null optional field, enum `valueOf` on bad input) · null dereferences · races and concurrency · resource leaks · cache staleness (wrong key, missing eviction) · API contract violations · security holes.

## Project-specific checklist

| Check | Why it matters here |
|---|---|
| Every service method that reads or writes project data calls `ProjectAccessGuard` (or the service equivalent) with `X-User-Id` | Otherwise any user reaches any project |
| User id comes only from `@RequestHeader("X-User-Id")`, never the body | The gateway strips the client-supplied header. A body field bypasses that |
| Exceptions are `com.sonhoang2.common.exception.*` in user/project/task-service | Local copies turn 404/409 into 500 |
| Responses are JSend with a named `Map.of(key, …)`. The matching frontend `map(d => d.key)` and `core/models` type match | Contract drift is silent in TS |
| DTO/event field renames are mirrored in Feign DTO copies, event copies in the other service, and the frontend models | No shared jar for these. Jackson nulls missing fields |
| Membership changes call `membershipCache.evictAfterCommit` | task-service authorizes from that Redis cache |
| `@Cacheable` keys include every input that changes the result, and writes evict them | e.g. `taskDistribution` keyed by `userId` |
| Entity changes are safe under `ddl-auto=update` (no rename/drop expected to migrate) | No migration tool exists |
| New filter columns have an `@Index` | Tables are load-tested at large volume (`fake-data/`) |
| Rate-limited paths keep `@RateLimit` and a meaningful `keyPrefix` | Shared counters across endpoints otherwise |
| No secrets in diff (`.env`, `app.env`, `k8s/secret.yaml`, tokens in properties) | Repo is pushed to GHCR/CI |
| New service or env var is wired in compose, the Helm `values.yaml`, CI matrix, gateway routes | See `.claude/rules/infra.md` |
| Angular: signals/OnPush/`inject()`/control flow, `ApiClient` only, tokens not hex colors | See `.claude/rules/frontend.md` |
| Tests cover the new branch, including 403/404 paths | CI only runs unit tests |

## Output shape

```
1. [Bug] task-service/.../TaskServiceImpl.java:118 — `TaskStatus.valueOf(status)` throws on "done" → 400 "Invalid status value" hides the real field. Fix: validate/normalise in controller.
2. [Contract drift] ...
No other issues found.
```
