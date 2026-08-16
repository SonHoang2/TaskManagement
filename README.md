# Task Management Platform API

A comprehensive microservices-based task management platform built with Spring Boot, Java 21, and Spring Cloud. This platform provides a robust solution for managing projects, tasks, sprints, and team collaboration with event-driven notifications and comprehensive logging.

## Overview

The Task Management Platform is designed to help teams organize and track their work efficiently through a distributed microservices architecture. It offers:

- **User Management**: Secure authentication and authorization with JWT tokens
- **Project Organization**: Create and manage projects with team members and labels
- **Task Tracking**: Comprehensive task management with comments, attachments, and change history
- **Sprint Planning**: Organize tasks into sprints for agile development workflows
- **Event-driven Notifications**: Asynchronous notification system using RabbitMQ for reliable message delivery
- **Service Discovery**: Dynamic service registration and discovery with Eureka
- **Centralized Logging**: Log aggregation with Loki and visualization with Grafana
- **API Gateway**: Single entry point with routing and load balancing

## Architecture Overview

This project follows a microservices architecture with the following components:

- **API Gateway** - Single entry point for all client requests with routing and authentication
- **Service Registry** - Eureka server for service discovery and registration
- **User Service** - Authentication, user management, and JWT token generation
- **Project Service** - Project management, member invitations, and label management
- **Task Service** - Task management with comments, attachments, history, and labels
- **Sprint Service** - Sprint management and task-sprint relationship handling
- **Notification Service** - Event-driven user notifications via RabbitMQ

## Features

### User Service
- User registration and authentication
- JWT-based secure authentication
- User profile management
- Role-based access control

### Project Service
- Project CRUD operations
- Project member management and invitations
- Label/tag management for projects
- Project visibility settings

### Task Service
- Task CRUD operations
- Task comments and discussions
- File attachments for tasks
- Task change history tracking
- Task labeling and categorization
- Integration with Sprint Service

### Sprint Service
- Sprint creation and management
- Task-sprint relationship management
- Sprint status tracking

### Notification Service
- Event-driven asynchronous notifications
- RabbitMQ message queue for reliable delivery
- Decoupled notification processing
- User notification preferences

### Infrastructure
- Service discovery with Eureka
- API Gateway for centralized routing
- Centralized logging with Loki
- Log visualization with Grafana
- Docker containerization
- PostgreSQL database for persistence

## Technology Stack

### Backend
- **Java 21** - Modern Java with enhanced features
- **Spring Boot 3.4.3** - Application framework
- **Spring Cloud 2024.0.0** - Microservices support
- **Spring Data JPA** - Database abstraction
- **Spring Security** - Authentication and authorization
- **Spring Cloud Gateway** - API Gateway
- **Spring Cloud Netflix Eureka** - Service discovery
- **Spring Cloud OpenFeign** - Declarative REST client
- **Spring AMQP** - RabbitMQ integration

### Security
- **JWT (jjwt 0.12.7)** - Token-based authentication
- **Spring Security** - Comprehensive security framework

### Database & Messaging
- **PostgreSQL 15** - Relational database
- **RabbitMQ 3.13** - Message broker for async communication

### Logging & Monitoring
- **Loki 2.9.0** - Log aggregation system
- **Promtail 2.9.0** - Log collection agent
- **Grafana 10.4.0** - Visualization and monitoring

### Build & Tools
- **Maven** - Dependency management and build tool
- **Lombok** - Reduce boilerplate code
- **ModelMapper 3.2.1** - Object mapping
- **Docker & Docker Compose** - Containerization

### API Standards
- **JSend** - Consistent JSON response format
- **RESTful API** - Standard HTTP methods and status codes

## Testing

The project includes comprehensive unit tests for core business logic using JUnit 5 and Mockito. Test coverage focuses on service layer implementations and controller endpoints.

### Test Coverage Summary

**User Service** (4 test classes, 22+ test methods)
- `AuthControllerTest` - Authentication endpoints (login, signup, logout)
- `UserServiceImplTest` - User CRUD operations, registration, email validation
- `JwtServiceTest` - JWT token generation and validation
- `UserControllerTest` - User management endpoints

**Task Service** (8 test classes, 50+ test methods)
- `TaskServiceImplTest` - Task CRUD, filtering by status/keyword, Feign integration
- `TaskCommentServiceImplTest` - Comment management with filtering
- `TaskAttachmentServiceImplTest` - File attachment operations
- `TaskHistoryServiceImplTest` - Change history tracking
- `TaskLabelServiceImplTest` - Task-label relationships
- `TaskControllerTest` - Task API endpoints
- `TaskCommentControllerTest` - Comment API endpoints
- `TaskAttachmentControllerTest` - Attachment API endpoints

**Project Service** (3 test classes, 31+ test methods)
- `ProjectServiceImplTest` - Project CRUD, member invitations, access control
- `LabelServiceImplTest` - Label management with pagination
- `LabelControllerTest` - Label API endpoints

### Running Tests

```bash
# Run all tests
./mvnw test

# Run tests for specific service
cd user-service
./mvnw test
```

### Test Framework
- **JUnit 5** - Testing framework
- **Mockito** - Mocking framework
- **Spring Boot Test** - Spring integration testing
- **MockMvc** - Web layer testing
- **H2 Database** - In-memory database for testing

## Services & Ports

| Service | Port | Description |
|---------|------|-------------|
| api-gateway | 8765 | API Gateway for routing requests |
| service-registry | 8761 | Eureka Service Registry |
| user-service | 5001 | User authentication and management |
| project-service | 5002 | Project and label management |
| task-service | 5003 | Task management with comments, attachments, history |
| sprint-service | 5004 | Sprint management |
| notification-service | 5005 | Notification management |
| rabbitmq | 5672, 15672 | Message broker (AMQP + Management UI) |
| postgres | 5432 | PostgreSQL database |
| grafana | 3000 | Monitoring and visualization dashboard |
| loki | 3100 | Log aggregation system |

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
