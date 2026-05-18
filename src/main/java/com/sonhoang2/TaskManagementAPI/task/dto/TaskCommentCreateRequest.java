package com.sonhoang2.TaskManagementAPI.task.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class TaskCommentCreateRequest {

    @NotNull(message = "taskId is required")
    private UUID taskId;

    @NotNull(message = "userId is required")
    private UUID userId;

    @NotBlank(message = "content is required")
    private String content;
}

