# Task Management API

A Spring Boot REST API for managing projects, tasks, users, collaboration, and task-related activity.

## Technologies

- Java 21
- Spring Boot 4
- Spring Web
- Spring Data JPA
- Spring Security
- JWT authentication
- PostgreSQL
- Maven
- Lombok
- ModelMapper
- Bean Validation
- JUnit and Mockito

## Key Features

- Built RESTful APIs for task management with CRUD operations for users, projects, tasks, labels, comments, attachments, task history, notifications, task-label mapping, and task-sprint mapping.
- Implemented JWT-based authentication with signup, login, logout endpoint, stateless security, password hashing with BCrypt, and protected API routes.
- Added user management with pagination, keyword search, email normalization, duplicate email prevention, and role support for `USER`, `ADMIN`, and `SUPER_ADMIN`.
- Implemented project collaboration features including project creation, automatic owner membership, project members, invitations, invitation acceptance/rejection, and member access checks.
- Added task management features with status, priority, assignee, reporter, due date, start date, parent task, and subtask support.
- Added filtering and pagination for major resources, including tasks, users, labels, comments, attachments, histories, and notifications.
- Implemented label management and task-label assignment with duplicate relation prevention through database constraints.
- Added task comments, attachment metadata, and task history tracking for collaboration and auditing.
- Added notification management with read/unread state and notification types such as task assigned, comment, task updated, and system notification.
- Designed reusable API response and pagination DTOs for consistent response format.
- Added centralized exception handling for validation errors, not found, unauthorized, forbidden, conflict, and internal server errors.
- Used UUID identifiers and automatic timestamp fields for core entities.
- Added unit tests for authentication, user service, and project service logic.
