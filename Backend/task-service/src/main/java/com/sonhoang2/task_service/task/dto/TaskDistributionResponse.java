package com.sonhoang2.task_service.task.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskDistributionResponse {
    private int todo;
    private int inProgress;
    private int done;
}
