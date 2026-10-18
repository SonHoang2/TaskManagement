# Task Management Platform

**Tiếng Việt** | [English](README.md)

Nền tảng quản lý công việc theo kiến trúc microservices: Spring Boot 3 / Java 21 ở backend, Angular ở frontend, triển khai trên Kubernetes (kind / k3s) với CI/CD bằng GitHub Actions và giám sát bằng Prometheus + Grafana.

## Tính năng

- **Người dùng**: đăng ký, đăng nhập, JWT, phân quyền theo vai trò.
- **Dự án**: CRUD dự án, mời thành viên, quản lý label.
- **Công việc**: CRUD task, bình luận, đính kèm file, lịch sử thay đổi, gắn label.
- **Sprint**: tạo sprint, gán task vào sprint, theo dõi trạng thái.
- **Thông báo**: bất đồng bộ qua RabbitMQ (task được giao, bình luận mới, lời mời vào dự án).
- **Dashboard**: tổng hợp số liệu từ project / task / user / sprint service.
- **Rate limiting**: ở gateway (Redis) và theo method bằng annotation `@RateLimit` (`common-lib`).

## Kiến trúc

```
Angular (Frontend)
      │
      ▼
 API Gateway :8765  ── JWT + rate limit (Redis) ──►  /{service}/** (StripPrefix=1)
      │
      ├── user-service          :5001   xác thực, người dùng
      ├── project-service       :5002   dự án, thành viên, label
      ├── task-service          :5003   task, comment, attachment, history
      ├── sprint-service        :5004   sprint
      ├── notification-service  :5005   thông báo (consumer RabbitMQ)
      └── dashboard-service     :8086   tổng hợp số liệu (gọi các service khác)

Hạ tầng: PostgreSQL (mỗi service một database) · Redis · RabbitMQ
Giám sát: Prometheus · Alertmanager · Grafana (· Loki + Promtail trong docker compose)
```

- Service discovery dùng **Kubernetes Service DNS** (ví dụ `http://user-service`), không có Eureka.
- Gateway giải mã JWT rồi chuyển thông tin người dùng xuống service phía sau qua header.
- Service gọi nhau bằng OpenFeign / WebClient; sự kiện đi qua RabbitMQ.
- Mã dùng chung (JSend, `PageResponse`, exception handler, rate limit) nằm trong module `common-lib`.

## Cấu trúc thư mục

```
.
├── Backend/
│   ├── pom.xml                  # parent Maven, các module bên dưới
│   ├── common-lib/              # DTO, exception, rate limit dùng chung
│   ├── api-gateway/
│   ├── user-service/ project-service/ task-service/
│   ├── sprint-service/ notification-service/ dashboard-service/
│   ├── docker-compose.yml       # chạy toàn bộ stack local
│   ├── init-db.sql              # tạo các database
│   ├── k8s/                     # manifest + script triển khai (xem k8s/README.vi.md)
│   ├── observability/           # Prometheus rules, Alertmanager, dashboard Grafana
│   ├── load-tests/              # k6 (xem load-tests/README.md)
│   └── fake-data/               # sinh dữ liệu giả quy mô lớn
├── Frontend/                    # Angular 21
├── postman/                     # collection + environment Postman
└── .github/workflows/           # ci.yml, cd.yml
```

## Công nghệ

| Nhóm | Công nghệ |
|---|---|
| Backend | Java 21, Spring Boot 3.4.3, Spring Cloud (Gateway, OpenFeign), Spring Data JPA, Spring Security, Spring AMQP, Actuator + Micrometer |
| Frontend | Angular 21 |
| Dữ liệu / messaging | PostgreSQL, Redis, RabbitMQ |
| Giám sát | Prometheus, Alertmanager, Grafana, Loki + Promtail (compose) |
| Build / chạy | Maven (wrapper), Docker, Docker Compose, Kubernetes (kind, k3s) |
| CI/CD | GitHub Actions, GitHub Container Registry (GHCR) |
| Test | JUnit 5, Mockito, MockMvc, k6 (load test) |

## Bắt đầu nhanh

Yêu cầu: Java 21, Docker (kèm Compose). Với frontend cần thêm Node.js.

### Cách 1: Docker Compose (nhanh nhất)

```bash
cd Backend
# tạo file .env (xem danh sách biến bên dưới), sau đó:
docker compose up -d --build
```

Biến cần có trong `Backend/.env`: `POSTGRES_USER`, `POSTGRES_PASSWORD`, `POSTGRES_DB`, `DB_USERNAME`, `DB_PASSWORD`, `USER_DB_NAME`, `PROJECT_DB_NAME`, `TASK_DB_NAME`, `SPRINT_DB_NAME`, `NOTIFICATION_DB_NAME`, `JWT_SECRET` (tối thiểu 32 ký tự), `RABBITMQ_DEFAULT_USER`, `RABBITMQ_DEFAULT_PASS`, `SPRING_PROFILES_ACTIVE`, `GF_AUTH_ANONYMOUS_ENABLED`, `GF_AUTH_ANONYMOUS_ORG_ROLE`.

| Thành phần | Cổng |
|---|---|
| api-gateway | 8765 |
| user / project / task / sprint / notification service | 5001 / 5002 / 5003 / 5004 / 5005 |
| dashboard-service | 8086 |
| PostgreSQL / Redis | 5432 / 6379 |
| RabbitMQ (AMQP / UI) | 5672 / 15672 |
| Prometheus / Alertmanager | 9090 / 9093 |
| Grafana / Loki | 3000 / 3100 |

### Cách 2: Kubernetes (kind)

