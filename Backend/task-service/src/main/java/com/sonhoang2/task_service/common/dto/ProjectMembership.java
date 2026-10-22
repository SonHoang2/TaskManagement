package com.sonhoang2.task_service.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectMembership {
    private UUID projectId;
    /** OWNER, ADMIN or MEMBER, as defined by project-service. */
    private String role;
}
