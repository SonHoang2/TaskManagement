package com.sonhoang2.task_service.tasklabel.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class TaskLabelResponse {

    private UUID taskId;
    private UUID labelId;
}



