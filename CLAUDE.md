# Task Management Platform

Microservices task manager: Spring Boot 3.4 / Java 21 backend (`Backend/`), Angular 21 frontend (`Frontend/`), deployed to Kubernetes with Helm. Overview, ports and env vars: [README.md](README.md).

## Layout

- `Backend/common-lib` — shared JSend DTO, `GlobalExceptionHandler`, `@RateLimit`, Redis `MembershipCache`. Used by user/project/task (via `scanBasePackages`) and as a dependency elsewhere.
- `Backend/<name>-service` — one Maven module and one Postgres database per service. Package root `com.sonhoang2.<name>_service` (`userservice` for user-service).
- `Backend/api-gateway` — routes `/<service>/**` → `http://<service>` (StripPrefix=1), decodes the JWT, sets `X-User-Id`.
- `Backend/k8s/helm/taskmgmt` — the deploy chart; `Backend/observability` — Prometheus/Grafana/Alertmanager.
- `Frontend/src/app` — `core/` (http, auth, services, models), `features/<area>/`, `layout/`, `shared/`.
- `postman/collections/<service>/` — one request per YAML file.

## Commands

```bash
cd Backend && ./mvnw -pl task-service -am test                 # one service (-am builds common-lib too)
cd Backend && ./mvnw -B verify -Dtest='!*ApplicationTests' -Dsurefire.failIfNoSpecifiedTests=false   # what CI runs
cd Frontend && npm run build                                    # type-check + build
cd Frontend && npm run format                                   # prettier
```

`*ApplicationTests` need real Postgres/RabbitMQ, so skip them locally like CI does.

## Hard rules

- Never commit or print `Backend/.env`, `Backend/app.env` or `Backend/k8s/secret.yaml`; edit `secret.example.yaml` instead.
- Services trust `X-User-Id` from the gateway. Never read the user from a request body or the JWT inside a service.
- Every endpoint answers in JSend (`success` / `fail` / `error`). The one exception is dashboard-service, which the frontend reads with `ApiClient.getRaw`.
- The schema comes from Hibernate `ddl-auto=update` (there are no migrations). Renaming or dropping a column needs a manual SQL note in the PR.
- Never run `git commit` or `git push`, and never open a PR. The user commits and pushes. Leave changes in the working tree, and if asked, suggest a Conventional Commits message with a scope (`feat(task): …`, `fix(ratelimit): …`, `chore: …`).
- Write all code, identifiers, comments, logs, error messages, commit-message suggestions and project docs (rules, skills) in English. Keep comments short; only explain *why*.

Area rules load automatically from `.claude/rules/`. Project skills live in `.claude/skills/`.
