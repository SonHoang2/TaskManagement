# Task Management API

Simple Spring Boot CRUD for tasks.

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

### Sample Create Payload

```json
{
  "title": "Implement auth",
  "description": "Add JWT auth flow",
  "status": "TODO",
  "dueDate": "2026-03-31"
}
```

## Run tests

```powershell
.\mvnw.cmd test
```

## Run app

```powershell
.\mvnw.cmd spring-boot:run
```

