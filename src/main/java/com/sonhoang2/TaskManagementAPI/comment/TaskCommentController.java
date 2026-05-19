package com.sonhoang2.TaskManagementAPI.comment;

import com.sonhoang2.TaskManagementAPI.common.dto.JSendResponse;
import com.sonhoang2.TaskManagementAPI.common.dto.PageResponse;
import com.sonhoang2.TaskManagementAPI.comment.dto.TaskCommentCreateRequest;
import com.sonhoang2.TaskManagementAPI.comment.dto.TaskCommentResponse;
import com.sonhoang2.TaskManagementAPI.comment.dto.TaskCommentUpdateRequest;
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
            @Valid @RequestBody TaskCommentCreateRequest request
    ) {
        TaskCommentResponse comment = taskCommentService.create(request);
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
            @RequestParam(required = false) UUID taskId,
            @RequestParam(required = false) UUID userId,
            Pageable pageable
    ) {
        PageResponse<TaskCommentResponse> pageResponse = taskCommentService.findAll(taskId, userId, pageable);
        return ResponseEntity.ok(JSendResponse.success(Map.of("page", pageResponse)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<JSendResponse<Map<String, TaskCommentResponse>>> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(JSendResponse.success(Map.of("comment", taskCommentService.findById(id))));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<JSendResponse<Map<String, TaskCommentResponse>>> update(
            @PathVariable UUID id,
            @Valid @RequestBody TaskCommentUpdateRequest request
    ) {
        return ResponseEntity.ok(JSendResponse.success(Map.of("comment", taskCommentService.update(id, request))));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<JSendResponse<Void>> delete(@PathVariable UUID id) {
        taskCommentService.delete(id);
        return ResponseEntity.ok(JSendResponse.success(null));
    }
}



