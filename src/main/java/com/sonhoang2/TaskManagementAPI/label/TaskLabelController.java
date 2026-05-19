package com.sonhoang2.TaskManagementAPI.label;

import com.sonhoang2.TaskManagementAPI.common.dto.JSendResponse;
import com.sonhoang2.TaskManagementAPI.common.dto.PageResponse;
import com.sonhoang2.TaskManagementAPI.label.dto.TaskLabelCreateRequest;
import com.sonhoang2.TaskManagementAPI.label.dto.TaskLabelResponse;
import com.sonhoang2.TaskManagementAPI.label.dto.TaskLabelUpdateRequest;
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
            @Valid @RequestBody TaskLabelCreateRequest request
    ) {
        TaskLabelResponse taskLabel = taskLabelService.create(request);
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
            @RequestParam(required = false) UUID taskId,
            @RequestParam(required = false) UUID labelId,
            Pageable pageable
    ) {
        PageResponse<TaskLabelResponse> pageResponse = taskLabelService.findAll(taskId, labelId, pageable);
        return ResponseEntity.ok(JSendResponse.success(Map.of("page", pageResponse)));
    }

    @GetMapping("/{taskId}/{labelId}")
    public ResponseEntity<JSendResponse<Map<String, TaskLabelResponse>>> findById(
            @PathVariable UUID taskId,
            @PathVariable UUID labelId
    ) {
        return ResponseEntity.ok(JSendResponse.success(Map.of("taskLabel", taskLabelService.findById(taskId, labelId))));
    }

    @PatchMapping("/{taskId}/{labelId}")
    public ResponseEntity<JSendResponse<Map<String, TaskLabelResponse>>> update(
            @PathVariable UUID taskId,
            @PathVariable UUID labelId,
            @Valid @RequestBody TaskLabelUpdateRequest request
    ) {
        return ResponseEntity.ok(JSendResponse.success(Map.of("taskLabel", taskLabelService.update(taskId, labelId, request))));
    }

    @DeleteMapping("/{taskId}/{labelId}")
    public ResponseEntity<JSendResponse<Void>> delete(
            @PathVariable UUID taskId,
            @PathVariable UUID labelId
    ) {
        taskLabelService.delete(taskId, labelId);
        return ResponseEntity.ok(JSendResponse.success(null));
    }
}



