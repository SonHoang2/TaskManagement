package com.sonhoang2.task_service.attachment;

import com.sonhoang2.task_service.attachment.dto.TaskAttachmentCreateRequest;
import com.sonhoang2.task_service.attachment.dto.TaskAttachmentResponse;
import com.sonhoang2.task_service.attachment.dto.TaskAttachmentUpdateRequest;
import com.sonhoang2.task_service.common.dto.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface TaskAttachmentService {

    TaskAttachmentResponse create(TaskAttachmentCreateRequest request, UUID currentUserId);

    PageResponse<TaskAttachmentResponse> findAll(UUID taskId, UUID uploadedBy, Pageable pageable, UUID currentUserId);

    TaskAttachmentResponse findById(UUID id, UUID currentUserId);

    TaskAttachmentResponse update(UUID id, TaskAttachmentUpdateRequest request, UUID currentUserId);

    void delete(UUID id, UUID currentUserId);
}



