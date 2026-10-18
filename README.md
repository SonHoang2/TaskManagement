# Task Management Platform

[Tiếng Việt](README.vi.md) | **English**

A microservices task management platform: Spring Boot 3 / Java 21 on the backend, Angular on the frontend, deployed to Kubernetes (kind / k3s) with GitHub Actions CI/CD and Prometheus + Grafana monitoring.

## Features

- **Users**: registration, login, JWT, role-based access control.
- **Projects**: project CRUD, member invitations, label management.
- **Tasks**: task CRUD, comments, file attachments, change history, labels.
- **Sprints**: create sprints, assign tasks to sprints, track status.
- **Notifications**: asynchronous via RabbitMQ (task assigned, new comment, project invitation).
- **Dashboard**: aggregates figures from the project / task / user / sprint services.
- **Rate limiting**: at the gateway (Redis) and per method via the `@RateLimit` annotation (`common-lib`).

## Architecture

```
Angular (Frontend)
      │
      ▼
 API Gateway :8765  ── JWT + rate limit (Redis) ──►  /{service}/** (StripPrefix=1)
      │
      ├── user-service          :5001   authentication, users
      ├── project-service       :5002   projects, members, labels
      ├── task-service          :5003   tasks, comments, attachments, history
      ├── sprint-service        :5004   sprints
      ├── notification-service  :5005   notifications (RabbitMQ consumer)
      └── dashboard-service     :8086   aggregated figures (calls the other services)

Infrastructure: PostgreSQL (one database per service) · Redis · RabbitMQ
Monitoring:     Prometheus · Alertmanager · Grafana (· Loki + Promtail in docker compose)
```

- Service discovery uses **Kubernetes Service DNS** (e.g. `http://user-service`); there is no Eureka.
- The gateway decodes the JWT and forwards user information to downstream services via headers.
- Services call each other with OpenFeign / WebClient; events travel through RabbitMQ.
- Shared code (JSend, `PageResponse`, exception handler, rate limiting) lives in the `common-lib` module.

## Repository layout

```
.
├── Backend/
│   ├── pom.xml                  # Maven parent, lists the modules below
│   ├── common-lib/              # shared DTOs, exceptions, rate limiting
│   ├── api-gateway/
│   ├── user-service/ project-service/ task-service/
│   ├── sprint-service/ notification-service/ dashboard-service/
│   ├── docker-compose.yml       # run the whole stack locally
│   ├── init-db.sql              # creates the databases
│   ├── k8s/                     # manifests + deploy scripts (see k8s/README.md)
│   ├── observability/           # Prometheus rules, Alertmanager, Grafana dashboards
│   ├── load-tests/              # k6 (see load-tests/README.md)
│   └── fake-data/               # large-scale fake data generator
├── Frontend/                    # Angular 21
├── postman/                     # Postman collections + environment
└── .github/workflows/           # ci.yml, cd.yml
```

## Tech stack

| Area | Technologies |
|---|---|
| Backend | Java 21, Spring Boot 3.4.3, Spring Cloud (Gateway, OpenFeign), Spring Data JPA, Spring Security, Spring AMQP, Actuator + Micrometer |
| Frontend | Angular 21 |
| Data / messaging | PostgreSQL, Redis, RabbitMQ |
| Monitoring | Prometheus, Alertmanager, Grafana, Loki + Promtail (compose) |
| Build / run | Maven (wrapper), Docker, Docker Compose, Kubernetes (kind, k3s) |
| CI/CD | GitHub Actions, GitHub Container Registry (GHCR) |
| Testing | JUnit 5, Mockito, MockMvc, k6 (load testing) |

## Quick start

Requirements: Java 21, Docker (with Compose). Node.js is also needed for the frontend.

### Option 1: Docker Compose (fastest)

```bash
cd Backend
# create a .env file (variables listed below), then:
docker compose up -d --build
```

Variables required in `Backend/.env`: `POSTGRES_USER`, `POSTGRES_PASSWORD`, `POSTGRES_DB`, `DB_USERNAME`, `DB_PASSWORD`, `USER_DB_NAME`, `PROJECT_DB_NAME`, `TASK_DB_NAME`, `SPRINT_DB_NAME`, `NOTIFICATION_DB_NAME`, `JWT_SECRET` (at least 32 characters), `RABBITMQ_DEFAULT_USER`, `RABBITMQ_DEFAULT_PASS`, `SPRING_PROFILES_ACTIVE`, `GF_AUTH_ANONYMOUS_ENABLED`, `GF_AUTH_ANONYMOUS_ORG_ROLE`.

| Component | Port |
|---|---|
| api-gateway | 8765 |
| user / project / task / sprint / notification service | 5001 / 5002 / 5003 / 5004 / 5005 |
| dashboard-service | 8086 |
| PostgreSQL / Redis | 5432 / 6379 |
| RabbitMQ (AMQP / UI) | 5672 / 15672 |
| Prometheus / Alertmanager | 9090 / 9093 |
| Grafana / Loki | 3000 / 3100 |

### Option 2: Kubernetes (kind)

