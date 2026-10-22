package com.sonhoang2.task_service.task;

import com.sonhoang2.task_service.common.dto.PageResponse;
import com.sonhoang2.task_service.common.exception.ResourceNotFoundException;
import com.sonhoang2.task_service.common.security.ProjectAccessGuard;
import com.sonhoang2.task_service.task.dto.TaskCreateRequest;
import com.sonhoang2.task_service.task.dto.TaskResponse;
import com.sonhoang2.task_service.task.dto.TaskUpdateRequest;
import com.sonhoang2.task_service.task.entity.Task;
import com.sonhoang2.task_service.task.entity.TaskPriority;
import com.sonhoang2.task_service.task.entity.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceImplTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private ProjectAccessGuard projectAccessGuard;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private TaskServiceImpl taskService;

    private Task task;
    private TaskCreateRequest createRequest;
    private TaskUpdateRequest updateRequest;
    private UUID taskId;
    private UUID projectId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        taskId = UUID.randomUUID();
        projectId = UUID.randomUUID();
        userId = UUID.randomUUID();

        task = Task.builder()
                .id(taskId)
                .projectId(projectId)
                .title("Test Task")
                .description("Test Description")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.HIGH)
                .assigneeId(UUID.randomUUID())
                .reporterId(UUID.randomUUID())
                .dueDate(Instant.now())
                .startDate(Instant.now())
                .parentTaskId(null)
                .build();

        createRequest = new TaskCreateRequest();
        createRequest.setProjectId(projectId);
        createRequest.setTitle("Test Task");
        createRequest.setDescription("Test Description");
        createRequest.setStatus(TaskStatus.TODO);
        createRequest.setPriority(TaskPriority.HIGH);

        updateRequest = new TaskUpdateRequest();
        updateRequest.setTitle("Updated Task");
        updateRequest.setDescription("Updated Description");
        updateRequest.setStatus(TaskStatus.IN_PROGRESS);
    }

    @Test
    void create_ShouldSaveTaskWithCallerAsReporter_WhenCallerIsMember() {
        when(projectAccessGuard.requireMember(projectId, userId)).thenReturn("MEMBER");
        when(modelMapper.map(createRequest, Task.class)).thenReturn(task);
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskResponse result = taskService.create(createRequest, userId);

        assertEquals(taskId, result.getId());
        assertEquals(userId, result.getReporterId());
        verify(taskRepository).save(task);
    }

    @Test
    void create_ShouldThrowAccessDenied_WhenCallerIsNotMember() {
        when(projectAccessGuard.requireMember(projectId, userId)).thenThrow(new AccessDeniedException("denied"));

        assertThrows(AccessDeniedException.class, () -> taskService.create(createRequest, userId));

        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void findAll_WithoutFilters_ShouldOnlySearchCallersProjects() {
        Pageable pageable = PageRequest.of(0, 10);
        Set<UUID> projectIds = Set.of(projectId);
        when(projectAccessGuard.memberProjectIds(userId)).thenReturn(projectIds);
        when(taskRepository.findByProjectIdIn(projectIds, pageable)).thenReturn(new PageImpl<>(List.of(task)));

        PageResponse<TaskResponse> result = taskService.findAll(null, null, pageable, userId);

        assertEquals(1, result.content().size());
        verify(taskRepository).findByProjectIdIn(projectIds, pageable);
    }

    @Test
    void findAll_WithStatusFilter_ShouldReturnFilteredTasks() {
        Pageable pageable = PageRequest.of(0, 10);
        Set<UUID> projectIds = Set.of(projectId);
        when(projectAccessGuard.memberProjectIds(userId)).thenReturn(projectIds);
        when(taskRepository.findByProjectIdInAndStatus(projectIds, TaskStatus.TODO, pageable))
                .thenReturn(new PageImpl<>(List.of(task)));

        PageResponse<TaskResponse> result = taskService.findAll("TODO", null, pageable, userId);

        assertEquals(1, result.content().size());
    }

    @Test
    void findAll_WithKeywordFilter_ShouldReturnFilteredTasks() {
        Pageable pageable = PageRequest.of(0, 10);
        Set<UUID> projectIds = Set.of(projectId);
        when(projectAccessGuard.memberProjectIds(userId)).thenReturn(projectIds);
        when(taskRepository.findByProjectIdInAndTitleContainingIgnoreCase(projectIds, "Test", pageable))
                .thenReturn(new PageImpl<>(List.of(task)));

        PageResponse<TaskResponse> result = taskService.findAll(null, "Test", pageable, userId);

        assertEquals(1, result.content().size());
    }

    @Test
    void findAll_WithStatusAndKeywordFilters_ShouldReturnFilteredTasks() {
        Pageable pageable = PageRequest.of(0, 10);
        Set<UUID> projectIds = Set.of(projectId);
        when(projectAccessGuard.memberProjectIds(userId)).thenReturn(projectIds);
        when(taskRepository.findByProjectIdInAndStatusAndTitleContainingIgnoreCase(projectIds, TaskStatus.TODO,
                "Test", pageable)).thenReturn(new PageImpl<>(List.of(task)));

        PageResponse<TaskResponse> result = taskService.findAll("TODO", "Test", pageable, userId);

        assertEquals(1, result.content().size());
    }

    @Test
    void findAll_ShouldReturnEmptyPage_WhenCallerHasNoProjects() {
        Pageable pageable = PageRequest.of(0, 10);
        when(projectAccessGuard.memberProjectIds(userId)).thenReturn(Set.of());

        PageResponse<TaskResponse> result = taskService.findAll(null, null, pageable, userId);

        assertTrue(result.content().isEmpty());
        verifyNoInteractions(taskRepository);
    }

    @Test
    void findById_ShouldReturnTaskResponse_WhenCallerIsMember() {
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

        TaskResponse result = taskService.findById(taskId, userId);

        assertEquals(taskId, result.getId());
        verify(projectAccessGuard).requireMember(projectId, userId);
    }

    @Test
    void findById_ShouldThrowAccessDenied_WhenCallerIsNotMember() {
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(projectAccessGuard.requireMember(projectId, userId)).thenThrow(new AccessDeniedException("denied"));

        assertThrows(AccessDeniedException.class, () -> taskService.findById(taskId, userId));
    }

    @Test
    void findById_ShouldThrowResourceNotFoundException_WhenTaskNotFound() {
        when(taskRepository.findById(taskId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> taskService.findById(taskId, userId));
    }

    @Test
    void update_ShouldReturnUpdatedTaskResponse_WhenCallerIsMember() {
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        doNothing().when(modelMapper).map(updateRequest, task);

        TaskResponse result = taskService.update(taskId, updateRequest, userId);

        assertNotNull(result);
        verify(projectAccessGuard).requireMember(projectId, userId);
        verify(modelMapper).map(updateRequest, task);
    }

    @Test
    void update_ShouldThrowAccessDenied_WhenMovingTaskToProjectCallerIsNotIn() {
        UUID otherProjectId = UUID.randomUUID();
        updateRequest.setProjectId(otherProjectId);
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(projectAccessGuard.requireMember(projectId, userId)).thenReturn("MEMBER");
        when(projectAccessGuard.requireMember(otherProjectId, userId)).thenThrow(new AccessDeniedException("denied"));

        assertThrows(AccessDeniedException.class, () -> taskService.update(taskId, updateRequest, userId));

        verify(modelMapper, never()).map(any(), any());
    }

    @Test
    void update_ShouldThrowResourceNotFoundException_WhenTaskNotFound() {
        when(taskRepository.findById(taskId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> taskService.update(taskId, updateRequest, userId));

        verify(modelMapper, never()).map(any(), any());
    }

    @Test
    void delete_ShouldDeleteTask_WhenCallerIsMember() {
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

        taskService.delete(taskId, userId);

        verify(projectAccessGuard).requireMember(projectId, userId);
        verify(taskRepository).delete(task);
    }

    @Test
    void delete_ShouldNotDelete_WhenCallerIsNotMember() {
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(projectAccessGuard.requireMember(projectId, userId)).thenThrow(new AccessDeniedException("denied"));

        assertThrows(AccessDeniedException.class, () -> taskService.delete(taskId, userId));

        verify(taskRepository, never()).delete(any(Task.class));
    }

    @Test
    void delete_ShouldThrowResourceNotFoundException_WhenTaskNotFound() {
        when(taskRepository.findById(taskId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> taskService.delete(taskId, userId));

        verify(taskRepository, never()).delete(any(Task.class));
    }

    @Test
    void getTaskStatsByProjectId_ShouldThrowAccessDenied_WhenCallerIsNotMember() {
        when(projectAccessGuard.requireMember(projectId, userId)).thenThrow(new AccessDeniedException("denied"));

        assertThrows(AccessDeniedException.class, () -> taskService.getTaskStatsByProjectId(projectId, userId));

        verifyNoInteractions(taskRepository);
    }

}
