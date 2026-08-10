package com.sonhoang2.task_service.task.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TaskStats {
    private int total;
    private int todo;
    private int inProgress;
    private int done;
}
