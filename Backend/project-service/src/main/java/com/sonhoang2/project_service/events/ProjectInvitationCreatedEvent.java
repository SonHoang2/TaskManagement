package com.sonhoang2.project_service.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectInvitationCreatedEvent {
    private UUID invitationId;
    private UUID projectId;
    private String projectName;
    private UUID invitedById;
    private UUID inviteeId;
    private String eventType;
}
