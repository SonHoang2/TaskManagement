package com.sonhoang2.TaskManagementAPI.history;

import com.sonhoang2.TaskManagementAPI.common.dto.JSendResponse;
import com.sonhoang2.TaskManagementAPI.common.dto.PageResponse;
import com.sonhoang2.TaskManagementAPI.history.dto.TaskHistoryCreateRequest;
import com.sonhoang2.TaskManagementAPI.history.dto.TaskHistoryResponse;
import com.sonhoang2.TaskManagementAPI.history.dto.TaskHistoryUpdateRequest;
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
            @Valid @RequestBody TaskHistoryCreateRequest request
    ) {
        TaskHistoryResponse history = taskHistoryService.create(request);
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
            @RequestParam(required = false) UUID taskId,
            @RequestParam(required = false) UUID changedBy,
            Pageable pageable
    ) {
        PageResponse<TaskHistoryResponse> pageResponse = taskHistoryService.findAll(taskId, changedBy, pageable);
        return ResponseEntity.ok(JSendResponse.success(Map.of("page", pageResponse)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<JSendResponse<Map<String, TaskHistoryResponse>>> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(JSendResponse.success(Map.of("history", taskHistoryService.findById(id))));
    }

    @PutMapping("/{id}")
    public ResponseEntity<JSendResponse<Map<String, TaskHistoryResponse>>> update(
            @PathVariable UUID id,
            @Valid @RequestBody TaskHistoryUpdateRequest request
    ) {
        return ResponseEntity.ok(JSendResponse.success(Map.of("history", taskHistoryService.update(id, request))));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<JSendResponse<Void>> delete(@PathVariable UUID id) {
        taskHistoryService.delete(id);
        return ResponseEntity.ok(JSendResponse.success(null));
    }
}



