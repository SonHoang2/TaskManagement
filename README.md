# Task Management Platform API

A microservices-based task management platform built with Spring Boot, Java 21, and Spring Cloud.

## Architecture Overview

This project follows a microservices architecture with the following components:

- **API Gateway** (Port 8765) - Single entry point for all client requests
- **Service Registry** (Port 8761) - Eureka server for service discovery
- **User Service** - Authentication and user management
- **Project Service** - Project management, member invitations, and labels
- **Task Service** - Task management with comments, attachments, history, and labels
- **Sprint Service** - Task-Sprint relationship management
- **Notification Service** - User notifications

## Technology Stack

- **Java 21**
- **Spring Boot 3.x**
- **Spring Cloud** (Gateway, Eureka)
- **Maven** for dependency management
- **JSend** response format for consistent API responses
- **JWT** for authentication

## Services & Ports

| Service | Port | Description |
|---------|------|-------------|
| api-gateway | 8765 | API Gateway for routing requests |
| service-registry | 8761 | Eureka Service Registry |
| user-service | 8081 | User authentication and management |
| project-service | 8082 | Project and label management |
| task-service | 8083 | Task management with comments, attachments, history |
| sprint-service | 8084 | Sprint management |
| notification-service | 8085 | Notification management |

## API Response Format

All APIs return responses in **JSend format**:

```json
{
  "status": "success",
  "data": {
    "resource": { ... }
  }
}
```

## Authentication

The platform uses JWT-based authentication. Include the JWT token in the `Authorization` header:

```
Authorization: Bearer <your-jwt-token>
```

For project-related endpoints, also include the user ID in the `X-User-Id` header:

```
X-User-Id: <user-uuid>
```

## API Endpoints

### Base URL
All requests should go through the API Gateway: `http://localhost:8765`

### Authentication Service (`/auth`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/auth/login` | User login with email and password |
| POST | `/auth/signup` | Register a new user account |
| POST | `/auth/logout` | Logout current user |

**Request/Response Examples:**

```json
// POST /auth/login
{
  "email": "user@example.com",
  "password": "password123"
}

// Response
{
  "status": "success",
  "data": {
    "auth": {
      "accessToken": "<jwt-token-here>",
      "tokenType": "Bearer",
      "expiresInMs": <token-expiration-time>,
      "userId": "<user-uuid>"
    }
  }
}
```

### User Service (`/users`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/users` | Create a new user (admin) |
| GET | `/users` | List all users with pagination |
| GET | `/users/{id}` | Get user by ID |
| PATCH | `/users/{id}` | Update user information |
| DELETE | `/users/{id}` | Delete user |

**Query Parameters:**
- `keyword` (optional): Search users by keyword
- `page`, `size`, `sort`: Pagination parameters

### Project Service (`/projects`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/projects` | Create a new project |
| GET | `/projects` | List all projects |
| POST | `/projects/{projectId}/invites` | Invite a member to a project |
| PATCH | `/projects/invites/{invitationId}` | Accept/decline project invitation |
| GET | `/projects/{projectId}/members` | List project members |

**Request Headers:**
- `X-User-Id`: Required for project operations

**Request Examples:**

```json
// POST /projects
{
  "name": "My Project",
  "description": "Project description"
}

// POST /projects/{projectId}/invites
{
  "email": "member@example.com",
  "role": "MEMBER"
}

// PATCH /projects/invites/{invitationId}
{
  "decision": "ACCEPTED" // or "DECLINED"
}
```

### Label Service (`/labels`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/labels` | Create a new label |
| GET | `/labels` | List labels with pagination |
| GET | `/labels/{id}` | Get label by ID |
| PATCH | `/labels/{id}` | Update label |
| DELETE | `/labels/{id}` | Delete label |

**Query Parameters:**
- `projectId` (optional): Filter by project ID
- `name` (optional): Filter by label name

### Task Service (`/tasks`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/tasks` | Create a new task |
| GET | `/tasks` | List tasks with pagination |
| GET | `/tasks/{id}` | Get task by ID |
| PATCH | `/tasks/{id}` | Update task |
| DELETE | `/tasks/{id}` | Delete task |

**Query Parameters:**
- `status` (optional): Filter by task status
- `keyword` (optional): Search tasks by keyword

### Task Comments (`/task-comments`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/task-comments` | Add a comment to a task |
| GET | `/task-comments` | List comments with pagination |
| GET | `/task-comments/{id}` | Get comment by ID |
| PATCH | `/task-comments/{id}` | Update comment |
| DELETE | `/task-comments/{id}` | Delete comment |

**Query Parameters:**
- `taskId` (optional): Filter by task ID
- `userId` (optional): Filter by user ID

### Task Attachments (`/task-attachments`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/task-attachments` | Upload attachment to a task |
| GET | `/task-attachments` | List attachments with pagination |
| GET | `/task-attachments/{id}` | Get attachment by ID |
| PATCH | `/task-attachments/{id}` | Update attachment |
| DELETE | `/task-attachments/{id}` | Delete attachment |

