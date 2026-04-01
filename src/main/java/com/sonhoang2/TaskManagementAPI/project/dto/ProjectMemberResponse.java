package com.sonhoang2.TaskManagementAPI.project.dto;

import com.sonhoang2.TaskManagementAPI.project.entity.ProjectMemberRole;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class ProjectMemberResponse {

    private UUID userId;
    private ProjectMemberRole role;
    private Instant joinedAt;
}

