package com.sonhoang2.project_service.project.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InvitationDecisionRequest {

    @NotNull(message = "decision is required")
    private InvitationDecision decision;
}

