package com.sonhoang2.task_service.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class TaskCommentCreateRequest {

    @NotNull(message = "taskId is required")
    private UUID taskId;

    @NotBlank(message = "content is required")
    private String content;
}



