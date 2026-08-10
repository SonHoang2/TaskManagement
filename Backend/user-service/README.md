# User Service

Standalone user-service for Task Management platform.

## Features

- Auth: signup, login (JWT)
- User CRUD
- Role management

## Quick start

1) Configure database and JWT in `src/main/resources/application.properties`.
2) Run the service:

```bash
./mvnw spring-boot:run
```

## API

- `POST /auth/signup`
- `POST /auth/login`
- `POST /users`
- `GET /users`
- `GET /users/{id}`
- `PATCH /users/{id}`
- `DELETE /users/{id}`