```bash
cd Backend
cp k8s/secret.example.yaml k8s/secret.yaml    # fill in DB_PASSWORD, JWT_SECRET, ...
./k8s/build-and-deploy.sh kind                # build images, load into kind, apply manifests
kubectl port-forward -n taskmgmt svc/api-gateway 8765:80
```

Full guide (cluster creation, ingress, HPA, debugging, cleanup): [Backend/k8s/README.md](Backend/k8s/README.md).

### Option 3: Run services one by one with Maven

PostgreSQL, Redis and RabbitMQ must already be running (e.g. `docker compose up -d postgres redis rabbitmq`). The default profile is `local`; point service URLs at localhost with environment variables:

```bash
cd Backend
export JWT_SECRET=<string-of-at-least-32-characters>
export USER_SERVICE_URL=http://localhost:5001 PROJECT_SERVICE_URL=http://localhost:5002 \
       TASK_SERVICE_URL=http://localhost:5003 SPRINT_SERVICE_URL=http://localhost:5004 \
       NOTIFICATION_SERVICE_URL=http://localhost:5005 DASHBOARD_SERVICE_URL=http://localhost:8086
export REDIS_HOST=localhost

./mvnw -pl user-service -am spring-boot:run    # repeat for each service, one terminal each
```

Feign clients and the dashboard use `SERVICES_<NAME>_SERVICE_URL` (e.g. `SERVICES_TASK_SERVICE_URL=http://localhost:5003`); the default is the Kubernetes DNS name.

### Frontend

```bash
cd Frontend
npm install
npm start          # http://localhost:4200
```

The gateway allows CORS from `http://localhost:3000` and `http://localhost:4200` (override with `CORS_ALLOWED_ORIGINS`).

## API

Every request goes through the gateway: `http://localhost:8765/{service-name}/{endpoint}`, for example:

```
http://localhost:8765/user-service/...
http://localhost:8765/project-service/...
http://localhost:8765/task-service/...
http://localhost:8765/sprint-service/...
http://localhost:8765/notification-service/...
http://localhost:8765/dashboard-service/dashboard
```

Endpoints and sample requests are in the [postman/](postman) folder (one collection per service plus a `dev` environment).

### Response format (JSend)

```json
{ "status": "success", "data": { "...": "..." } }
```

```json
{ "status": "error", "message": "Error description", "data": null }
```

Common status codes: `200`, `201`, `400`, `401`, `403`, `404`, `409` (resource conflict), `429` (rate limit exceeded), `500`.

## Testing

```bash
cd Backend
./mvnw test                          # everything
./mvnw -pl task-service -am test     # a single service
```

Existing tests (JUnit 5 + Mockito + MockMvc) live in `user-service`, `project-service` and `task-service`: service layer (`*ServiceImplTest`), JWT (`JwtServiceTest`) and controllers (`*ControllerTest`). `api-gateway`, `sprint-service` and `notification-service` only have application-startup tests, and `dashboard-service` has none; CI skips `*ApplicationTests`.

k6 load tests (gateway rate limiter, task creation, task distribution): see [Backend/load-tests/README.md](Backend/load-tests/README.md). Large datasets can be generated with [Backend/fake-data/generate_fake_data.py](Backend/fake-data/generate_fake_data.py).

## CI/CD

- **CI** ([.github/workflows/ci.yml](.github/workflows/ci.yml)): runs `./mvnw verify` (Java 21) on pushes to `master` and on PRs that touch `Backend/**` or the workflows; on pushes to `master` it also builds each service image and pushes it to GHCR (`ghcr.io/<owner>/taskmgmt-<service>`, tagged with the first 8 characters of the commit SHA and `latest`).
- **CD** ([.github/workflows/cd.yml](.github/workflows/cd.yml)): runs after CI succeeds on `master` (or manually). A self-hosted runner on the k3s VM validates the GitHub secrets, applies the manifests, rolls out the images for that SHA, then deploys observability.
- Required repository secrets: `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `SPRING_RABBITMQ_USERNAME`, `SPRING_RABBITMQ_PASSWORD`.

## Monitoring

Each service exposes Actuator on a dedicated port `9090` (not routed through the Service/Ingress) and Prometheus scrapes every pod. Included:

- A Grafana RED & JVM dashboard (`Backend/observability/grafana/dashboards/`).
- Recording rules and alerts based on a 99.5% SLO (`TargetDown`, fast/slow error-budget burn, `HighLatencyP95`, `JvmHeapHigh`, `DbConnectionPoolSaturated`, ...).
- Kubernetes: `./k8s/deploy-observability.sh`; compose: `docker compose up -d prometheus alertmanager grafana`.

Details are in section 12 of [Backend/k8s/README.md](Backend/k8s/README.md).

## Operational notes

- On Kubernetes, `api-gateway`, `user`, `project` and `task` services have an HPA (2–4 pods); `sprint`, `notification` and `dashboard` default to `replicas: 0` to save RAM — enable them with `kubectl scale`.
- The schema is managed by Hibernate (`ddl-auto=update`); there is no migration tool yet.
- Never commit `Backend/k8s/secret.yaml` or `Backend/.env`.
