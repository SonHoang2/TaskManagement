# Frontend Development Plan for Task Management API

Complete Angular frontend implementation for the Task Management microservices backend using custom SCSS styling and Kanban board task view.

## Backend API Overview

The backend consists of these microservices (all accessible via API Gateway at port 8765):

- **User Service** (port 5001): Authentication (login/signup/logout), user management
- **Project Service** (port 5002): Projects, members, invitations, labels
- **Task Service** (port 5003): Tasks, comments, attachments, history, labels
- **Sprint Service** (port 5004): Sprints, task-sprint relationships
- **Notification Service** (port 5005): Event-driven notifications
- **Dashboard Service**: Dashboard statistics and metrics

All APIs use JSend response format and require JWT authentication (via X-User-Id header after gateway decodes token).

## Current Frontend State

- Angular 21.2.0 project with basic setup
- No components, services, or routing configured yet
- Empty routes configuration
- Only core Angular dependencies installed

## Implementation Steps

### Phase 1: Foundation & Configuration
1. **Project Setup**
   - Install required dependencies: `@angular/common/http`, `@angular/forms`, `dragula` (for drag-and-drop)
   - Configure environment files (`environment.ts`, `environment.prod.ts`) with API Gateway URL (http://localhost:8765)
   - Set up SCSS folder structure for global styles, variables, and component-specific styles

2. **Core Services & Interceptors**
   - Create `HttpService` with base configuration for API calls
   - Create `JwtInterceptor` to automatically attach JWT token to requests
   - Create `AuthInterceptor` to handle 401 errors and redirect to login
   - Create error handling service for consistent error display

3. **Authentication System**
   - Create `AuthService` for login/signup/logout and JWT token management
   - Create `UserService` for user profile management
   - Create `AuthGuard` for route protection
   - Build Login component with form validation
   - Build Signup component with form validation
   - Configure routing for auth pages

### Phase 2: Layout & Navigation
4. **Main Layout**
   - Create `LayoutComponent` with sidebar navigation and header
   - Create `SidebarComponent` with navigation links
   - Create `HeaderComponent` with user info and logout
   - Create global SCSS variables for colors, spacing, typography
   - Implement responsive design for mobile/desktop

### Phase 3: Project Management
5. **Project Services**
   - Create `ProjectService` with all CRUD operations
   - Create DTOs/interfaces for Project, ProjectMember, ProjectInvitation
   - Implement pagination and search filtering

6. **Project Components**
   - Create `ProjectListComponent` with search, filter, and pagination
   - Create `ProjectDetailComponent` with project info
   - Create `ProjectFormComponent` for create/edit projects
   - Create `ProjectMembersComponent` for member management
   - Create `ProjectInvitationsComponent` for invitation handling
   - Create `MyInvitationsComponent` for accepting/rejecting invites

### Phase 4: Task Management (Kanban Board)
7. **Task Services**
   - Create `TaskService` with all CRUD operations
   - Create `TaskCommentService` for comment management
   - Create `TaskAttachmentService` for file uploads
   - Create `TaskHistoryService` for change tracking
   - Create DTOs/interfaces for Task, Comment, Attachment, History

8. **Task Components**
   - Create `KanbanBoardComponent` with drag-and-drop columns (To Do, In Progress, Done)
   - Create `TaskCardComponent` for individual task display
   - Create `TaskDetailComponent` with full task info, comments, attachments, history
   - Create `TaskFormComponent` for create/edit tasks
   - Create `TaskCommentsComponent` for comment threads
   - Create `TaskAttachmentsComponent` for file management
   - Integrate dragula for drag-and-drop functionality

### Phase 5: Sprint Management
9. **Sprint Services**
   - Create `SprintService` with all CRUD operations
   - Create `TaskSprintService` for task-sprint relationships
   - Create DTOs/interfaces for Sprint

10. **Sprint Components**
    - Create `SprintListComponent` with project filtering
    - Create `SprintDetailComponent` with sprint info and task assignments
    - Create `SprintFormComponent` for create/edit sprints
    - Add sprint filtering to Kanban board

### Phase 6: Dashboard
11. **Dashboard Services**
    - Create `DashboardService` for statistics and metrics
    - Create DTOs/interfaces for DashboardStats, ProjectSummary, TaskDistribution

12. **Dashboard Components**
    - Create `DashboardComponent` with overview statistics
    - Create `ProjectSummaryComponent` for project cards
    - Create `TaskStatsComponent` for task distribution charts
    - Implement simple chart visualization (using CSS or basic SVG)

### Phase 7: Notifications
13. **Notification Services**
    - Create `NotificationService` for fetching notifications
    - Create DTOs/interfaces for Notification

14. **Notification Components**
    - Create `NotificationListComponent` for notification list
    - Create `NotificationBadgeComponent` for header notification indicator
    - Implement mark as read/delete functionality

### Phase 8: Labels & Tags
15. **Label Services**
    - Create `LabelService` for project labels
    - Create `TaskLabelService` for task-label relationships

16. **Label Components**
    - Create `LabelManagerComponent` for managing project labels
    - Integrate labels into task cards and task forms
    - Add label filtering to task views

### Phase 9: Routing & Navigation
17. **Route Configuration**
    - Set up complete routing structure
    - Implement lazy loading for feature modules
    - Configure route guards for protected pages
    - Add breadcrumb navigation

### Phase 10: Styling & Polish
18. **SCSS Styling**
    - Create global SCSS variables (colors, spacing, typography)
    - Implement component-specific SCSS files
    - Add responsive breakpoints
    - Create utility classes for common patterns
    - Add animations and transitions

19. **Error Handling & Loading States**
    - Create global error component
    - Add loading spinners/skeletons
    - Implement toast notifications for success/error messages
    - Add form validation styling

## Files to Create/Modify

### New Files to Create
- `src/environments/environment.ts` - API configuration
- `src/environments/environment.prod.ts` - Production API config
- `src/app/core/` - Core services and interceptors
- `src/app/features/auth/` - Authentication module
- `src/app/features/projects/` - Project management module
- `src/app/features/tasks/` - Task management module
- `src/app/features/sprints/` - Sprint management module
- `src/app/features/dashboard/` - Dashboard module
- `src/app/features/notifications/` - Notification module
- `src/app/shared/` - Shared components and utilities
- `src/styles/` - Global SCSS files
- `src/app/app.routes.ts` - Update with all routes

### Files to Modify
- `src/app/app.config.ts` - Add HTTP client and providers
- `package.json` - Add dependencies (dragula, etc.)
- `angular.json` - Configure SCSS paths if needed

## Verification Plan

### Testing Strategy
- Manual testing of each feature as it's implemented
- Test authentication flow (login → protected route → logout)
- Test CRUD operations for projects, tasks, sprints
- Test drag-and-drop functionality in Kanban board
- Test error handling (invalid JWT, network errors)
- Cross-browser testing (Chrome, Firefox, Safari)
- Responsive design testing (mobile, tablet, desktop)

### Build & Validation Commands
```bash
# Install dependencies
npm install

# Start development server
ng serve

# Build for production
ng build --configuration production

# Run tests (if implemented)
ng test

# Lint code
ng lint
```

## Risks & Considerations

### Technical Risks
- **Drag-and-drop complexity**: Implementing smooth Kanban drag-and-drop may require additional libraries or custom solutions
- **JWT token refresh**: Need to handle token expiration and refresh logic
- **File uploads**: Task attachments need proper file upload handling
- **Real-time updates**: Notifications may need WebSocket integration for real-time updates
- **State management**: Complex application state may benefit from NgRx or similar

### Design Considerations
- **Custom SCSS**: Building all UI from scratch will take more time than using a UI library
- **Responsive design**: Need to ensure Kanban board works well on mobile devices
- **Performance**: Large task lists may need virtual scrolling
- **Accessibility**: Ensure custom components meet accessibility standards

### Integration Considerations
- **API Gateway**: All requests go through gateway at port 8765
- **CORS**: May need to configure CORS in backend for local development
- **Error handling**: Backend uses JSend format - need consistent error parsing
- **X-User-Id header**: Gateway decodes JWT and adds this header automatically

## User Requirements

Based on user responses:
- **Styling**: Custom SCSS (no UI framework library)
- **Task View**: Kanban Board with drag-and-drop
- **Scope**: Full feature set (all features in one implementation)
