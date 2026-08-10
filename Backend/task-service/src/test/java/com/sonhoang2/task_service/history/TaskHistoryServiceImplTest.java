package com.sonhoang2.task_service.history;

import com.sonhoang2.task_service.common.dto.PageResponse;
import com.sonhoang2.task_service.common.exception.ResourceNotFoundException;
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

    @InjectMocks
    private TaskHistoryServiceImpl taskHistoryService;

    private TaskHistory history;
    private TaskHistoryCreateRequest createRequest;
    private TaskHistoryUpdateRequest updateRequest;
    private UUID historyId;
    private UUID taskId;
    private UUID changedBy;

    @BeforeEach
    void setUp() {
        historyId = UUID.randomUUID();
        taskId = UUID.randomUUID();
        changedBy = UUID.randomUUID();

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
        createRequest.setChangedBy(changedBy);
        createRequest.setField("status");
        createRequest.setOldValue("TODO");
        createRequest.setNewValue("IN_PROGRESS");

        updateRequest = new TaskHistoryUpdateRequest();
        updateRequest.setField("priority");
        updateRequest.setOldValue("HIGH");
        updateRequest.setNewValue("MEDIUM");
    }

    @Test
    void create_ShouldReturnHistoryResponse() {
        when(taskHistoryRepository.save(any(TaskHistory.class))).thenReturn(history);

        TaskHistoryResponse result = taskHistoryService.create(createRequest);

        assertNotNull(result);
        assertEquals(historyId, result.getId());
        assertEquals(taskId, result.getTaskId());
        assertEquals(changedBy, result.getChangedBy());
        assertEquals("status", result.getField());
        assertEquals("TODO", result.getOldValue());
        assertEquals("IN_PROGRESS", result.getNewValue());
        verify(taskHistoryRepository).save(any(TaskHistory.class));
    }

    @Test
    void findAll_WithoutFilters_ShouldReturnAllHistories() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<TaskHistory> historyPage = new PageImpl<>(java.util.List.of(history));
        when(taskHistoryRepository.findAll(pageable)).thenReturn(historyPage);

        PageResponse<TaskHistoryResponse> result = taskHistoryService.findAll(null, null, pageable);

        assertNotNull(result);
        assertEquals(1, result.content().size());
        verify(taskHistoryRepository).findAll(pageable);
    }

    @Test
    void findAll_WithTaskIdFilter_ShouldReturnFilteredHistories() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<TaskHistory> historyPage = new PageImpl<>(java.util.List.of(history));
        when(taskHistoryRepository.findByTaskId(taskId, pageable)).thenReturn(historyPage);

        PageResponse<TaskHistoryResponse> result = taskHistoryService.findAll(taskId, null, pageable);

        assertNotNull(result);
        assertEquals(1, result.content().size());
        verify(taskHistoryRepository).findByTaskId(taskId, pageable);
    }

    @Test
    void findAll_WithChangedByFilter_ShouldReturnFilteredHistories() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<TaskHistory> historyPage = new PageImpl<>(java.util.List.of(history));
        when(taskHistoryRepository.findByChangedBy(changedBy, pageable)).thenReturn(historyPage);

        PageResponse<TaskHistoryResponse> result = taskHistoryService.findAll(null, changedBy, pageable);

        assertNotNull(result);
        assertEquals(1, result.content().size());
        verify(taskHistoryRepository).findByChangedBy(changedBy, pageable);
    }

    @Test
    void findAll_WithTaskIdAndChangedByFilters_ShouldReturnFilteredHistories() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<TaskHistory> historyPage = new PageImpl<>(java.util.List.of(history));
        when(taskHistoryRepository.findByTaskIdAndChangedBy(taskId, changedBy, pageable)).thenReturn(historyPage);

        PageResponse<TaskHistoryResponse> result = taskHistoryService.findAll(taskId, changedBy, pageable);

        assertNotNull(result);
        assertEquals(1, result.content().size());
        verify(taskHistoryRepository).findByTaskIdAndChangedBy(taskId, changedBy, pageable);
    }

    @Test
    void findById_ShouldReturnHistoryResponse_WhenHistoryExists() {
        when(taskHistoryRepository.findById(historyId)).thenReturn(Optional.of(history));

        TaskHistoryResponse result = taskHistoryService.findById(historyId);

        assertNotNull(result);
        assertEquals(historyId, result.getId());
        verify(taskHistoryRepository).findById(historyId);
    }

    @Test
    void findById_ShouldThrowResourceNotFoundException_WhenHistoryNotFound() {
        when(taskHistoryRepository.findById(historyId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> taskHistoryService.findById(historyId));

        verify(taskHistoryRepository).findById(historyId);
    }

    @Test
    void update_ShouldReturnUpdatedHistoryResponse_WhenHistoryExists() {
        when(taskHistoryRepository.findById(historyId)).thenReturn(Optional.of(history));
        doNothing().when(modelMapper).map(updateRequest, history);

        TaskHistoryResponse result = taskHistoryService.update(historyId, updateRequest);

        assertNotNull(result);
        verify(taskHistoryRepository).findById(historyId);
        verify(modelMapper).map(updateRequest, history);
    }

    @Test
    void update_ShouldThrowResourceNotFoundException_WhenHistoryNotFound() {
        when(taskHistoryRepository.findById(historyId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> taskHistoryService.update(historyId, updateRequest));

        verify(taskHistoryRepository).findById(historyId);
        verify(modelMapper, never()).map(any(), any());
    }

    @Test
    void delete_ShouldDeleteHistory_WhenHistoryExists() {
        when(taskHistoryRepository.findById(historyId)).thenReturn(Optional.of(history));
        doNothing().when(taskHistoryRepository).delete(history);

        taskHistoryService.delete(historyId);

        verify(taskHistoryRepository).findById(historyId);
        verify(taskHistoryRepository).delete(history);
    }

    @Test
    void delete_ShouldThrowResourceNotFoundException_WhenHistoryNotFound() {
        when(taskHistoryRepository.findById(historyId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> taskHistoryService.delete(historyId));

        verify(taskHistoryRepository).findById(historyId);
        verify(taskHistoryRepository, never()).delete(any(TaskHistory.class));
    }
}
