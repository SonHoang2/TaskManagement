## Microservice Migration

Plan này dựa trên sơ đồ `microservice_architecture.svg`. Mục tiêu là tách Spring Boot monolith hiện tại thành nhiều
service độc lập, mỗi service sở hữu database riêng và giao tiếp qua API Gateway, HTTP nội bộ và message broker.

### 1. Hiện trạng project

Project hiện tại đang là một Spring Boot monolith gồm các module chính:

- `auth`: đăng ký, đăng nhập, JWT.
- `user`: quản lý user và role.
- `project`: project, project member, invitation.
- `task`: task chính.
- `label`, `tasklabel`: label và gán label cho task.
- `comment`: comment của task.
- `attachment`: metadata file đính kèm.
- `history`: lịch sử thay đổi task.
- `sprint`: sprint và quan hệ task-sprint.
- `notification`: thông báo.

Trong monolith, nhiều entity đang liên kết trực tiếp bằng JPA relation, ví dụ `Task -> Project`, `Task -> User`,
`Project -> User`, `Sprint -> Project`. Khi migrate sang microservice, các liên kết giữa service phải được thay bằng
UUID và API/event.

### 2. Kiến trúc đích

Kiến trúc đích gồm:

- `api-gateway`: cửa ngõ duy nhất cho client.
- `user-service`: quản lý auth, user, role.
- `project-service`: quản lý project, member, invitation, label.
- `task-service`: quản lý task, comment, attachment, history, task-label.
- `sprint-service`: quản lý sprint và task trong sprint.
- `notification-service`: quản lý notification, email, push, websocket nếu cần.
- `message-broker`: Kafka hoặc RabbitMQ cho async event.
- shared infrastructure: service discovery, centralized config, tracing, logging.

Mỗi service có database riêng:

- `user_db`
- `project_db`
- `task_db`
- `sprint_db`
- `notification_db`

### 3. Chia service boundary

#### User Service

Chứa:

- `auth`
- `user`
- `common/security` liên quan đến JWT

Sở hữu:

- `users`

Trách nhiệm:

- Đăng ký user.
- Đăng nhập và cấp JWT.
- Quản lý role.
- Cung cấp API nội bộ để service khác kiểm tra user có tồn tại hay không.

Lưu ý:

- Chỉ `user-service` được đọc/ghi bảng `users`.
- Service khác chỉ lưu `userId`, không join trực tiếp sang user table.

#### Project Service

Chứa:

- `project`
- `label`

Sở hữu:

- `projects`
- `project_members`
- `project_invitations`
- `labels`

Trách nhiệm:

- Tạo và cập nhật project.
- Quản lý owner, member, invitation.
- Quản lý label theo project.

Lưu ý:

- `ownerId`, `userId`, `inviteeId`, `invitedById` chỉ là UUID tham chiếu sang `user-service`.
- Không dùng foreign key cross-database.

#### Task Service

Chứa:

- `task`
- `comment`
- `attachment`
- `history`
- `tasklabel`

Sở hữu:

- `tasks`
- `task_comments`
- `task_attachments`
- `task_histories`
- `task_labels`

Trách nhiệm:

- CRUD task.
- Gán assignee, reporter.
- Quản lý comment, attachment, history.
- Gán label cho task.

Lưu ý:

- `projectId`, `assigneeId`, `reporterId`, `labelId` chỉ là UUID.
- Khi cần validate project/user/label thì gọi service tương ứng qua HTTP nội bộ.

#### Sprint Service

Chứa:

- `sprint`
- `tasksprint`

Sở hữu:

- `sprints`
- `task_sprints`

Trách nhiệm:

- Tạo và quản lý sprint.
- Thêm/xóa task khỏi sprint.

Lưu ý:

- `projectId` và `taskId` chỉ là UUID.
- Khi thêm task vào sprint, cần validate task qua `task-service`.

#### Notification Service

Chứa:

- `notification`

Sở hữu:

- `notifications`, hoặc Redis/PostgreSQL tùy nhu cầu.

Trách nhiệm:

- Nhận event từ message broker.
- Tạo notification.
- Hỗ trợ unread/read state.
- Mở rộng email, push, websocket nếu cần.

### 4. Tạo cấu trúc repository

Giai đoạn đầu nên dùng monorepo để dễ migrate:

