---
paths:
  - "Backend/k8s/**"
  - "Backend/docker-compose*.yml"
  - "Backend/**/Dockerfile"
  - "Backend/observability/**"
  - ".github/workflows/**"
---

# Infra / deploy rules

- When you add a service, register it everywhere: `services:` in `Backend/k8s/helm/taskmgmt/values.yaml` (with `port`, `db`, `rabbit`, `jwt`, `hpa` and `replicas` flags), `Backend/docker-compose.yml`, the `build-push` matrix in `.github/workflows/ci.yml`, the gateway routes in `api-gateway/src/main/resources/application.properties`, a database in `Backend/init-db.sql`, and `ServiceName` in `Frontend/src/app/core/http/api-client.ts`.
- Non-secret shared config goes in `config:` in `values.yaml` (rendered into ConfigMap `app-config`; values are always strings) and in `docker-compose.yml` for local runs.
- Services that depend on `common-lib` build with `Backend/` as the Docker context. Standalone ones (api-gateway, dashboard-service) use their own folder.
- Secrets are only ever referenced, never inlined. The chart reads Secret `app-secrets`, which it doesn't create: `k8s/secret.yaml` creates it locally (template `secret.example.yaml`), and in CD it comes from GitHub secrets. A new secret must be added to `secret.example.yaml`, the env/validation step in `cd.yml`, and the README list.
- The chart's `image.*` default (`taskmgmt/<svc>:latest`) is for kind. CD overrides it with `--set image.registry=ghcr.io/<owner> image.prefix=taskmgmt- image.tag=<sha8>`. Keep both paths working.
- Actuator listens on port 9090 and is never exposed through the Service/Ingress. Prometheus scrapes pods directly.
- Alerts and recording rules in `observability/prometheus/rules` follow the 99.5% SLO. Use the existing recording rules instead of raw histograms in new alerts.
