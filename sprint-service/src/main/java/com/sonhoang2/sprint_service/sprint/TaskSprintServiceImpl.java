package com.sonhoang2.sprint_service.sprint;

import com.sonhoang2.sprint_service.common.dto.PageResponse;
import com.sonhoang2.sprint_service.common.exception.ResourceNotFoundException;
import com.sonhoang2.sprint_service.sprint.dto.TaskSprintCreateRequest;
import com.sonhoang2.sprint_service.sprint.dto.TaskSprintResponse;
import com.sonhoang2.sprint_service.sprint.dto.TaskSprintUpdateRequest;
import com.sonhoang2.sprint_service.sprint.entity.TaskSprint;
import com.sonhoang2.sprint_service.sprint.entity.TaskSprintId;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class TaskSprintServiceImpl implements TaskSprintService {

    private final TaskSprintRepository taskSprintRepository;

    private PageResponse<TaskSprintResponse> toPageResponse(Page<TaskSprint> page) {
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
    public TaskSprintResponse create(TaskSprintCreateRequest request) {
        TaskSprintId id = new TaskSprintId(request.getTaskId(), request.getSprintId());
        TaskSprint taskSprint = TaskSprint.builder().id(id).build();
        return toResponse(taskSprintRepository.save(taskSprint));
    }

    @Override
    public PageResponse<TaskSprintResponse> findAll(UUID taskId, UUID sprintId, Pageable pageable) {
        Page<TaskSprint> page;

        if (taskId != null && sprintId != null) {
            page = taskSprintRepository.findByIdTaskIdAndIdSprintId(taskId, sprintId, pageable);
        } else if (taskId != null) {
            page = taskSprintRepository.findByIdTaskId(taskId, pageable);
        } else if (sprintId != null) {
            page = taskSprintRepository.findByIdSprintId(sprintId, pageable);
        } else {
            page = taskSprintRepository.findAll(pageable);
        }

        return toPageResponse(page);
    }

    @Override
    @Transactional(readOnly = true)
    public TaskSprintResponse findById(UUID taskId, UUID sprintId) {
        return toResponse(findTaskSprintByIdOrThrow(taskId, sprintId));
    }

    @Override
    public TaskSprintResponse update(UUID taskId, UUID sprintId, TaskSprintUpdateRequest request) {
        TaskSprint existing = findTaskSprintByIdOrThrow(taskId, sprintId);
        TaskSprintId newId = new TaskSprintId(request.getTaskId(), request.getSprintId());

        if (!existing.getId().equals(newId)) {
            taskSprintRepository.delete(existing);
            TaskSprint replacement = TaskSprint.builder().id(newId).build();
            return toResponse(taskSprintRepository.save(replacement));
        }

        return toResponse(existing);
    }

    @Override
    public void delete(UUID taskId, UUID sprintId) {
        taskSprintRepository.delete(findTaskSprintByIdOrThrow(taskId, sprintId));
    }

    private TaskSprint findTaskSprintByIdOrThrow(UUID taskId, UUID sprintId) {
        TaskSprintId id = new TaskSprintId(taskId, sprintId);
        return taskSprintRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TaskSprint with id " + id + " not found"));
    }

    private TaskSprintResponse toResponse(TaskSprint taskSprint) {
        return TaskSprintResponse.builder()
                .taskId(taskSprint.getId().getTaskId())
                .sprintId(taskSprint.getId().getSprintId())
                .build();
    }
}



