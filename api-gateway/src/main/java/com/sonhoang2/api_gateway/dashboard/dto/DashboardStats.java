package com.sonhoang2.api_gateway.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStats {
    private int totalProjects;
    private int totalTasks;
    private int totalUsers;
    private int activeSprints;
    private int completedTasks;
    private int inProgressTasks;
    private int todoTasks;
}
