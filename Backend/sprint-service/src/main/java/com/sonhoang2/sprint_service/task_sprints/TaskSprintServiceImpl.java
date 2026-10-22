package com.sonhoang2.sprint_service.task_sprints;

import com.sonhoang2.sprint_service.common.dto.PageResponse;
import com.sonhoang2.sprint_service.common.dto.TaskSummary;
import com.sonhoang2.sprint_service.common.exception.ResourceConflictException;
import com.sonhoang2.sprint_service.common.exception.ResourceNotFoundException;
import com.sonhoang2.sprint_service.common.security.ProjectAccessGuard;
import com.sonhoang2.sprint_service.feign.TaskServiceClient;
import com.sonhoang2.sprint_service.sprints.SprintRepository;
import com.sonhoang2.sprint_service.sprints.entity.Sprint;
import com.sonhoang2.sprint_service.task_sprints.dto.TaskSprintCreateRequest;
import com.sonhoang2.sprint_service.task_sprints.dto.TaskSprintResponse;
import com.sonhoang2.sprint_service.task_sprints.dto.TaskSprintUpdateRequest;
import com.sonhoang2.sprint_service.task_sprints.entity.TaskSprint;
import com.sonhoang2.sprint_service.task_sprints.entity.TaskSprintId;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class TaskSprintServiceImpl implements TaskSprintService {

    private final TaskSprintRepository taskSprintRepository;
    private final SprintRepository sprintRepository;
    private final ProjectAccessGuard projectAccessGuard;
    private final TaskServiceClient taskServiceClient;

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
    public TaskSprintResponse create(TaskSprintCreateRequest request, UUID userId) {
        requireLinkable(request.getTaskId(), request.getSprintId(), userId);

        TaskSprintId id = new TaskSprintId(request.getTaskId(), request.getSprintId());
        TaskSprint taskSprint = TaskSprint.builder().id(id).build();
        return toResponse(taskSprintRepository.save(taskSprint));
    }

    @Override
    public PageResponse<TaskSprintResponse> findAll(UUID taskId, UUID sprintId, Pageable pageable, UUID userId) {
        Page<TaskSprint> page;

        if (sprintId != null) {
            requireSprintMember(sprintId, userId);
            page = taskId != null
                    ? taskSprintRepository.findByIdTaskIdAndIdSprintId(taskId, sprintId, pageable)
                    : taskSprintRepository.findByIdSprintId(sprintId, pageable);
        } else if (taskId != null) {
            page = taskSprintRepository.findByIdTaskId(taskId, pageable);
            // A task's sprints live in the task's project, so checking them covers the task too.
            page.getContent().stream()
                    .map(taskSprint -> taskSprint.getSprint().getProjectId())
                    .distinct()
                    .forEach(projectId -> projectAccessGuard.requireMember(projectId, userId));
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "taskId or sprintId is required");
        }

        return toPageResponse(page);
    }

    @Override
    @Transactional(readOnly = true)
    public TaskSprintResponse findById(UUID taskId, UUID sprintId, UUID userId) {
        requireSprintMember(sprintId, userId);
        return toResponse(findTaskSprintByIdOrThrow(taskId, sprintId));
    }

    @Override
    public TaskSprintResponse update(UUID taskId, UUID sprintId, TaskSprintUpdateRequest request, UUID userId) {
        requireSprintMember(sprintId, userId);
        TaskSprint existing = findTaskSprintByIdOrThrow(taskId, sprintId);
        TaskSprintId newId = new TaskSprintId(
                request.getTaskId() != null ? request.getTaskId() : taskId,
                request.getSprintId() != null ? request.getSprintId() : sprintId);

        if (!existing.getId().equals(newId)) {
            requireLinkable(newId.getTaskId(), newId.getSprintId(), userId);
            taskSprintRepository.delete(existing);
            TaskSprint replacement = TaskSprint.builder().id(newId).build();
            return toResponse(taskSprintRepository.save(replacement));
        }

        return toResponse(existing);
    }

    @Override
    public void delete(UUID taskId, UUID sprintId, UUID userId) {
        requireSprintMember(sprintId, userId);
        taskSprintRepository.delete(findTaskSprintByIdOrThrow(taskId, sprintId));
    }

    private Sprint requireSprintMember(UUID sprintId, UUID userId) {
        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new ResourceNotFoundException("Sprint with id " + sprintId + " not found"));
        projectAccessGuard.requireMember(sprint.getProjectId(), userId);
        return sprint;
    }

    /** The user must see both the sprint and the task, and both must belong to the same project. */
    private void requireLinkable(UUID taskId, UUID sprintId, UUID userId) {
        Sprint sprint = requireSprintMember(sprintId, userId);
        TaskSummary task = taskServiceClient.findById(taskId, userId).data().get("task");
        if (!sprint.getProjectId().equals(task.getProjectId())) {
            throw new ResourceConflictException("Task and sprint belong to different projects");
        }
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



