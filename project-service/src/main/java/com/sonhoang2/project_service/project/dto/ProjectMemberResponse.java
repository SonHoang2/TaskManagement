package com.sonhoang2.project_service.project.dto;

import com.sonhoang2.project_service.project.entity.ProjectMemberRole;
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

