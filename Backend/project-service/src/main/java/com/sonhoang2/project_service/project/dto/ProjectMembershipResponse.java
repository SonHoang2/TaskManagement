package com.sonhoang2.project_service.project.dto;

import com.sonhoang2.project_service.project.entity.ProjectMemberRole;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class ProjectMembershipResponse {

    private UUID projectId;
    private ProjectMemberRole role;
}
