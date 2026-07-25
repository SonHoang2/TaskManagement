package com.sonhoang2.dashboard_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskDistribution {
    private int todo;
    private int inProgress;
    private int done;
}
