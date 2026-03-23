package com.sonhoang2.TaskManagementAPI.service;

import com.sonhoang2.TaskManagementAPI.dto.common.PageResponse;
import com.sonhoang2.TaskManagementAPI.exception.ResourceNotFoundException;
import com.sonhoang2.TaskManagementAPI.dto.task.TaskCreateRequest;
import com.sonhoang2.TaskManagementAPI.dto.task.TaskResponse;
import com.sonhoang2.TaskManagementAPI.dto.task.TaskUpdateRequest;
import com.sonhoang2.TaskManagementAPI.entity.Task;
import com.sonhoang2.TaskManagementAPI.repository.TaskRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;

    @Override
    public TaskResponse create(TaskCreateRequest request) {
        Task task = Task.builder().title(request.getTitle()).description(request.getDescription()).status(request.getStatus()).dueDate(request.getDueDate()).build();

        return toResponse(taskRepository.save(task));
    }

    public PageResponse<TaskResponse> findAll(Pageable pageable) {
        Page<Task> page = taskRepository.findAll(pageable);

        return new PageResponse<>(page.getContent().stream().map(this::toResponse).toList(), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages(), page.hasNext(), page.hasPrevious(), page.getNumberOfElements());
    }

    @Override
    @Transactional(readOnly = true)
    public TaskResponse findById(Long id) {
        return toResponse(findTaskByIdOrThrow(id));
    }

    @Override
    public TaskResponse update(Long id, TaskUpdateRequest request) {
        Task task = findTaskByIdOrThrow(id);

        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setStatus(request.getStatus());
        task.setDueDate(request.getDueDate());

        return toResponse(taskRepository.save(task));
    }

    @Override
    public void delete(Long id) {
        taskRepository.delete(findTaskByIdOrThrow(id));
    }

    private Task findTaskByIdOrThrow(Long id) {
        return taskRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Task with id " + id + " not found"));
    }

    private TaskResponse toResponse(Task task) {
        return TaskResponse.builder().id(task.getId()).title(task.getTitle()).description(task.getDescription()).status(task.getStatus()).dueDate(task.getDueDate()).createdAt(task.getCreatedAt()).updatedAt(task.getUpdatedAt()).build();
    }
}