package com.sonhoang2.TaskManagementAPI.project.dto;

import com.sonhoang2.TaskManagementAPI.project.entity.ProjectInvitationStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class ProjectInvitationResponse {

    private UUID id;
    private UUID projectId;
    private UUID invitedById;
    private UUID inviteeId;
    private ProjectInvitationStatus status;
    private Instant createdAt;
    private Instant respondedAt;
}

