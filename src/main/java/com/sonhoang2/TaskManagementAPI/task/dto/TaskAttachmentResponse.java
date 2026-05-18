package com.sonhoang2.TaskManagementAPI.task.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class TaskAttachmentResponse {

    private UUID id;
    private UUID taskId;
    private String fileUrl;
    private String fileName;
    private UUID uploadedBy;
    private Instant createdAt;
}

