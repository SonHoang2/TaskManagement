package com.sonhoang2.TaskManagementAPI.attachment;

import com.sonhoang2.TaskManagementAPI.attachment.entity.TaskAttachment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TaskAttachmentRepository extends JpaRepository<TaskAttachment, UUID> {
    Page<TaskAttachment> findByTaskId(UUID taskId, Pageable pageable);

    Page<TaskAttachment> findByUploadedBy(UUID uploadedBy, Pageable pageable);

    Page<TaskAttachment> findByTaskIdAndUploadedBy(UUID taskId, UUID uploadedBy, Pageable pageable);
}