```bash
cd Backend
cp k8s/secret.example.yaml k8s/secret.yaml    # điền DB_PASSWORD, JWT_SECRET, ...
./k8s/build-and-deploy.sh kind                # build image, nạp vào kind, apply manifest
kubectl port-forward -n taskmgmt svc/api-gateway 8765:80
```

Hướng dẫn đầy đủ (tạo cluster, ingress, HPA, debug, dọn dẹp): [Backend/k8s/README.vi.md](Backend/k8s/README.vi.md).

### Cách 3: Chạy từng service bằng Maven

Cần PostgreSQL, Redis, RabbitMQ chạy sẵn (có thể dùng `docker compose up -d postgres redis rabbitmq`). Profile mặc định là `local`; trỏ URL các service về localhost bằng biến môi trường:

```bash
cd Backend
export JWT_SECRET=<chuoi-toi-thieu-32-ky-tu>
export USER_SERVICE_URL=http://localhost:5001 PROJECT_SERVICE_URL=http://localhost:5002 \
       TASK_SERVICE_URL=http://localhost:5003 SPRINT_SERVICE_URL=http://localhost:5004 \
       NOTIFICATION_SERVICE_URL=http://localhost:5005 DASHBOARD_SERVICE_URL=http://localhost:8086
export REDIS_HOST=localhost

./mvnw -pl user-service -am spring-boot:run    # lặp lại cho từng service, mỗi lần một terminal
```

Feign / dashboard dùng `SERVICES_<TÊN>_SERVICE_URL` (ví dụ `SERVICES_TASK_SERVICE_URL=http://localhost:5003`); mặc định là DNS của k8s.

### Frontend

```bash
cd Frontend
npm install
npm start          # http://localhost:4200
```

Gateway cho phép CORS từ `http://localhost:3000` và `http://localhost:4200` (đổi bằng `CORS_ALLOWED_ORIGINS`).

## API

Mọi request đi qua gateway: `http://localhost:8765/{tên-service}/{endpoint}`, ví dụ:

```
http://localhost:8765/user-service/...
http://localhost:8765/project-service/...
http://localhost:8765/task-service/...
http://localhost:8765/sprint-service/...
http://localhost:8765/notification-service/...
http://localhost:8765/dashboard-service/dashboard
```

Danh sách endpoint và request mẫu nằm trong thư mục [postman/](postman) (collection theo từng service + environment `dev`).

### Định dạng phản hồi (JSend)

```json
{ "status": "success", "data": { "...": "..." } }
```

```json
{ "status": "error", "message": "Mô tả lỗi", "data": null }
```

Mã trạng thái thường gặp: `200`, `201`, `400`, `401`, `403`, `404`, `409` (xung đột tài nguyên), `429` (vượt giới hạn rate limit), `500`.

## Test

```bash
cd Backend
./mvnw test                          # toàn bộ
./mvnw -pl task-service -am test     # một service
```

Các test hiện có (JUnit 5 + Mockito + MockMvc) nằm ở `user-service`, `project-service` và `task-service`: test tầng service (`*ServiceImplTest`), JWT (`JwtServiceTest`) và controller (`*ControllerTest`). `api-gateway`, `sprint-service`, `notification-service` chỉ có test khởi động ứng dụng, `dashboard-service` chưa có test; CI bỏ qua `*ApplicationTests`.

Load test bằng k6 (rate limiter của gateway, tạo task, phân bố task): xem [Backend/load-tests/README.md](Backend/load-tests/README.md). Có thể sinh dữ liệu lớn bằng [Backend/fake-data/generate_fake_data.py](Backend/fake-data/generate_fake_data.py).

## CI/CD

- **CI** ([.github/workflows/ci.yml](.github/workflows/ci.yml)): chạy `./mvnw verify` (Java 21) cho push vào `master` và PR có thay đổi trong `Backend/**` hoặc workflow; khi push vào `master` thì build image của từng service và đẩy lên GHCR (`ghcr.io/<owner>/taskmgmt-<service>`, tag là 8 ký tự đầu của commit SHA và `latest`).
- **CD** ([.github/workflows/cd.yml](.github/workflows/cd.yml)): chạy sau khi CI trên `master` thành công (hoặc kích hoạt tay). Self-hosted runner trên VM k3s kiểm tra GitHub secrets, apply manifest, rollout image theo SHA, rồi triển khai observability.
- Secrets cần có trong repo: `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `SPRING_RABBITMQ_USERNAME`, `SPRING_RABBITMQ_PASSWORD`.

## Giám sát

Mỗi service mở Actuator ở cổng riêng `9090` (không đi qua Service/Ingress), Prometheus scrape từng pod. Có sẵn:

- Dashboard Grafana RED & JVM (`Backend/observability/grafana/dashboards/`).
- Recording rule và alert theo SLO 99.5% (`TargetDown`, error-budget burn nhanh/chậm, `HighLatencyP95`, `JvmHeapHigh`, `DbConnectionPoolSaturated`, ...).
- Kubernetes: `./k8s/deploy-observability.sh`; compose: `docker compose up -d prometheus alertmanager grafana`.

Chi tiết ở mục 12 của [Backend/k8s/README.vi.md](Backend/k8s/README.vi.md).

## Ghi chú vận hành

- Trên k8s, `api-gateway`, `user`, `project`, `task` service có HPA (2–4 pod); `sprint`, `notification`, `dashboard` mặc định `replicas: 0` để tiết kiệm RAM, bật bằng `kubectl scale`.
- Schema được Hibernate tự cập nhật (`ddl-auto=update`); chưa dùng công cụ migration.
- Không commit `Backend/k8s/secret.yaml` và `Backend/.env`.
