package com.sonhoang2.task_service.attachment;

import com.sonhoang2.task_service.common.dto.JSendResponse;
import com.sonhoang2.task_service.common.dto.PageResponse;
import com.sonhoang2.task_service.attachment.dto.TaskAttachmentCreateRequest;
import com.sonhoang2.task_service.attachment.dto.TaskAttachmentResponse;
import com.sonhoang2.task_service.attachment.dto.TaskAttachmentUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/task-attachments")
public class TaskAttachmentController {

    private final TaskAttachmentService taskAttachmentService;

    public TaskAttachmentController(TaskAttachmentService taskAttachmentService) {
        this.taskAttachmentService = taskAttachmentService;
    }

    @PostMapping
    public ResponseEntity<JSendResponse<Map<String, TaskAttachmentResponse>>> create(
            @Valid @RequestBody TaskAttachmentCreateRequest request,
            @RequestHeader("X-User-Id") UUID currentUserId
    ) {
        TaskAttachmentResponse attachment = taskAttachmentService.create(request, currentUserId);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(attachment.getId())
                .toUri();

        return ResponseEntity.created(location)
                .body(JSendResponse.success(Map.of("attachment", attachment)));
    }

    @GetMapping
    public ResponseEntity<JSendResponse<Map<String, PageResponse<TaskAttachmentResponse>>>> findAll(
            @RequestParam UUID taskId,
            @RequestParam(required = false) UUID uploadedBy,
            @RequestHeader("X-User-Id") UUID currentUserId,
            Pageable pageable
    ) {
        PageResponse<TaskAttachmentResponse> pageResponse =
                taskAttachmentService.findAll(taskId, uploadedBy, pageable, currentUserId);
        return ResponseEntity.ok(JSendResponse.success(Map.of("page", pageResponse)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<JSendResponse<Map<String, TaskAttachmentResponse>>> findById(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID currentUserId
    ) {
        return ResponseEntity.ok(JSendResponse.success(Map.of("attachment",
                taskAttachmentService.findById(id, currentUserId))));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<JSendResponse<Map<String, TaskAttachmentResponse>>> update(
            @PathVariable UUID id,
            @Valid @RequestBody TaskAttachmentUpdateRequest request,
            @RequestHeader("X-User-Id") UUID currentUserId
    ) {
        return ResponseEntity.ok(JSendResponse.success(Map.of("attachment",
                taskAttachmentService.update(id, request, currentUserId))));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<JSendResponse<Void>> delete(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID currentUserId
    ) {
        taskAttachmentService.delete(id, currentUserId);
        return ResponseEntity.ok(JSendResponse.success(null));
    }
}



