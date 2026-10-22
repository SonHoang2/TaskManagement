package com.sonhoang2.task_service.tasklabel;

import com.sonhoang2.common.exception.ResourceConflictException;
import com.sonhoang2.task_service.common.dto.LabelSummary;
import com.sonhoang2.task_service.common.dto.PageResponse;
import com.sonhoang2.task_service.common.exception.ResourceNotFoundException;
import com.sonhoang2.task_service.common.security.ProjectAccessGuard;
import com.sonhoang2.task_service.feign.ProjectServiceClient;
import com.sonhoang2.task_service.tasklabel.dto.TaskLabelCreateRequest;
import com.sonhoang2.task_service.tasklabel.dto.TaskLabelResponse;
import com.sonhoang2.task_service.tasklabel.dto.TaskLabelUpdateRequest;
import com.sonhoang2.task_service.tasklabel.entity.TaskLabel;
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
public class TaskLabelServiceImpl implements TaskLabelService {

    private final TaskLabelRepository taskLabelRepository;
    private final ModelMapper modelMapper;
    private final ProjectAccessGuard projectAccessGuard;
    private final ProjectServiceClient projectServiceClient;

    private PageResponse<TaskLabelResponse> toPageResponse(Page<TaskLabel> page) {
        return new PageResponse<>(
                page.getContent().stream().map(this::toResponse).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext(),
                page.hasPrevious(),
                page.getNumberOfElements()
        );
    }

    @Override
    public TaskLabelResponse create(TaskLabelCreateRequest request, UUID currentUserId) {
        UUID projectId = projectAccessGuard.requireTaskMember(request.getTaskId(), currentUserId);
        requireLabelInProject(request.getLabelId(), projectId, currentUserId);

        TaskLabel taskLabel = TaskLabel.builder()
                .taskId(request.getTaskId())
                .labelId(request.getLabelId())
                .build();
        return toResponse(taskLabelRepository.save(taskLabel));
    }

    @Override
    public PageResponse<TaskLabelResponse> findAll(UUID taskId, UUID labelId, Pageable pageable, UUID currentUserId) {
        projectAccessGuard.requireTaskMember(taskId, currentUserId);

        Page<TaskLabel> page = labelId != null
                ? taskLabelRepository.findByTaskIdAndLabelId(taskId, labelId, pageable)
                : taskLabelRepository.findByTaskId(taskId, pageable);

        return toPageResponse(page);
    }

    @Override
    @Transactional(readOnly = true)
    public TaskLabelResponse findById(UUID taskId, UUID labelId, UUID currentUserId) {
        projectAccessGuard.requireTaskMember(taskId, currentUserId);
        return toResponse(findTaskLabelOrThrow(taskId, labelId));
    }

    @Override
    public TaskLabelResponse update(UUID taskId, UUID labelId, TaskLabelUpdateRequest request, UUID currentUserId) {
        projectAccessGuard.requireTaskMember(taskId, currentUserId);
        UUID newTaskId = request.getTaskId() != null ? request.getTaskId() : taskId;
        UUID newLabelId = request.getLabelId() != null ? request.getLabelId() : labelId;
        if (!newTaskId.equals(taskId) || !newLabelId.equals(labelId)) {
            UUID projectId = projectAccessGuard.requireTaskMember(newTaskId, currentUserId);
            requireLabelInProject(newLabelId, projectId, currentUserId);
        }
        TaskLabel existing = findTaskLabelOrThrow(taskId, labelId);
        modelMapper.map(request, existing);
        return toResponse(existing);
    }

    @Override
    public void delete(UUID taskId, UUID labelId, UUID currentUserId) {
        projectAccessGuard.requireTaskMember(taskId, currentUserId);
        taskLabelRepository.delete(findTaskLabelOrThrow(taskId, labelId));
    }

    private void requireLabelInProject(UUID labelId, UUID projectId, UUID currentUserId) {
        LabelSummary label = projectServiceClient.findLabel(labelId, currentUserId).data().get("label");
        if (!projectId.equals(label.getProjectId())) {
            throw new ResourceConflictException("Label and task belong to different projects");
        }
    }

    private TaskLabel findTaskLabelOrThrow(UUID taskId, UUID labelId) {
        return taskLabelRepository.findByTaskIdAndLabelId(taskId, labelId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "TaskLabel with taskId " + taskId + " and labelId " + labelId + " not found"
                ));
    }

    private TaskLabelResponse toResponse(TaskLabel taskLabel) {
        return TaskLabelResponse.builder()
                .taskId(taskLabel.getTaskId())
                .labelId(taskLabel.getLabelId())
                .build();
    }
}