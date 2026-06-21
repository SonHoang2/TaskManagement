package com.sonhoang2.task_service.tasklabel.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class TaskLabelUpdateRequest {

    private UUID taskId;

    private UUID labelId;
}



