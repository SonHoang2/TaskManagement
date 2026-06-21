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
            @Valid @RequestBody TaskAttachmentCreateRequest request
    ) {
        TaskAttachmentResponse attachment = taskAttachmentService.create(request);
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
            @RequestParam(required = false) UUID taskId,
            @RequestParam(required = false) UUID uploadedBy,
            Pageable pageable
    ) {
        PageResponse<TaskAttachmentResponse> pageResponse = taskAttachmentService.findAll(taskId, uploadedBy, pageable);
        return ResponseEntity.ok(JSendResponse.success(Map.of("page", pageResponse)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<JSendResponse<Map<String, TaskAttachmentResponse>>> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(JSendResponse.success(Map.of("attachment", taskAttachmentService.findById(id))));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<JSendResponse<Map<String, TaskAttachmentResponse>>> update(
            @PathVariable UUID id,
            @Valid @RequestBody TaskAttachmentUpdateRequest request
    ) {
        return ResponseEntity.ok(JSendResponse.success(Map.of("attachment",
                taskAttachmentService.update(id, request))));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<JSendResponse<Void>> delete(@PathVariable UUID id) {
        taskAttachmentService.delete(id);
        return ResponseEntity.ok(JSendResponse.success(null));
    }
}



