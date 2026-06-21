package com.sonhoang2.task_service.task;

import com.sonhoang2.task_service.common.dto.PageResponse;
import com.sonhoang2.task_service.common.exception.ResourceNotFoundException;
import com.sonhoang2.task_service.feign.ProjectServiceClient;
import com.sonhoang2.task_service.task.dto.TaskCreateRequest;
import com.sonhoang2.task_service.task.dto.TaskResponse;
import com.sonhoang2.task_service.task.dto.TaskUpdateRequest;
import com.sonhoang2.task_service.task.entity.Task;
import com.sonhoang2.task_service.task.entity.TaskStatus;
import feign.FeignException;
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
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final ProjectServiceClient projectServiceClient;
    private final ModelMapper modelMapper;

    private PageResponse<TaskResponse> toPageResponse(Page<Task> page) {
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
    public TaskResponse create(TaskCreateRequest request) {
        try {
            projectServiceClient.findById(request.getProjectId());
        } catch (FeignException.NotFound e) {
            throw new ResourceNotFoundException("Project not found: " + request.getProjectId());
        }

        Task task = modelMapper.map(request, Task.class);
        return toResponse(taskRepository.save(task));
    }

    @Override
    public PageResponse<TaskResponse> findAll(String status, String keyword, Pageable pageable) {
        Page<Task> page;

        if (status != null && keyword != null) {
            page = taskRepository.findByStatusAndTitleContainingIgnoreCase(TaskStatus.valueOf(status),
                    keyword,
                    pageable);
        } else if (status != null) {
            page = taskRepository.findByStatus(TaskStatus.valueOf(status), pageable);
        } else if (keyword != null) {
            page = taskRepository.findByTitleContainingIgnoreCase(keyword, pageable);
        } else {
            page = taskRepository.findAll(pageable);
        }

        return toPageResponse(page);
    }

    @Override
    @Transactional(readOnly = true)
    public TaskResponse findById(UUID id) {
        return toResponse(findTaskByIdOrThrow(id));
    }

    @Override
    public TaskResponse update(UUID id, TaskUpdateRequest request) {
        Task task = findTaskByIdOrThrow(id);
        modelMapper.map(request, task);
        return toResponse(task);
    }

    @Override
    public void delete(UUID id) {
        taskRepository.delete(findTaskByIdOrThrow(id));
    }

    private Task findTaskByIdOrThrow(UUID id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task with id " + id + " not found"));
    }

    private TaskResponse toResponse(Task task) {
        return TaskResponse.builder()
                .id(task.getId())
                .projectId(task.getProjectId())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus())
                .priority(task.getPriority())
                .assigneeId(task.getAssigneeId())
                .reporterId(task.getReporterId())
                .dueDate(task.getDueDate())
                .startDate(task.getStartDate())
                .parentTaskId(task.getParentTaskId())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }
}