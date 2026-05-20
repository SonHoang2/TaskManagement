package com.sonhoang2.TaskManagementAPI.tasklabel;

import com.sonhoang2.TaskManagementAPI.common.dto.PageResponse;
import com.sonhoang2.TaskManagementAPI.common.exception.ResourceNotFoundException;
import com.sonhoang2.TaskManagementAPI.tasklabel.dto.TaskLabelCreateRequest;
import com.sonhoang2.TaskManagementAPI.tasklabel.dto.TaskLabelResponse;
import com.sonhoang2.TaskManagementAPI.tasklabel.dto.TaskLabelUpdateRequest;
import com.sonhoang2.TaskManagementAPI.tasklabel.entity.TaskLabel;
import com.sonhoang2.TaskManagementAPI.tasklabel.entity.TaskLabelId;
import lombok.RequiredArgsConstructor;
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

    private PageResponse<TaskLabelResponse> toPageResponse(Page<TaskLabel> page) {
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
    public TaskLabelResponse create(TaskLabelCreateRequest request) {
        TaskLabelId id = new TaskLabelId(request.getTaskId(), request.getLabelId());
        TaskLabel taskLabel = TaskLabel.builder().id(id).build();
        return toResponse(taskLabelRepository.save(taskLabel));
    }

    @Override
    public PageResponse<TaskLabelResponse> findAll(UUID taskId, UUID labelId, Pageable pageable) {
        Page<TaskLabel> page;

        if (taskId != null && labelId != null) {
            page = taskLabelRepository.findByIdTaskIdAndIdLabelId(taskId, labelId, pageable);
        } else if (taskId != null) {
            page = taskLabelRepository.findByIdTaskId(taskId, pageable);
        } else if (labelId != null) {
            page = taskLabelRepository.findByIdLabelId(labelId, pageable);
        } else {
            page = taskLabelRepository.findAll(pageable);
        }

        return toPageResponse(page);
    }

    @Override
    @Transactional(readOnly = true)
    public TaskLabelResponse findById(UUID taskId, UUID labelId) {
        return toResponse(findTaskLabelByIdOrThrow(taskId, labelId));
    }

    @Override
    public TaskLabelResponse update(UUID taskId, UUID labelId, TaskLabelUpdateRequest request) {
        TaskLabel existing = findTaskLabelByIdOrThrow(taskId, labelId);
        TaskLabelId newId = new TaskLabelId(request.getTaskId(), request.getLabelId());

        if (!existing.getId().equals(newId)) {
            taskLabelRepository.delete(existing);
            TaskLabel replacement = TaskLabel.builder().id(newId).build();
            return toResponse(taskLabelRepository.save(replacement));
        }

        return toResponse(existing);
    }

    @Override
    public void delete(UUID taskId, UUID labelId) {
        taskLabelRepository.delete(findTaskLabelByIdOrThrow(taskId, labelId));
    }

    private TaskLabel findTaskLabelByIdOrThrow(UUID taskId, UUID labelId) {
        TaskLabelId id = new TaskLabelId(taskId, labelId);
        return taskLabelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TaskLabel with id " + id + " not found"));
    }

    private TaskLabelResponse toResponse(TaskLabel taskLabel) {
        return TaskLabelResponse.builder()
                .taskId(taskLabel.getId().getTaskId())
                .labelId(taskLabel.getId().getLabelId())
                .build();
    }
}



