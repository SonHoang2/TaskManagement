package com.sonhoang2.sprint_service.task_sprints;

import com.sonhoang2.sprint_service.common.dto.JSendResponse;
import com.sonhoang2.sprint_service.common.dto.PageResponse;
import com.sonhoang2.sprint_service.task_sprints.dto.TaskSprintCreateRequest;
import com.sonhoang2.sprint_service.task_sprints.dto.TaskSprintResponse;
import com.sonhoang2.sprint_service.task_sprints.dto.TaskSprintUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/task-sprints")
public class TaskSprintController {

    private final TaskSprintService taskSprintService;

    public TaskSprintController(TaskSprintService taskSprintService) {
        this.taskSprintService = taskSprintService;
    }

    @PostMapping
    public ResponseEntity<JSendResponse<Map<String, TaskSprintResponse>>> create(
            @Valid @RequestBody TaskSprintCreateRequest request,
            @RequestHeader("X-User-Id") UUID userId
    ) {
        TaskSprintResponse taskSprint = taskSprintService.create(request, userId);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{taskId}/{sprintId}")
                .buildAndExpand(taskSprint.getTaskId(), taskSprint.getSprintId())
                .toUri();

        return ResponseEntity.created(location)
                .body(JSendResponse.success(Map.of("taskSprint", taskSprint)));
    }

    @GetMapping
    public ResponseEntity<JSendResponse<Map<String, PageResponse<TaskSprintResponse>>>> findAll(
            @RequestParam(required = false) UUID taskId,
            @RequestParam(required = false) UUID sprintId,
            @RequestHeader("X-User-Id") UUID userId,
            Pageable pageable
    ) {
        PageResponse<TaskSprintResponse> pageResponse = taskSprintService.findAll(taskId, sprintId, pageable, userId);
        return ResponseEntity.ok(JSendResponse.success(Map.of("page", pageResponse)));
    }

    @GetMapping("/{taskId}/{sprintId}")
    public ResponseEntity<JSendResponse<Map<String, TaskSprintResponse>>> findById(
            @PathVariable UUID taskId,
            @PathVariable UUID sprintId,
            @RequestHeader("X-User-Id") UUID userId
    ) {
        return ResponseEntity.ok(JSendResponse.success(Map.of("taskSprint",
                taskSprintService.findById(taskId, sprintId, userId))));
    }

    @PatchMapping("/{taskId}/{sprintId}")
    public ResponseEntity<JSendResponse<Map<String, TaskSprintResponse>>> update(
            @PathVariable UUID taskId,
            @PathVariable UUID sprintId,
            @Valid @RequestBody TaskSprintUpdateRequest request,
            @RequestHeader("X-User-Id") UUID userId
    ) {
        return ResponseEntity.ok(JSendResponse.success(Map.of("taskSprint",
                taskSprintService.update(taskId, sprintId, request, userId))));
    }

    @DeleteMapping("/{taskId}/{sprintId}")
    public ResponseEntity<JSendResponse<Void>> delete(
            @PathVariable UUID taskId,
            @PathVariable UUID sprintId,
            @RequestHeader("X-User-Id") UUID userId
    ) {
        taskSprintService.delete(taskId, sprintId, userId);
        return ResponseEntity.ok(JSendResponse.success(null));
    }
}



