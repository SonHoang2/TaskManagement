package com.sonhoang2.TaskManagementAPI.controller;

import com.sonhoang2.TaskManagementAPI.dto.common.ApiSuccessResponse;
import com.sonhoang2.TaskManagementAPI.dto.common.PageResponse;
import com.sonhoang2.TaskManagementAPI.dto.task.TaskCreateRequest;
import com.sonhoang2.TaskManagementAPI.dto.task.TaskResponse;
import com.sonhoang2.TaskManagementAPI.dto.task.TaskUpdateRequest;
import com.sonhoang2.TaskManagementAPI.service.TaskService;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;

import java.net.URI;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    public ResponseEntity<ApiSuccessResponse<TaskResponse>> create(
            @Valid @RequestBody TaskCreateRequest request,
            HttpServletRequest httpRequest) {
        TaskResponse createdTask = taskService.create(request);
        return ResponseEntity.created(URI.create("/tasks/" + createdTask.getId()))
                .body(ApiSuccessResponse.success(
                        "Created successfully",
                        createdTask,
                        httpRequest.getRequestURI()
                ));
    }

    @GetMapping
    public ApiSuccessResponse<PageResponse<TaskResponse>> findAll(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            Pageable pageable,
            HttpServletRequest httpRequest) {
        return ApiSuccessResponse.success(
                "Success",
                taskService.findAll(status, keyword, pageable),
                httpRequest.getRequestURI()
        );
    }

    @GetMapping("/{id}")
    public ApiSuccessResponse<TaskResponse> findById(@PathVariable Long id, HttpServletRequest httpRequest) {
        return ApiSuccessResponse.success(
                "Success",
                taskService.findById(id),
                httpRequest.getRequestURI()
        );
    }

    @PutMapping("/{id}")
    public ApiSuccessResponse<TaskResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody TaskUpdateRequest request,
            HttpServletRequest httpRequest) {
        return ApiSuccessResponse.success(
                "Updated successfully",
                taskService.update(id, request),
                httpRequest.getRequestURI()
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiSuccessResponse<Void>> delete(@PathVariable Long id, HttpServletRequest httpRequest) {
        taskService.delete(id);
        return ResponseEntity.ok(
                ApiSuccessResponse.success("Deleted successfully", null, httpRequest.getRequestURI())
        );
    }
}

