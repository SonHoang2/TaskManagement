package com.sonhoang2.task_service.tasklabel;

import com.sonhoang2.task_service.common.dto.PageResponse;
import com.sonhoang2.task_service.common.exception.ResourceNotFoundException;
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
    public TaskLabelResponse create(TaskLabelCreateRequest request) {
        TaskLabel taskLabel = TaskLabel.builder()
                .taskId(request.getTaskId())
                .labelId(request.getLabelId())
                .build();
        return toResponse(taskLabelRepository.save(taskLabel));
    }

    @Override
    public PageResponse<TaskLabelResponse> findAll(UUID taskId, UUID labelId, Pageable pageable) {
        Page<TaskLabel> page;

        if (taskId != null && labelId != null) {
            page = taskLabelRepository.findByTaskIdAndLabelId(taskId, labelId, pageable);
        } else if (taskId != null) {
            page = taskLabelRepository.findByTaskId(taskId, pageable);
        } else if (labelId != null) {
            page = taskLabelRepository.findByLabelId(labelId, pageable);
        } else {
            page = taskLabelRepository.findAll(pageable);
        }

        return toPageResponse(page);
    }

    @Override
    @Transactional(readOnly = true)
    public TaskLabelResponse findById(UUID taskId, UUID labelId) {
        return toResponse(findTaskLabelOrThrow(taskId, labelId));
    }

    @Override
    public TaskLabelResponse update(UUID taskId, UUID labelId, TaskLabelUpdateRequest request) {
        TaskLabel existing = findTaskLabelOrThrow(taskId, labelId);
        modelMapper.map(request, existing);
        return toResponse(existing);
    }

    @Override
    public void delete(UUID taskId, UUID labelId) {
        taskLabelRepository.delete(findTaskLabelOrThrow(taskId, labelId));
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