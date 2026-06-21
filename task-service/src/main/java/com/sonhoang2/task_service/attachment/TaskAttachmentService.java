package com.sonhoang2.task_service.attachment;

import com.sonhoang2.task_service.common.dto.PageResponse;
import com.sonhoang2.task_service.attachment.dto.TaskAttachmentCreateRequest;
import com.sonhoang2.task_service.attachment.dto.TaskAttachmentResponse;
import com.sonhoang2.task_service.attachment.dto.TaskAttachmentUpdateRequest;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface TaskAttachmentService {

    TaskAttachmentResponse create(TaskAttachmentCreateRequest request);

    PageResponse<TaskAttachmentResponse> findAll(UUID taskId, UUID uploadedBy, Pageable pageable);

    TaskAttachmentResponse findById(UUID id);

    TaskAttachmentResponse update(UUID id, TaskAttachmentUpdateRequest request);

    void delete(UUID id);
}



