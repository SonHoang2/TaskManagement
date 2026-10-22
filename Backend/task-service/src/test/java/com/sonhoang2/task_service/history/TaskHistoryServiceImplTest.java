package com.sonhoang2.task_service.history;

import com.sonhoang2.task_service.common.dto.PageResponse;
import com.sonhoang2.task_service.common.exception.ResourceNotFoundException;
import com.sonhoang2.task_service.common.security.ProjectAccessGuard;
import com.sonhoang2.task_service.history.dto.TaskHistoryCreateRequest;
import com.sonhoang2.task_service.history.dto.TaskHistoryResponse;
import com.sonhoang2.task_service.history.dto.TaskHistoryUpdateRequest;
import com.sonhoang2.task_service.history.entity.TaskHistory;
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

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskHistoryServiceImplTest {

    @Mock
    private TaskHistoryRepository taskHistoryRepository;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private ProjectAccessGuard projectAccessGuard;

    @InjectMocks
    private TaskHistoryServiceImpl taskHistoryService;

    private TaskHistory history;
    private TaskHistoryCreateRequest createRequest;
    private TaskHistoryUpdateRequest updateRequest;
    private UUID historyId;
    private UUID taskId;
    private UUID changedBy;
    private UUID projectId;

    @BeforeEach
    void setUp() {
        historyId = UUID.randomUUID();
        taskId = UUID.randomUUID();
        changedBy = UUID.randomUUID();
        projectId = UUID.randomUUID();

        history = TaskHistory.builder()
                .id(historyId)
                .taskId(taskId)
                .changedBy(changedBy)
                .field("status")
                .oldValue("TODO")
                .newValue("IN_PROGRESS")
                .createdAt(Instant.now())
                .build();

        createRequest = new TaskHistoryCreateRequest();
        createRequest.setTaskId(taskId);
        createRequest.setField("status");
        createRequest.setOldValue("TODO");
        createRequest.setNewValue("IN_PROGRESS");

        updateRequest = new TaskHistoryUpdateRequest();
        updateRequest.setField("priority");
        updateRequest.setOldValue("HIGH");
        updateRequest.setNewValue("MEDIUM");
    }

    @Test
    void create_ShouldRecordCallerAsAuthor_WhenCallerIsMember() {
        when(taskHistoryRepository.save(any(TaskHistory.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskHistoryResponse result = taskHistoryService.create(createRequest, changedBy);

        assertEquals(taskId, result.getTaskId());
        assertEquals(changedBy, result.getChangedBy());
        assertEquals("status", result.getField());
        verify(projectAccessGuard).requireTaskMember(taskId, changedBy);
    }

    @Test
    void create_ShouldThrowAccessDenied_WhenCallerIsNotMember() {
        when(projectAccessGuard.requireTaskMember(taskId, changedBy)).thenThrow(new AccessDeniedException("denied"));

        assertThrows(AccessDeniedException.class, () -> taskHistoryService.create(createRequest, changedBy));

        verify(taskHistoryRepository, never()).save(any(TaskHistory.class));
    }

    @Test
    void findAll_WithTaskIdFilter_ShouldReturnFilteredHistories() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<TaskHistory> historyPage = new PageImpl<>(java.util.List.of(history));
        when(taskHistoryRepository.findByTaskId(taskId, pageable)).thenReturn(historyPage);

        PageResponse<TaskHistoryResponse> result = taskHistoryService.findAll(taskId, null, pageable, changedBy);

        assertEquals(1, result.content().size());
        verify(projectAccessGuard).requireTaskMember(taskId, changedBy);
    }

    @Test
    void findAll_WithTaskIdAndChangedByFilters_ShouldReturnFilteredHistories() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<TaskHistory> historyPage = new PageImpl<>(java.util.List.of(history));
        when(taskHistoryRepository.findByTaskIdAndChangedBy(taskId, changedBy, pageable)).thenReturn(historyPage);

        PageResponse<TaskHistoryResponse> result =
                taskHistoryService.findAll(taskId, changedBy, pageable, changedBy);

        assertEquals(1, result.content().size());
    }

    @Test
    void findById_ShouldReturnHistoryResponse_WhenCallerIsMember() {
        when(taskHistoryRepository.findById(historyId)).thenReturn(Optional.of(history));

        TaskHistoryResponse result = taskHistoryService.findById(historyId, changedBy);

        assertEquals(historyId, result.getId());
        verify(projectAccessGuard).requireTaskMember(taskId, changedBy);
    }

    @Test
    void findById_ShouldThrowResourceNotFoundException_WhenHistoryNotFound() {
        when(taskHistoryRepository.findById(historyId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> taskHistoryService.findById(historyId, changedBy));
    }

    @Test
    void update_ShouldReturnUpdatedHistoryResponse_WhenCallerIsManager() {
        when(taskHistoryRepository.findById(historyId)).thenReturn(Optional.of(history));
        when(projectAccessGuard.requireTaskMember(taskId, changedBy)).thenReturn(projectId);
        doNothing().when(modelMapper).map(updateRequest, history);

        TaskHistoryResponse result = taskHistoryService.update(historyId, updateRequest, changedBy);

        assertNotNull(result);
        verify(projectAccessGuard).requireManager(projectId, changedBy);
        verify(modelMapper).map(updateRequest, history);
    }

    @Test
    void update_ShouldThrowAccessDenied_WhenCallerIsNotManager() {
        when(taskHistoryRepository.findById(historyId)).thenReturn(Optional.of(history));
        when(projectAccessGuard.requireTaskMember(taskId, changedBy)).thenReturn(projectId);
        doThrow(new AccessDeniedException("denied")).when(projectAccessGuard).requireManager(projectId, changedBy);

        assertThrows(AccessDeniedException.class,
                () -> taskHistoryService.update(historyId, updateRequest, changedBy));

        verify(modelMapper, never()).map(any(), any());
    }

    @Test
    void update_ShouldThrowResourceNotFoundException_WhenHistoryNotFound() {
        when(taskHistoryRepository.findById(historyId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> taskHistoryService.update(historyId, updateRequest, changedBy));

        verify(modelMapper, never()).map(any(), any());
    }

    @Test
    void delete_ShouldDeleteHistory_WhenCallerIsManager() {
        when(taskHistoryRepository.findById(historyId)).thenReturn(Optional.of(history));
        when(projectAccessGuard.requireTaskMember(taskId, changedBy)).thenReturn(projectId);

        taskHistoryService.delete(historyId, changedBy);

        verify(projectAccessGuard).requireManager(projectId, changedBy);
        verify(taskHistoryRepository).delete(history);
    }

    @Test
    void delete_ShouldThrowAccessDenied_WhenCallerIsNotManager() {
        when(taskHistoryRepository.findById(historyId)).thenReturn(Optional.of(history));
        when(projectAccessGuard.requireTaskMember(taskId, changedBy)).thenReturn(projectId);
        doThrow(new AccessDeniedException("denied")).when(projectAccessGuard).requireManager(projectId, changedBy);

        assertThrows(AccessDeniedException.class, () -> taskHistoryService.delete(historyId, changedBy));

        verify(taskHistoryRepository, never()).delete(any(TaskHistory.class));
    }

    @Test
    void delete_ShouldThrowResourceNotFoundException_WhenHistoryNotFound() {
        when(taskHistoryRepository.findById(historyId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> taskHistoryService.delete(historyId, changedBy));

        verify(taskHistoryRepository, never()).delete(any(TaskHistory.class));
    }
}
