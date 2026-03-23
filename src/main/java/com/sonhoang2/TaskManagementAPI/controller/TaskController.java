package com.sonhoang2.TaskManagementAPI.controller;

import com.sonhoang2.TaskManagementAPI.dto.task.TaskCreateRequest;
import com.sonhoang2.TaskManagementAPI.dto.task.TaskResponse;
import com.sonhoang2.TaskManagementAPI.dto.task.TaskUpdateRequest;
import com.sonhoang2.TaskManagementAPI.service.TaskService;
import jakarta.validation.Valid;

import java.net.URI;

import com.sonhoang2.TaskManagementAPI.dto.common.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    public ResponseEntity<TaskResponse> create(@Valid @RequestBody TaskCreateRequest request) {
        TaskResponse createdTask = taskService.create(request);
        return ResponseEntity.created(URI.create("/tasks/" + createdTask.getId())).body(createdTask);
    }

    @GetMapping
    public PageResponse<TaskResponse> findAll(Pageable pageable) {
        return taskService.findAll(pageable);
    }

    @GetMapping("/{id}")
    public TaskResponse findById(@PathVariable Long id) {
        return taskService.findById(id);
    }

    @PutMapping("/{id}")
    public TaskResponse update(@PathVariable Long id, @Valid @RequestBody TaskUpdateRequest request) {
        return taskService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        taskService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

