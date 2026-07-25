package com.sonhoang2.dashboard_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponse {
    private DashboardStats stats;
    private List<ProjectSummary> recentProjects;
    private TaskDistribution taskDistribution;
}
