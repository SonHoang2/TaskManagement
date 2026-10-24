package com.sonhoang2.task_service.task;

import com.sonhoang2.task_service.common.dto.JSendResponse;
import com.sonhoang2.task_service.common.dto.PageResponse;
import com.sonhoang2.task_service.task.dto.TaskCreateRequest;
import com.sonhoang2.task_service.task.dto.TaskDetailResponse;
import com.sonhoang2.task_service.task.dto.TaskDistributionResponse;
import com.sonhoang2.task_service.task.dto.TaskResponse;
import com.sonhoang2.task_service.task.dto.TaskStats;
import com.sonhoang2.task_service.task.dto.TaskUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    public ResponseEntity<JSendResponse<Map<String, TaskResponse>>> create(@Valid @RequestBody TaskCreateRequest request,
                                                                           @RequestHeader("X-User-Id") UUID userId) {
        TaskResponse createdTask = taskService.create(request, userId);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdTask.getId())
                .toUri();

        return ResponseEntity.created(location)
                .body(JSendResponse.success(Map.of("task", createdTask)));
    }

    @GetMapping
    public ResponseEntity<JSendResponse<Map<String, PageResponse<TaskResponse>>>> findAll(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestHeader("X-User-Id") UUID userId,
            Pageable pageable) {
        PageResponse<TaskResponse> pageResponse = taskService.findAll(status, keyword, pageable, userId);
        return ResponseEntity.ok(JSendResponse.success(Map.of("page", pageResponse)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<JSendResponse<Map<String, TaskResponse>>> findById(@PathVariable UUID id,
                                                                             @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(JSendResponse.success(Map.of("task", taskService.findById(id, userId))));
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<JSendResponse<Map<String, PageResponse<TaskDetailResponse>>>> findByProjectId(
            @PathVariable UUID projectId,
            @RequestHeader("X-User-Id") UUID userId,
            Pageable pageable) {
        PageResponse<TaskDetailResponse> tasks = taskService.findByProjectId(projectId, pageable, userId);
        return ResponseEntity.ok(JSendResponse.success(Map.of("page", tasks)));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<JSendResponse<Map<String, TaskResponse>>> update(
            @PathVariable UUID id,
            @Valid @RequestBody TaskUpdateRequest request,
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(JSendResponse.success(Map.of("task", taskService.update(id, request, userId))));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<JSendResponse<Void>> delete(@PathVariable UUID id,
                                                      @RequestHeader("X-User-Id") UUID userId) {
        taskService.delete(id, userId);
        return ResponseEntity.ok(JSendResponse.success(null));
    }

    @GetMapping("/distribution")
    public ResponseEntity<JSendResponse<Map<String, TaskDistributionResponse>>> getTaskDistribution(
            @RequestHeader("X-User-Id") UUID userId) {
        TaskDistributionResponse distribution = taskService.getTaskDistribution(userId);
        return ResponseEntity.ok(JSendResponse.success(Map.of("distribution", distribution)));
    }

    @GetMapping("/project/{projectId}/stats")
    public ResponseEntity<JSendResponse<Map<String, TaskStats>>> getTaskStatsByProjectId(
            @PathVariable UUID projectId,
            @RequestHeader("X-User-Id") UUID userId) {
        TaskStats stats = taskService.getTaskStatsByProjectId(projectId, userId);
        return ResponseEntity.ok(JSendResponse.success(Map.of("stats", stats)));
    }
}