```text
task-management-platform/
  api-gateway/
  user-service/
  project-service/
  task-service/
  sprint-service/
  notification-service/
  common-lib/
  docker-compose.yml
```

Sau khi hệ thống ổn định, có thể tách thành nhiều repository nếu team/deployment cần.

### 5. Tạo API Gateway

API Gateway nhận request từ client và route đến service phù hợp.

Route đề xuất:

```text
/api/v1/auth/**          -> user-service
/api/v1/users/**         -> user-service
/api/v1/projects/**      -> project-service
/api/v1/labels/**        -> project-service
/api/v1/tasks/**         -> task-service
/api/v1/comments/**      -> task-service
/api/v1/attachments/**   -> task-service
/api/v1/histories/**     -> task-service
/api/v1/sprints/**       -> sprint-service
/api/v1/notifications/** -> notification-service
```

Gateway nên xử lý:

- JWT validation.
- Routing.
- CORS.
- Rate limit.
- Request logging.

### 6. Tách database

Hiện tại project dùng một database:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/taskmanagement
```

Sau khi migrate, mỗi service có datasource riêng:

```text
user-service         -> user_db
project-service      -> project_db
task-service         -> task_db
sprint-service       -> sprint_db
notification-service -> notification_db
```

Nguyên tắc:

- Service chỉ đọc/ghi database của chính nó.
- Không tạo foreign key giữa database của các service.
- Không dùng JPA relation tới entity nằm ở service khác.
- Các ID tham chiếu giữa service được lưu bằng UUID.

Ví dụ `Task` sau khi tách không nên giữ `Project project` hay `User assignee`, mà nên giữ:

```java
private UUID projectId;
private UUID assigneeId;
private UUID reporterId;
```

### 7. Thứ tự migrate đề xuất

#### Bước 1: Chuẩn bị contract

- Liệt kê API hiện tại của từng controller.
- Chốt request/response DTO cho từng service.
- Chốt event name và payload.
- Tạo `common-lib` cho response chung, pagination, exception format, event contracts.

#### Bước 2: Tách User Service

- Tạo Spring Boot app `user-service`.
- Di chuyển `auth`, `user`, JWT logic.
- Tạo `user_db`.
- Test lại register/login.
- Gateway route `/auth/**` và `/users/**` sang `user-service`.

Đây là service nên tách đầu tiên vì các service khác phụ thuộc vào user identity.

#### Bước 3: Tạo API Gateway

- Tạo `api-gateway`.
- Cấu hình route đến `user-service`.
- Verify JWT ở gateway.
- Đảm bảo client chỉ gọi qua gateway, không gọi trực tiếp service bên trong.

#### Bước 4: Tách Project Service

- Tạo `project-service`.
- Di chuyển `project`, `label`.
- Tạo `project_db`.
- Thay relation tới `User` bằng UUID.
- Gọi `user-service` khi cần validate user.
- Publish event khi tạo project, mời member, chấp nhận invitation.

Event đề xuất:

```text
ProjectCreated
ProjectMemberAdded
ProjectInvitationCreated
ProjectInvitationAccepted
```

#### Bước 5: Tách Task Service

- Tạo `task-service`.
- Di chuyển `task`, `comment`, `attachment`, `history`, `tasklabel`.
- Tạo `task_db`.
- Thay relation tới `Project`, `User`, `Label` bằng UUID.
- Gọi `project-service` để validate project/label.
- Gọi `user-service` để validate assignee/reporter.
- Publish event khi task thay đổi.

Event đề xuất:

```text
TaskCreated
TaskUpdated
TaskAssigned
TaskStatusChanged
TaskCommentCreated
TaskAttachmentAdded
```

#### Bước 6: Thêm Message Broker

Có thể chọn:

- RabbitMQ: dễ setup, phù hợp giai đoạn đầu.
- Kafka: tốt hơn khi event volume lớn và cần event streaming lâu dài.

Nên bắt đầu với RabbitMQ nếu mục tiêu là migrate nhanh và dễ vận hành.

Broker dùng cho việc async:

- Tạo notification.
- Gửi email.
- Push websocket.
- Ghi audit log.
- Đồng bộ read model nếu sau này cần.

#### Bước 7: Tách Notification Service

- Tạo `notification-service`.
- Di chuyển `notification`.
- Tạo `notification_db`.
- Subscribe event từ broker.
- Tạo notification dựa trên event.

Ví dụ:

```text
TaskAssigned -> tạo notification cho assignee
TaskCommentCreated -> tạo notification cho member liên quan
ProjectInvitationCreated -> tạo notification cho invitee
```

#### Bước 8: Tách Sprint Service

- Tạo `sprint-service`.
- Di chuyển `sprint`, `tasksprint`.
- Tạo `sprint_db`.
- Thay relation tới `Project` và `Task` bằng UUID.
- Gọi `project-service` để validate project.
- Gọi `task-service` để validate task.

Event đề xuất:

```text
SprintCreated
TaskAddedToSprint
TaskRemovedFromSprint
```

#### Bước 9: Thêm shared infrastructure

Sau khi các service chạy được riêng, thêm hạ tầng dùng chung:

- Service Discovery & Load Balancing: Kubernetes Service + DNS (built-in)
- Centralized Config: ConfigMap + Secret
- Distributed Tracing: OpenTelemetry (+ Jaeger/Tempo)
- Centralized Logging: Loki hoặc ELK
- Metrics: Prometheus + Grafana
- (Optional) Service Mesh: Istio

Thứ tự nên làm:

1. Containerize (Docker)
2. Deploy lên Kubernetes
3. Service (discovery + load balancing built-in)
4. ConfigMap / Secret
5. Observability (Prometheus, Grafana, tracing…)
6. (Optional) Service Mesh (Istio)

### 8. Giao tiếp giữa service

#### Sync HTTP/gRPC

Dùng khi cần kết quả ngay:

```text
Task Service -> Project Service: project có tồn tại không?
Task Service -> User Service: assignee có tồn tại không?
Sprint Service -> Task Service: task có tồn tại không?
Project Service -> User Service: invitee có tồn tại không?
```

#### Async Event

Dùng cho side effect không cần trả kết quả ngay:

```text
Task Service -> Broker -> Notification Service
Project Service -> Broker -> Notification Service
Sprint Service -> Broker -> Notification Service
```

Không nên dùng event cho các thao tác bắt buộc phải validate ngay trước khi trả response.

### 9. Những thay đổi code quan trọng

Cần sửa các entity đang có relation cross-domain:

- `Task -> Project`
- `Task -> User`
- `Task -> parentTask`
- `Project -> User`
- `ProjectMember -> User`
- `ProjectInvitation -> User`
- `Sprint -> Project`
- `TaskSprint -> Task`
- `Notification -> User`
- `TaskComment -> User`
- `TaskAttachment -> User`
- `TaskHistory -> User`

Hướng xử lý:

- Relation nội bộ cùng service có thể giữ lại.
- Relation sang service khác phải đổi thành UUID.
- DTO response nếu cần hiển thị tên user/project thì có 2 cách:
    - Gọi service khác để lấy thông tin.
    - Dùng read model/cache nếu cần performance cao.

### 10. Testing sau migration

Cần có test cho các flow chính:

- Register user.
- Login lấy JWT.
- Tạo project.
- Mời member vào project.
- Tạo task trong project.
- Assign task cho user.
- Comment task.
- Tạo notification từ event.
- Tạo sprint.
- Thêm task vào sprint.

Nên có:

- Unit test cho từng service.
- Integration test với database riêng.
- Contract test giữa các service.
- End-to-end test đi qua API Gateway.

### 11. Definition of Done

Migration được xem là hoàn thành khi:

- Mỗi service chạy độc lập.
- Mỗi service có database riêng.
- Client chỉ gọi API Gateway.
- JWT hoạt động qua gateway.
- Không service nào query database của service khác.
- Notification được tạo qua message broker.
- Docker Compose chạy được toàn bộ hệ thống.
- Các flow chính có test.
- Logs và tracing đủ giúp debug request qua nhiều service.

### 12. Thứ tự ưu tiên ngắn gọn

Nếu cần làm nhanh, thứ tự ưu tiên là:

1. Tách `user-service`.
2. Dựng `api-gateway`.
3. Tách `project-service`.
4. Tách `task-service`.
5. Thêm message broker.
6. Tách `notification-service`.
7. Tách `sprint-service`.
8. Tách database hoàn toàn và bỏ relation cross-service.
9. Thêm Docker Compose, tracing, logging, metrics.
