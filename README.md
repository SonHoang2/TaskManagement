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
cd task_sprints-service
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
