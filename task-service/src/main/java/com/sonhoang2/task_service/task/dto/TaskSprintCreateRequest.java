package com.sonhoang2.task_service.task.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class TaskSprintCreateRequest {

    @NotNull(message = "taskId is required")
    private UUID taskId;

    @NotNull(message = "sprintId is required")
    private UUID sprintId;
}