**Query Parameters:**
- `taskId` (optional): Filter by task ID
- `uploadedBy` (optional): Filter by uploader ID

### Task History (`/task-histories`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/task-histories` | Create a task history entry |
| GET | `/task-histories` | List history entries with pagination |
| GET | `/task-histories/{id}` | Get history entry by ID |
| PATCH | `/task-histories/{id}` | Update history entry |
| DELETE | `/task-histories/{id}` | Delete history entry |

**Query Parameters:**
- `taskId` (optional): Filter by task ID
- `changedBy` (optional): Filter by user who made changes

### Task Labels (`/task-labels`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/task-labels` | Associate a label with a task |
| GET | `/task-labels` | List task-label associations |
| GET | `/task-labels/{taskId}/{labelId}` | Get specific task-label association |
| PATCH | `/task-labels/{taskId}/{labelId}` | Update task-label association |
| DELETE | `/task-labels/{taskId}/{labelId}` | Remove label from task |

**Query Parameters:**
- `taskId` (optional): Filter by task ID
- `labelId` (optional): Filter by label ID

### Sprint Service (`/task-sprints`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/task-sprints` | Associate a task with a sprint |
| GET | `/task-sprints` | List task-sprint associations |
| GET | `/task-sprints/{taskId}/{sprintId}` | Get specific task-sprint association |
| PATCH | `/task-sprints/{taskId}/{sprintId}` | Update task-sprint association |
| DELETE | `/task-sprints/{taskId}/{sprintId}` | Remove task from sprint |

**Query Parameters:**
- `taskId` (optional): Filter by task ID
- `sprintId` (optional): Filter by sprint ID

### Notification Service (`/notifications`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/notifications` | Create a notification |
| GET | `/notifications` | List notifications with pagination |
| GET | `/notifications/{id}` | Get notification by ID |
| PATCH | `/notifications/{id}` | Update notification |
| DELETE | `/notifications/{id}` | Delete notification |

**Query Parameters:**
- `userId` (optional): Filter by user ID
- `isRead` (optional): Filter by read status (true/false)
- `type` (optional): Filter by notification type

## Pagination

All list endpoints support pagination through Spring Data's `Pageable` interface:

**Query Parameters:**
- `page`: Page number (0-indexed)
- `size`: Number of items per page
- `sort`: Sorting criteria (format: `field,direction`)

**Example:**
```
GET /tasks?page=0&size=10&sort=createdAt,desc
```

**Response Format:**
```json
{
  "status": "success",
  "data": {
    "page": {
      "content": [ ... ],
      "totalElements": 100,
      "totalPages": 10,
      "number": 0,
      "size": 10
    }
  }
}
```

## Getting Started

### Prerequisites

- Java 21
- Maven 3.x
- MySQL or PostgreSQL (for each service's database)

### Running the Services

1. **Start Service Registry:**
```bash
cd service-registry
./mvnw spring-boot:run
```

2. **Start API Gateway:**
```bash
cd api-gateway
./mvnw spring-boot:run
```

3. **Start Individual Services** (in separate terminals):
```bash
# User Service
cd user-service
./mvnw spring-boot:run

# Project Service
cd project-service
./mvnw spring-boot:run

# Task Service
cd task-service
./mvnw spring-boot:run

# Sprint Service
cd sprint-service
./mvnw spring-boot:run

# Notification Service
cd notification-service
./mvnw spring-boot:run
```

### Configuration

Each service has its own `application.properties` or `application.yml` file in `src/main/resources/`. Configure:

- Database connection (URL, username, password)
- JWT secret key (for user-service, api-gateway)
- Service registry URL (default: `http://localhost:8761/eureka`)

## Development

### Building All Services
```bash
./mvnw clean install
```

### Running Tests
```bash
./mvnw test
```

### Service Discovery

Once all services are running, you can view the registered services at:
```
http://localhost:8761
```

## API Gateway Routes

The API Gateway automatically creates routes based on service names registered in Eureka. Services are accessible via:

```
http://localhost:8765/{service-name}/{endpoint}
```

For example:
- User Service: `http://localhost:8765/user-service/users`
- Task Service: `http://localhost:8765/task-service/tasks`
- Project Service: `http://localhost:8765/project-service/projects`

## Error Handling

All endpoints return JSend-formatted error responses:

```json
{
  "status": "error",
  "message": "Error description",
  "data": { ... }
}
```

Common HTTP status codes:
- `200 OK` - Successful request
- `201 Created` - Resource created successfully
- `400 Bad Request` - Invalid request data
- `401 Unauthorized` - Authentication required
- `403 Forbidden` - Insufficient permissions
- `404 Not Found` - Resource not found
- `500 Internal Server Error` - Server error

## License

This project is licensed under the MIT License.
