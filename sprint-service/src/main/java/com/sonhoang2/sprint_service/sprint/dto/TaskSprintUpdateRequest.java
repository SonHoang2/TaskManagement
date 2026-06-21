package com.sonhoang2.sprint_service.sprint.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class TaskSprintUpdateRequest {

    private UUID taskId;

    private UUID sprintId;
}



