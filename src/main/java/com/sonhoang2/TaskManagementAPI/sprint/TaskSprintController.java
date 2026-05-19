package com.sonhoang2.TaskManagementAPI.sprint;

import com.sonhoang2.TaskManagementAPI.common.dto.JSendResponse;
import com.sonhoang2.TaskManagementAPI.common.dto.PageResponse;
import com.sonhoang2.TaskManagementAPI.sprint.dto.TaskSprintCreateRequest;
import com.sonhoang2.TaskManagementAPI.sprint.dto.TaskSprintResponse;
import com.sonhoang2.TaskManagementAPI.sprint.dto.TaskSprintUpdateRequest;
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
            @Valid @RequestBody TaskSprintCreateRequest request
    ) {
        TaskSprintResponse taskSprint = taskSprintService.create(request);
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
            Pageable pageable
    ) {
        PageResponse<TaskSprintResponse> pageResponse = taskSprintService.findAll(taskId, sprintId, pageable);
        return ResponseEntity.ok(JSendResponse.success(Map.of("page", pageResponse)));
    }

    @GetMapping("/{taskId}/{sprintId}")
    public ResponseEntity<JSendResponse<Map<String, TaskSprintResponse>>> findById(
            @PathVariable UUID taskId,
            @PathVariable UUID sprintId
    ) {
        return ResponseEntity.ok(JSendResponse.success(Map.of("taskSprint", taskSprintService.findById(taskId, sprintId))));
    }

    @PatchMapping("/{taskId}/{sprintId}")
    public ResponseEntity<JSendResponse<Map<String, TaskSprintResponse>>> update(
            @PathVariable UUID taskId,
            @PathVariable UUID sprintId,
            @Valid @RequestBody TaskSprintUpdateRequest request
    ) {
        return ResponseEntity.ok(JSendResponse.success(Map.of("taskSprint", taskSprintService.update(taskId, sprintId, request))));
    }

    @DeleteMapping("/{taskId}/{sprintId}")
    public ResponseEntity<JSendResponse<Void>> delete(
            @PathVariable UUID taskId,
            @PathVariable UUID sprintId
    ) {
        taskSprintService.delete(taskId, sprintId);
        return ResponseEntity.ok(JSendResponse.success(null));
    }
}



