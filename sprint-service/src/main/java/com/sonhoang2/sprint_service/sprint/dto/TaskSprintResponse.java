package com.sonhoang2.sprint_service.sprint.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class TaskSprintResponse {

    private UUID taskId;
    private UUID sprintId;
}



