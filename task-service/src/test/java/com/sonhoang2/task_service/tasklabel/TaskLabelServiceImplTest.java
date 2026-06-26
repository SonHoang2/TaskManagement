package com.sonhoang2.task_service.tasklabel;

import com.sonhoang2.task_service.common.dto.PageResponse;
import com.sonhoang2.task_service.common.exception.ResourceNotFoundException;
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

    @InjectMocks
    private TaskLabelServiceImpl taskLabelService;

    private TaskLabel taskLabel;
    private TaskLabelCreateRequest createRequest;
    private TaskLabelUpdateRequest updateRequest;
    private UUID taskId;
    private UUID labelId;

    @BeforeEach
    void setUp() {
        taskId = UUID.randomUUID();
        labelId = UUID.randomUUID();

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
    void create_ShouldReturnTaskLabelResponse() {
        when(taskLabelRepository.save(any(TaskLabel.class))).thenReturn(taskLabel);

        TaskLabelResponse result = taskLabelService.create(createRequest);

        assertNotNull(result);
        assertEquals(taskId, result.getTaskId());
        assertEquals(labelId, result.getLabelId());
        verify(taskLabelRepository).save(any(TaskLabel.class));
    }

    @Test
    void findAll_WithoutFilters_ShouldReturnAllTaskLabels() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<TaskLabel> taskLabelPage = new PageImpl<>(java.util.List.of(taskLabel));
        when(taskLabelRepository.findAll(pageable)).thenReturn(taskLabelPage);

        PageResponse<TaskLabelResponse> result = taskLabelService.findAll(null, null, pageable);

        assertNotNull(result);
        assertEquals(1, result.getData().size());
        verify(taskLabelRepository).findAll(pageable);
    }

    @Test
    void findAll_WithTaskIdFilter_ShouldReturnFilteredTaskLabels() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<TaskLabel> taskLabelPage = new PageImpl<>(java.util.List.of(taskLabel));
        when(taskLabelRepository.findByTaskId(taskId, pageable)).thenReturn(taskLabelPage);

        PageResponse<TaskLabelResponse> result = taskLabelService.findAll(taskId, null, pageable);

        assertNotNull(result);
        assertEquals(1, result.getData().size());
        verify(taskLabelRepository).findByTaskId(taskId, pageable);
    }

    @Test
    void findAll_WithLabelIdFilter_ShouldReturnFilteredTaskLabels() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<TaskLabel> taskLabelPage = new PageImpl<>(java.util.List.of(taskLabel));
        when(taskLabelRepository.findByLabelId(labelId, pageable)).thenReturn(taskLabelPage);

        PageResponse<TaskLabelResponse> result = taskLabelService.findAll(null, labelId, pageable);

        assertNotNull(result);
        assertEquals(1, result.getData().size());
        verify(taskLabelRepository).findByLabelId(labelId, pageable);
    }

    @Test
    void findAll_WithTaskIdAndLabelIdFilters_ShouldReturnFilteredTaskLabels() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<TaskLabel> taskLabelPage = new PageImpl<>(java.util.List.of(taskLabel));
        when(taskLabelRepository.findByTaskIdAndLabelId(taskId, labelId, pageable)).thenReturn(taskLabelPage);

        PageResponse<TaskLabelResponse> result = taskLabelService.findAll(taskId, labelId, pageable);

        assertNotNull(result);
        assertEquals(1, result.getData().size());
        verify(taskLabelRepository).findByTaskIdAndLabelId(taskId, labelId, pageable);
    }

    @Test
    void findById_ShouldReturnTaskLabelResponse_WhenTaskLabelExists() {
        when(taskLabelRepository.findByTaskIdAndLabelId(taskId, labelId)).thenReturn(Optional.of(taskLabel));

        TaskLabelResponse result = taskLabelService.findById(taskId, labelId);

        assertNotNull(result);
        assertEquals(taskId, result.getTaskId());
        assertEquals(labelId, result.getLabelId());
        verify(taskLabelRepository).findByTaskIdAndLabelId(taskId, labelId);
    }

    @Test
    void findById_ShouldThrowResourceNotFoundException_WhenTaskLabelNotFound() {
        when(taskLabelRepository.findByTaskIdAndLabelId(taskId, labelId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> taskLabelService.findById(taskId, labelId));

        verify(taskLabelRepository).findByTaskIdAndLabelId(taskId, labelId);
    }

    @Test
    void update_ShouldReturnUpdatedTaskLabelResponse_WhenTaskLabelExists() {
        when(taskLabelRepository.findByTaskIdAndLabelId(taskId, labelId)).thenReturn(Optional.of(taskLabel));
        when(modelMapper.map(updateRequest, taskLabel)).thenReturn(taskLabel);

        TaskLabelResponse result = taskLabelService.update(taskId, labelId, updateRequest);

        assertNotNull(result);
        verify(taskLabelRepository).findByTaskIdAndLabelId(taskId, labelId);
        verify(modelMapper).map(updateRequest, taskLabel);
    }

    @Test
    void update_ShouldThrowResourceNotFoundException_WhenTaskLabelNotFound() {
        when(taskLabelRepository.findByTaskIdAndLabelId(taskId, labelId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> taskLabelService.update(taskId, labelId, updateRequest));

        verify(taskLabelRepository).findByTaskIdAndLabelId(taskId, labelId);
        verify(modelMapper, never()).map(any(), any());
    }

    @Test
    void delete_ShouldDeleteTaskLabel_WhenTaskLabelExists() {
        when(taskLabelRepository.findByTaskIdAndLabelId(taskId, labelId)).thenReturn(Optional.of(taskLabel));
        doNothing().when(taskLabelRepository).delete(taskLabel);

        taskLabelService.delete(taskId, labelId);

        verify(taskLabelRepository).findByTaskIdAndLabelId(taskId, labelId);
        verify(taskLabelRepository).delete(taskLabel);
    }

    @Test
    void delete_ShouldThrowResourceNotFoundException_WhenTaskLabelNotFound() {
        when(taskLabelRepository.findByTaskIdAndLabelId(taskId, labelId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> taskLabelService.delete(taskId, labelId));

        verify(taskLabelRepository).findByTaskIdAndLabelId(taskId, labelId);
        verify(taskLabelRepository, never()).delete(any(TaskLabel.class));
    }
}
