# Task Management API

Simple Spring Boot CRUD for tasks and users.

## Endpoints

Base URL uses config from `application.properties`:
- Context path: `/api/v1`
- Port: `5000`

Task endpoints:
- `POST /api/v1/tasks`
- `GET /api/v1/tasks`
- `GET /api/v1/tasks/{id}`
- `PUT /api/v1/tasks/{id}`
- `DELETE /api/v1/tasks/{id}`

User endpoints:
- `POST /api/v1/users`
- `GET /api/v1/users`
- `GET /api/v1/users/{id}`
- `PUT /api/v1/users/{id}`
- `DELETE /api/v1/users/{id}`

### Sample Create Payload

```json
{
  "title": "Implement auth",
  "description": "Add JWT auth flow",
  "status": "TODO",
  "dueDate": "2026-03-31"
}
```

### Sample Create User Payload

```json
{
  "fullName": "Son Hoang",
  "email": "son@example.com",
  "password": "secret123",
  "avatarUrl": "https://cdn.example.com/avatar.png"
}
```

## Run tests

```bash
./mvnw test
```

## Run app

```bash
./mvnw spring-boot:run
```

