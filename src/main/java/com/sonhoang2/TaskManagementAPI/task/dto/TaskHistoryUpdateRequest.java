package com.sonhoang2.TaskManagementAPI.task.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class TaskHistoryUpdateRequest {

    @NotNull(message = "taskId is required")
    private UUID taskId;

    @NotNull(message = "changedBy is required")
    private UUID changedBy;

    @Size(max = 50, message = "field length must be <= 50")
    private String field;

    private String oldValue;

    private String newValue;
}

