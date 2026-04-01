package com.sonhoang2.TaskManagementAPI.project.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InvitationDecisionRequest {

    @NotNull(message = "decision is required")
    private InvitationDecision decision;
}

