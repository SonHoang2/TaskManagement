package com.sonhoang2.sprint_service.task_sprints.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class TaskSprintUpdateRequest {

    private UUID taskId;

    private UUID sprintId;
}



