package com.sonhoang2.task_service.comment;

import com.sonhoang2.task_service.common.dto.JSendResponse;
import com.sonhoang2.task_service.common.dto.PageResponse;
import com.sonhoang2.task_service.comment.dto.TaskCommentCreateRequest;
import com.sonhoang2.task_service.comment.dto.TaskCommentResponse;
import com.sonhoang2.task_service.comment.dto.TaskCommentUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/task-comments")
public class TaskCommentController {

    private final TaskCommentService taskCommentService;

    public TaskCommentController(TaskCommentService taskCommentService) {
        this.taskCommentService = taskCommentService;
    }

    @PostMapping
    public ResponseEntity<JSendResponse<Map<String, TaskCommentResponse>>> create(
            @Valid @RequestBody TaskCommentCreateRequest request,
            @RequestHeader("X-User-Id") UUID currentUserId
    ) {
        TaskCommentResponse comment = taskCommentService.create(request, currentUserId);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(comment.getId())
                .toUri();

        return ResponseEntity.created(location)
                .body(JSendResponse.success(Map.of("comment", comment)));
    }

    @GetMapping
    public ResponseEntity<JSendResponse<Map<String, PageResponse<TaskCommentResponse>>>> findAll(
            @RequestParam UUID taskId,
            @RequestParam(required = false) UUID userId,
            @RequestHeader("X-User-Id") UUID currentUserId,
            Pageable pageable
    ) {
        PageResponse<TaskCommentResponse> pageResponse =
                taskCommentService.findAll(taskId, userId, pageable, currentUserId);
        return ResponseEntity.ok(JSendResponse.success(Map.of("page", pageResponse)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<JSendResponse<Map<String, TaskCommentResponse>>> findById(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID currentUserId
    ) {
        return ResponseEntity.ok(JSendResponse.success(Map.of("comment",
                taskCommentService.findById(id, currentUserId))));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<JSendResponse<Map<String, TaskCommentResponse>>> update(
            @PathVariable UUID id,
            @Valid @RequestBody TaskCommentUpdateRequest request,
            @RequestHeader("X-User-Id") UUID currentUserId
    ) {
        return ResponseEntity.ok(JSendResponse.success(Map.of("comment",
                taskCommentService.update(id, request, currentUserId))));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<JSendResponse<Void>> delete(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID currentUserId
    ) {
        taskCommentService.delete(id, currentUserId);
        return ResponseEntity.ok(JSendResponse.success(null));
    }
}



