package com.sonhoang2.TaskManagementAPI.service;

import com.sonhoang2.TaskManagementAPI.exception.ResourceNotFoundException;
import com.sonhoang2.TaskManagementAPI.dto.TaskCreateRequest;
import com.sonhoang2.TaskManagementAPI.dto.TaskResponse;
import com.sonhoang2.TaskManagementAPI.dto.TaskUpdateRequest;
import com.sonhoang2.TaskManagementAPI.entity.Task;
import com.sonhoang2.TaskManagementAPI.repository.TaskRepository;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;

    public TaskServiceImpl(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Override
    public TaskResponse create(TaskCreateRequest request) {
        Task task = new Task();
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setStatus(request.getStatus());
        task.setDueDate(request.getDueDate());

        Task savedTask = taskRepository.save(task);
        return toResponse(savedTask);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskResponse> findAll() {
        return taskRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
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

        Task updatedTask = taskRepository.save(task);
        return toResponse(updatedTask);
    }

    @Override
    public void delete(Long id) {
        Task task = findTaskByIdOrThrow(id);
        taskRepository.delete(task);
    }

    private Task findTaskByIdOrThrow(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task with id " + id + " not found"));
    }

    private TaskResponse toResponse(Task task) {
        return TaskResponse.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus())
                .dueDate(task.getDueDate())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }
}

