package com.sonhoang2.task_service.tasklabel;

import com.sonhoang2.task_service.common.dto.JSendResponse;
import com.sonhoang2.task_service.common.dto.PageResponse;
import com.sonhoang2.task_service.tasklabel.dto.TaskLabelCreateRequest;
import com.sonhoang2.task_service.tasklabel.dto.TaskLabelResponse;
import com.sonhoang2.task_service.tasklabel.dto.TaskLabelUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/task-labels")
public class TaskLabelController {

    private final TaskLabelService taskLabelService;

    public TaskLabelController(TaskLabelService taskLabelService) {
        this.taskLabelService = taskLabelService;
    }

    @PostMapping
    public ResponseEntity<JSendResponse<Map<String, TaskLabelResponse>>> create(
            @Valid @RequestBody TaskLabelCreateRequest request,
            @RequestHeader("X-User-Id") UUID currentUserId
    ) {
        TaskLabelResponse taskLabel = taskLabelService.create(request, currentUserId);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{taskId}/{labelId}")
                .buildAndExpand(taskLabel.getTaskId(), taskLabel.getLabelId())
                .toUri();

        return ResponseEntity.created(location)
                .body(JSendResponse.success(Map.of("taskLabel", taskLabel)));
    }

    @GetMapping
    public ResponseEntity<JSendResponse<Map<String, PageResponse<TaskLabelResponse>>>> findAll(
            @RequestParam UUID taskId,
            @RequestParam(required = false) UUID labelId,
            @RequestHeader("X-User-Id") UUID currentUserId,
            Pageable pageable
    ) {
        PageResponse<TaskLabelResponse> pageResponse =
                taskLabelService.findAll(taskId, labelId, pageable, currentUserId);
        return ResponseEntity.ok(JSendResponse.success(Map.of("page", pageResponse)));
    }

    @GetMapping("/{taskId}/{labelId}")
    public ResponseEntity<JSendResponse<Map<String, TaskLabelResponse>>> findById(
            @PathVariable UUID taskId,
            @PathVariable UUID labelId,
            @RequestHeader("X-User-Id") UUID currentUserId
    ) {
        return ResponseEntity.ok(JSendResponse.success(Map.of("taskLabel",
                taskLabelService.findById(taskId, labelId, currentUserId))));
    }

    @PatchMapping("/{taskId}/{labelId}")
    public ResponseEntity<JSendResponse<Map<String, TaskLabelResponse>>> update(
            @PathVariable UUID taskId,
            @PathVariable UUID labelId,
            @Valid @RequestBody TaskLabelUpdateRequest request,
            @RequestHeader("X-User-Id") UUID currentUserId
    ) {
        return ResponseEntity.ok(JSendResponse.success(Map.of("taskLabel",
                taskLabelService.update(taskId, labelId, request, currentUserId))));
    }

    @DeleteMapping("/{taskId}/{labelId}")
    public ResponseEntity<JSendResponse<Void>> delete(
            @PathVariable UUID taskId,
            @PathVariable UUID labelId,
            @RequestHeader("X-User-Id") UUID currentUserId
    ) {
        taskLabelService.delete(taskId, labelId, currentUserId);
        return ResponseEntity.ok(JSendResponse.success(null));
    }
}



