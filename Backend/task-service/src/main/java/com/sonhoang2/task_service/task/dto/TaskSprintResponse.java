package com.sonhoang2.task_service.task.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class TaskSprintResponse {

    private UUID taskId;
    private UUID sprintId;
}

