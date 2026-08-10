package com.sonhoang2.sprint_service.task_sprints.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class TaskSprintResponse {

    private UUID taskId;
    private UUID sprintId;
}



