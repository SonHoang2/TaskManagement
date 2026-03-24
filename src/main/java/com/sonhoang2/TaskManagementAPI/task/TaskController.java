package com.sonhoang2.TaskManagementAPI.task;

import com.sonhoang2.TaskManagementAPI.task.dto.TaskCreateRequest;
import com.sonhoang2.TaskManagementAPI.task.dto.TaskResponse;
import com.sonhoang2.TaskManagementAPI.task.dto.TaskUpdateRequest;
import jakarta.validation.Valid;

import java.net.URI;
import java.util.Map;

import com.sonhoang2.TaskManagementAPI.common.dto.JSendResponse;
import com.sonhoang2.TaskManagementAPI.common.dto.PageResponse;
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
    public ResponseEntity<JSendResponse<Map<String, TaskResponse>>> create(@Valid @RequestBody TaskCreateRequest request) {
        TaskResponse createdTask = taskService.create(request);
        return ResponseEntity.created(URI.create("/tasks/" + createdTask.getId()))
                .body(JSendResponse.success(Map.of("task", createdTask)));
    }

    @GetMapping
    public ResponseEntity<JSendResponse<Map<String, PageResponse<TaskResponse>>>> findAll(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            Pageable pageable) {
        PageResponse<TaskResponse> pageResponse = taskService.findAll(status, keyword, pageable);
        return ResponseEntity.ok(JSendResponse.success(Map.of("page", pageResponse)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<JSendResponse<Map<String, TaskResponse>>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(JSendResponse.success(Map.of("task", taskService.findById(id))));
    }

    @PutMapping("/{id}")
    public ResponseEntity<JSendResponse<Map<String, TaskResponse>>> update(
            @PathVariable Long id,
            @Valid @RequestBody TaskUpdateRequest request) {
        return ResponseEntity.ok(JSendResponse.success(Map.of("task", taskService.update(id, request))));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<JSendResponse<Void>> delete(@PathVariable Long id) {
        taskService.delete(id);
        return ResponseEntity.ok(JSendResponse.success(null));
    }
}

