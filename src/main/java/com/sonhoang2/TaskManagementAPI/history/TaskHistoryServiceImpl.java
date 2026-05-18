package com.sonhoang2.TaskManagementAPI.history;

import com.sonhoang2.TaskManagementAPI.common.dto.PageResponse;
import com.sonhoang2.TaskManagementAPI.common.exception.ResourceNotFoundException;
import com.sonhoang2.TaskManagementAPI.history.dto.TaskHistoryCreateRequest;
import com.sonhoang2.TaskManagementAPI.history.dto.TaskHistoryResponse;
import com.sonhoang2.TaskManagementAPI.history.dto.TaskHistoryUpdateRequest;
import com.sonhoang2.TaskManagementAPI.history.entity.TaskHistory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class TaskHistoryServiceImpl implements TaskHistoryService {

    private final TaskHistoryRepository taskHistoryRepository;

    private PageResponse<TaskHistoryResponse> toPageResponse(Page<TaskHistory> page) {
        return new PageResponse<>(page.getContent().stream().map(this::toResponse).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext(),
                page.hasPrevious(),
                page.getNumberOfElements());
    }

    @Override
    public TaskHistoryResponse create(TaskHistoryCreateRequest request) {
        TaskHistory history = TaskHistory.builder()
                .taskId(request.getTaskId())
                .changedBy(request.getChangedBy())
                .field(request.getField())
                .oldValue(request.getOldValue())
                .newValue(request.getNewValue())
                .build();

        return toResponse(taskHistoryRepository.save(history));
    }

    @Override
    public PageResponse<TaskHistoryResponse> findAll(UUID taskId, UUID changedBy, Pageable pageable) {
        Page<TaskHistory> page;

        if (taskId != null && changedBy != null) {
            page = taskHistoryRepository.findByTaskIdAndChangedBy(taskId, changedBy, pageable);
        } else if (taskId != null) {
            page = taskHistoryRepository.findByTaskId(taskId, pageable);
        } else if (changedBy != null) {
            page = taskHistoryRepository.findByChangedBy(changedBy, pageable);
        } else {
            page = taskHistoryRepository.findAll(pageable);
        }

        return toPageResponse(page);
    }

    @Override
    @Transactional(readOnly = true)
    public TaskHistoryResponse findById(UUID id) {
        return toResponse(findHistoryByIdOrThrow(id));
    }

    @Override
    public TaskHistoryResponse update(UUID id, TaskHistoryUpdateRequest request) {
        TaskHistory history = findHistoryByIdOrThrow(id);
        history.setTaskId(request.getTaskId());
        history.setChangedBy(request.getChangedBy());
        history.setField(request.getField());
        history.setOldValue(request.getOldValue());
        history.setNewValue(request.getNewValue());

        return toResponse(history);
    }

    @Override
    public void delete(UUID id) {
        taskHistoryRepository.delete(findHistoryByIdOrThrow(id));
    }

    private TaskHistory findHistoryByIdOrThrow(UUID id) {
        return taskHistoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TaskHistory with id " + id + " not found"));
    }

    private TaskHistoryResponse toResponse(TaskHistory history) {
        return TaskHistoryResponse.builder()
                .id(history.getId())
                .taskId(history.getTaskId())
                .changedBy(history.getChangedBy())
                .field(history.getField())
                .oldValue(history.getOldValue())
                .newValue(history.getNewValue())
                .createdAt(history.getCreatedAt())
                .build();
    }
}



