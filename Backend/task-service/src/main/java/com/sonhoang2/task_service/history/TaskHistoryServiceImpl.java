package com.sonhoang2.task_service.history;

import com.sonhoang2.task_service.common.dto.PageResponse;
import com.sonhoang2.task_service.common.exception.ResourceNotFoundException;
import com.sonhoang2.task_service.common.security.ProjectAccessGuard;
import com.sonhoang2.task_service.history.dto.TaskHistoryCreateRequest;
import com.sonhoang2.task_service.history.dto.TaskHistoryResponse;
import com.sonhoang2.task_service.history.dto.TaskHistoryUpdateRequest;
import com.sonhoang2.task_service.history.entity.TaskHistory;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
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
    private final ModelMapper modelMapper;
    private final ProjectAccessGuard projectAccessGuard;

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
    public TaskHistoryResponse create(TaskHistoryCreateRequest request, UUID currentUserId) {
        projectAccessGuard.requireTaskMember(request.getTaskId(), currentUserId);

        TaskHistory history = TaskHistory.builder()
                .taskId(request.getTaskId())
                .changedBy(currentUserId)
                .field(request.getField())
                .oldValue(request.getOldValue())
                .newValue(request.getNewValue())
                .build();

        return toResponse(taskHistoryRepository.save(history));
    }

    @Override
    public PageResponse<TaskHistoryResponse> findAll(UUID taskId,
                                                     UUID changedBy,
                                                     Pageable pageable,
                                                     UUID currentUserId) {
        projectAccessGuard.requireTaskMember(taskId, currentUserId);

        Page<TaskHistory> page = changedBy != null
                ? taskHistoryRepository.findByTaskIdAndChangedBy(taskId, changedBy, pageable)
                : taskHistoryRepository.findByTaskId(taskId, pageable);

        return toPageResponse(page);
    }

    @Override
    @Transactional(readOnly = true)
    public TaskHistoryResponse findById(UUID id, UUID currentUserId) {
        TaskHistory history = findHistoryByIdOrThrow(id);
        projectAccessGuard.requireTaskMember(history.getTaskId(), currentUserId);
        return toResponse(history);
    }

    // History is an audit trail: rewriting it is reserved for project owners/admins.
    @Override
    public TaskHistoryResponse update(UUID id, TaskHistoryUpdateRequest request, UUID currentUserId) {
        TaskHistory history = findHistoryByIdOrThrow(id);
        requireManagerOfTask(history.getTaskId(), currentUserId);
        modelMapper.map(request, history);

        return toResponse(history);
    }

    @Override
    public void delete(UUID id, UUID currentUserId) {
        TaskHistory history = findHistoryByIdOrThrow(id);
        requireManagerOfTask(history.getTaskId(), currentUserId);
        taskHistoryRepository.delete(history);
    }

    private void requireManagerOfTask(UUID taskId, UUID currentUserId) {
        UUID projectId = projectAccessGuard.requireTaskMember(taskId, currentUserId);
        projectAccessGuard.requireManager(projectId, currentUserId);
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



