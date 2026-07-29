package com.sonhoang2.task_service.task;

import com.sonhoang2.task_service.common.dto.JSendResponse;
import com.sonhoang2.task_service.common.dto.PageResponse;
import com.sonhoang2.task_service.common.dto.ProjectResponse;
import com.sonhoang2.task_service.common.exception.ResourceNotFoundException;
import com.sonhoang2.task_service.feign.ProjectServiceClient;
import com.sonhoang2.task_service.task.dto.TaskCreateRequest;
import com.sonhoang2.task_service.task.dto.TaskResponse;
import com.sonhoang2.task_service.task.dto.TaskUpdateRequest;
import com.sonhoang2.task_service.task.entity.Task;
import com.sonhoang2.task_service.task.entity.TaskPriority;
import com.sonhoang2.task_service.task.entity.TaskStatus;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceImplTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private ProjectServiceClient projectServiceClient;

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

//    @Test
//    void create_ShouldReturnTaskResponse_WhenProjectExists() {
//        // Tạo mock response sử dụng static method success()
//        Map<String, ProjectResponse> data = Map.of("project", new ProjectResponse());
//        JSendResponse<Map<String, ProjectResponse>> mockResponse = JSendResponse.success(data);
//
//        when(projectServiceClient.findById(projectId, userId)).thenReturn(mockResponse);
//        when(modelMapper.map(createRequest, Task.class)).thenReturn(task);
//        when(taskRepository.save(any(Task.class))).thenReturn(task);
//
//        TaskResponse result = taskService.create(createRequest, userId);
//
//        assertNotNull(result);
//        assertEquals(taskId, result.getId());
//        assertEquals("Test Task", result.getTitle());
//        verify(projectServiceClient).findById(projectId, userId);
//        verify(taskRepository).save(any(Task.class));
//    }

    @Test
    void create_ShouldThrowResourceNotFoundException_WhenProjectNotFound() {
        when(projectServiceClient.findById(projectId, userId)).thenThrow(FeignException.NotFound.class);

        assertThrows(ResourceNotFoundException.class, () -> taskService.create(createRequest, userId));

        verify(projectServiceClient).findById(projectId, userId);
        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void findAll_WithoutFilters_ShouldReturnAllTasks() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Task> taskPage = new PageImpl<>(java.util.List.of(task));
        when(taskRepository.findAll(pageable)).thenReturn(taskPage);

        PageResponse<TaskResponse> result = taskService.findAll(null, null, pageable);

        assertNotNull(result);
        assertEquals(1, result.content().size());
        verify(taskRepository).findAll(pageable);
    }

    @Test
    void findAll_WithStatusFilter_ShouldReturnFilteredTasks() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Task> taskPage = new PageImpl<>(java.util.List.of(task));
        when(taskRepository.findByStatus(TaskStatus.TODO, pageable)).thenReturn(taskPage);

        PageResponse<TaskResponse> result = taskService.findAll("TODO", null, pageable);

        assertNotNull(result);
        assertEquals(1, result.content().size());
        verify(taskRepository).findByStatus(TaskStatus.TODO, pageable);
    }

    @Test
    void findAll_WithKeywordFilter_ShouldReturnFilteredTasks() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Task> taskPage = new PageImpl<>(java.util.List.of(task));
        when(taskRepository.findByTitleContainingIgnoreCase("Test", pageable)).thenReturn(taskPage);

        PageResponse<TaskResponse> result = taskService.findAll(null, "Test", pageable);

        assertNotNull(result);
        assertEquals(1, result.content().size());
        verify(taskRepository).findByTitleContainingIgnoreCase("Test", pageable);
    }

    @Test
    void findAll_WithStatusAndKeywordFilters_ShouldReturnFilteredTasks() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Task> taskPage = new PageImpl<>(java.util.List.of(task));
        when(taskRepository.findByStatusAndTitleContainingIgnoreCase(TaskStatus.TODO, "Test", pageable))
                .thenReturn(taskPage);

        PageResponse<TaskResponse> result = taskService.findAll("TODO", "Test", pageable);

        assertNotNull(result);
        assertEquals(1, result.content().size());
        verify(taskRepository).findByStatusAndTitleContainingIgnoreCase(TaskStatus.TODO, "Test", pageable);
    }

    @Test
    void findById_ShouldReturnTaskResponse_WhenTaskExists() {
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

        TaskResponse result = taskService.findById(taskId);

        assertNotNull(result);
        assertEquals(taskId, result.getId());
        verify(taskRepository).findById(taskId);
    }

    @Test
    void findById_ShouldThrowResourceNotFoundException_WhenTaskNotFound() {
        when(taskRepository.findById(taskId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> taskService.findById(taskId));

        verify(taskRepository).findById(taskId);
    }

    @Test
    void update_ShouldReturnUpdatedTaskResponse_WhenTaskExists() {
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        doNothing().when(modelMapper).map(updateRequest, task);

        TaskResponse result = taskService.update(taskId, updateRequest);

        assertNotNull(result);
        verify(taskRepository).findById(taskId);
        verify(modelMapper).map(updateRequest, task);
    }

    @Test
    void update_ShouldThrowResourceNotFoundException_WhenTaskNotFound() {
        when(taskRepository.findById(taskId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> taskService.update(taskId, updateRequest));

        verify(taskRepository).findById(taskId);
        verify(modelMapper, never()).map(any(), any());
    }

    @Test
    void delete_ShouldDeleteTask_WhenTaskExists() {
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        doNothing().when(taskRepository).delete(task);

        taskService.delete(taskId);

        verify(taskRepository).findById(taskId);
        verify(taskRepository).delete(task);
    }

    @Test
    void delete_ShouldThrowResourceNotFoundException_WhenTaskNotFound() {
        when(taskRepository.findById(taskId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> taskService.delete(taskId));

        verify(taskRepository).findById(taskId);
        verify(taskRepository, never()).delete(any(Task.class));
    }

}
