package com.sonhoang2.TaskManagementAPI.sprint.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class TaskSprintUpdateRequest {

    private UUID taskId;

    private UUID sprintId;
}



