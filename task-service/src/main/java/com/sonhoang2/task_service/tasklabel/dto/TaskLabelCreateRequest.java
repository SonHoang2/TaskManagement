package com.sonhoang2.task_service.tasklabel.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class TaskLabelCreateRequest {

    @NotNull(message = "taskId is required")
    private UUID taskId;

    @NotNull(message = "labelId is required")
    private UUID labelId;
}



