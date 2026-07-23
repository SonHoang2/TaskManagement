package com.sonhoang2.api_gateway.dashboard.dto;

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
