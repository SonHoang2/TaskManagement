package com.sonhoang2.task_service.task.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class TaskHistoryCreateRequest {

    @NotNull(message = "taskId is required")
    private UUID taskId;

    @NotNull(message = "changedBy is required")
    private UUID changedBy;

    @Size(max = 50, message = "field length must be <= 50")
    private String field;

    private String oldValue;

    private String newValue;
}

