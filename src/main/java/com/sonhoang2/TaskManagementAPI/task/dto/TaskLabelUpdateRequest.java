package com.sonhoang2.TaskManagementAPI.task.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class TaskLabelUpdateRequest {

    @NotNull(message = "taskId is required")
    private UUID taskId;

    @NotNull(message = "labelId is required")
    private UUID labelId;
}

