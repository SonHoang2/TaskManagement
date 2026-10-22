package com.sonhoang2.task_service.tasklabel;

import com.sonhoang2.common.exception.ResourceConflictException;
import com.sonhoang2.task_service.common.dto.JSendResponse;
import com.sonhoang2.task_service.common.dto.LabelSummary;
import com.sonhoang2.task_service.common.dto.PageResponse;
import com.sonhoang2.task_service.common.exception.ResourceNotFoundException;
import com.sonhoang2.task_service.common.security.ProjectAccessGuard;
import com.sonhoang2.task_service.feign.ProjectServiceClient;
import com.sonhoang2.task_service.tasklabel.dto.TaskLabelCreateRequest;
import com.sonhoang2.task_service.tasklabel.dto.TaskLabelResponse;
import com.sonhoang2.task_service.tasklabel.dto.TaskLabelUpdateRequest;
import com.sonhoang2.task_service.tasklabel.entity.TaskLabel;
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
import org.springframework.security.access.AccessDeniedException;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskLabelServiceImplTest {

    @Mock
    private TaskLabelRepository taskLabelRepository;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private ProjectAccessGuard projectAccessGuard;

    @Mock
    private ProjectServiceClient projectServiceClient;

    @InjectMocks
    private TaskLabelServiceImpl taskLabelService;

    private TaskLabel taskLabel;
    private TaskLabelCreateRequest createRequest;
    private TaskLabelUpdateRequest updateRequest;
    private UUID taskId;
    private UUID labelId;
    private UUID userId;
    private UUID projectId;

    @BeforeEach
    void setUp() {
        taskId = UUID.randomUUID();
        labelId = UUID.randomUUID();
        userId = UUID.randomUUID();
        projectId = UUID.randomUUID();

        taskLabel = TaskLabel.builder()
                .id(UUID.randomUUID())
                .taskId(taskId)
                .labelId(labelId)
                .build();

        createRequest = new TaskLabelCreateRequest();
        createRequest.setTaskId(taskId);
        createRequest.setLabelId(labelId);

        updateRequest = new TaskLabelUpdateRequest();
        updateRequest.setTaskId(UUID.randomUUID());
        updateRequest.setLabelId(UUID.randomUUID());
    }

    @Test
    void create_ShouldReturnTaskLabelResponse_WhenLabelIsInTasksProject() {
        when(projectAccessGuard.requireTaskMember(taskId, userId)).thenReturn(projectId);
        when(projectServiceClient.findLabel(labelId, userId)).thenReturn(label(labelId, projectId));
        when(taskLabelRepository.save(any(TaskLabel.class))).thenReturn(taskLabel);

        TaskLabelResponse result = taskLabelService.create(createRequest, userId);

        assertEquals(taskId, result.getTaskId());
        assertEquals(labelId, result.getLabelId());
    }

    @Test
    void create_ShouldThrowConflict_WhenLabelBelongsToAnotherProject() {
        when(projectAccessGuard.requireTaskMember(taskId, userId)).thenReturn(projectId);
        when(projectServiceClient.findLabel(labelId, userId)).thenReturn(label(labelId, UUID.randomUUID()));

        assertThrows(ResourceConflictException.class, () -> taskLabelService.create(createRequest, userId));

        verify(taskLabelRepository, never()).save(any(TaskLabel.class));
    }

    @Test
    void create_ShouldThrowAccessDenied_WhenCallerIsNotMember() {
        when(projectAccessGuard.requireTaskMember(taskId, userId)).thenThrow(new AccessDeniedException("denied"));

        assertThrows(AccessDeniedException.class, () -> taskLabelService.create(createRequest, userId));

        verify(taskLabelRepository, never()).save(any(TaskLabel.class));
    }

    @Test
    void findAll_WithTaskIdFilter_ShouldReturnFilteredTaskLabels() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<TaskLabel> page = new PageImpl<>(java.util.List.of(taskLabel));
        when(taskLabelRepository.findByTaskId(taskId, pageable)).thenReturn(page);

        PageResponse<TaskLabelResponse> result = taskLabelService.findAll(taskId, null, pageable, userId);

        assertEquals(1, result.content().size());
        verify(projectAccessGuard).requireTaskMember(taskId, userId);
    }

    @Test
    void findAll_WithTaskIdAndLabelIdFilters_ShouldReturnFilteredTaskLabels() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<TaskLabel> page = new PageImpl<>(java.util.List.of(taskLabel));
        when(taskLabelRepository.findByTaskIdAndLabelId(taskId, labelId, pageable)).thenReturn(page);

        PageResponse<TaskLabelResponse> result = taskLabelService.findAll(taskId, labelId, pageable, userId);

        assertEquals(1, result.content().size());
    }

    @Test
    void findById_ShouldReturnTaskLabelResponse_WhenCallerIsMember() {
        when(taskLabelRepository.findByTaskIdAndLabelId(taskId, labelId)).thenReturn(Optional.of(taskLabel));

        TaskLabelResponse result = taskLabelService.findById(taskId, labelId, userId);

        assertEquals(taskId, result.getTaskId());
        verify(projectAccessGuard).requireTaskMember(taskId, userId);
    }

    @Test
    void findById_ShouldThrowAccessDenied_BeforeLookingUpTheLabel_WhenCallerIsNotMember() {
        when(projectAccessGuard.requireTaskMember(taskId, userId)).thenThrow(new AccessDeniedException("denied"));

        assertThrows(AccessDeniedException.class, () -> taskLabelService.findById(taskId, labelId, userId));

        verifyNoInteractions(taskLabelRepository);
    }

    @Test
    void findById_ShouldThrowResourceNotFoundException_WhenTaskLabelNotFound() {
        when(taskLabelRepository.findByTaskIdAndLabelId(taskId, labelId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> taskLabelService.findById(taskId, labelId, userId));
    }

    @Test
    void update_ShouldReturnUpdatedTaskLabelResponse_WhenCallerIsMemberOfBothTasks() {
        when(projectAccessGuard.requireTaskMember(taskId, userId)).thenReturn(projectId);
        when(projectAccessGuard.requireTaskMember(updateRequest.getTaskId(), userId)).thenReturn(projectId);
        when(projectServiceClient.findLabel(updateRequest.getLabelId(), userId))
                .thenReturn(label(updateRequest.getLabelId(), projectId));
        when(taskLabelRepository.findByTaskIdAndLabelId(taskId, labelId)).thenReturn(Optional.of(taskLabel));
        doNothing().when(modelMapper).map(updateRequest, taskLabel);

        TaskLabelResponse result = taskLabelService.update(taskId, labelId, updateRequest, userId);

        assertNotNull(result);
        verify(projectAccessGuard).requireTaskMember(taskId, userId);
        verify(projectAccessGuard).requireTaskMember(updateRequest.getTaskId(), userId);
        verify(modelMapper).map(updateRequest, taskLabel);
    }

    @Test
    void update_ShouldThrowAccessDenied_WhenMovingLabelToTaskCallerCannotAccess() {
        when(projectAccessGuard.requireTaskMember(taskId, userId)).thenReturn(UUID.randomUUID());
        when(projectAccessGuard.requireTaskMember(updateRequest.getTaskId(), userId))
                .thenThrow(new AccessDeniedException("denied"));

        assertThrows(AccessDeniedException.class,
                () -> taskLabelService.update(taskId, labelId, updateRequest, userId));

        verify(modelMapper, never()).map(any(), any());
    }

    @Test
    void update_ShouldThrowResourceNotFoundException_WhenTaskLabelNotFound() {
        when(projectAccessGuard.requireTaskMember(taskId, userId)).thenReturn(projectId);
        when(projectAccessGuard.requireTaskMember(updateRequest.getTaskId(), userId)).thenReturn(projectId);
        when(projectServiceClient.findLabel(updateRequest.getLabelId(), userId))
                .thenReturn(label(updateRequest.getLabelId(), projectId));
        when(taskLabelRepository.findByTaskIdAndLabelId(taskId, labelId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> taskLabelService.update(taskId, labelId, updateRequest, userId));

        verify(modelMapper, never()).map(any(), any());
    }

    @Test
    void delete_ShouldDeleteTaskLabel_WhenCallerIsMember() {
        when(taskLabelRepository.findByTaskIdAndLabelId(taskId, labelId)).thenReturn(Optional.of(taskLabel));

        taskLabelService.delete(taskId, labelId, userId);

        verify(taskLabelRepository).delete(taskLabel);
    }

    @Test
    void delete_ShouldThrowAccessDenied_WhenCallerIsNotMember() {
        when(projectAccessGuard.requireTaskMember(taskId, userId)).thenThrow(new AccessDeniedException("denied"));

        assertThrows(AccessDeniedException.class, () -> taskLabelService.delete(taskId, labelId, userId));

        verify(taskLabelRepository, never()).delete(any(TaskLabel.class));
    }

    @Test
    void delete_ShouldThrowResourceNotFoundException_WhenTaskLabelNotFound() {
        when(taskLabelRepository.findByTaskIdAndLabelId(taskId, labelId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> taskLabelService.delete(taskId, labelId, userId));

        verify(taskLabelRepository, never()).delete(any(TaskLabel.class));
    }

    private static JSendResponse<Map<String, LabelSummary>> label(UUID id, UUID projectId) {
        return JSendResponse.success(Map.of("label", new LabelSummary(id, projectId)));
    }
}
