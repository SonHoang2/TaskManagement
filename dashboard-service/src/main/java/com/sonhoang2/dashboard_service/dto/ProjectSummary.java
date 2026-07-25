package com.sonhoang2.dashboard_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectSummary {
    private UUID id;
    private String name;
    private String description;
    private int memberCount;
    private int taskCount;
    private String createdAt;
}
