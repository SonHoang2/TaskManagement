package com.sonhoang2.task_service.task.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class TaskAttachmentUpdateRequest {

    @NotNull(message = "taskId is required")
    private UUID taskId;

    @NotBlank(message = "fileUrl is required")
    @Size(max = 500, message = "fileUrl length must be <= 500")
    private String fileUrl;

    @Size(max = 255, message = "fileName length must be <= 255")
    private String fileName;

    private UUID uploadedBy;
}

