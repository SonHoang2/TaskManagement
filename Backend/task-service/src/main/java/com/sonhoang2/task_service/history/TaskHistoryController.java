package com.sonhoang2.task_service.history;

import com.sonhoang2.task_service.common.dto.JSendResponse;
import com.sonhoang2.task_service.common.dto.PageResponse;
import com.sonhoang2.task_service.history.dto.TaskHistoryCreateRequest;
import com.sonhoang2.task_service.history.dto.TaskHistoryResponse;
import com.sonhoang2.task_service.history.dto.TaskHistoryUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/task-histories")
public class TaskHistoryController {

    private final TaskHistoryService taskHistoryService;

    public TaskHistoryController(TaskHistoryService taskHistoryService) {
        this.taskHistoryService = taskHistoryService;
    }

    @PostMapping
    public ResponseEntity<JSendResponse<Map<String, TaskHistoryResponse>>> create(
            @Valid @RequestBody TaskHistoryCreateRequest request,
            @RequestHeader("X-User-Id") UUID currentUserId
    ) {
        TaskHistoryResponse history = taskHistoryService.create(request, currentUserId);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(history.getId())
                .toUri();

        return ResponseEntity.created(location)
                .body(JSendResponse.success(Map.of("history", history)));
    }

    @GetMapping
    public ResponseEntity<JSendResponse<Map<String, PageResponse<TaskHistoryResponse>>>> findAll(
            @RequestParam UUID taskId,
            @RequestParam(required = false) UUID changedBy,
            @RequestHeader("X-User-Id") UUID currentUserId,
            Pageable pageable
    ) {
        PageResponse<TaskHistoryResponse> pageResponse =
                taskHistoryService.findAll(taskId, changedBy, pageable, currentUserId);
        return ResponseEntity.ok(JSendResponse.success(Map.of("page", pageResponse)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<JSendResponse<Map<String, TaskHistoryResponse>>> findById(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID currentUserId
    ) {
        return ResponseEntity.ok(JSendResponse.success(Map.of("history",
                taskHistoryService.findById(id, currentUserId))));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<JSendResponse<Map<String, TaskHistoryResponse>>> update(
            @PathVariable UUID id,
            @Valid @RequestBody TaskHistoryUpdateRequest request,
            @RequestHeader("X-User-Id") UUID currentUserId
    ) {
        return ResponseEntity.ok(JSendResponse.success(Map.of("history",
                taskHistoryService.update(id, request, currentUserId))));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<JSendResponse<Void>> delete(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID currentUserId
    ) {
        taskHistoryService.delete(id, currentUserId);
        return ResponseEntity.ok(JSendResponse.success(null));
    }
}



