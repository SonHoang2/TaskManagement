package com.sonhoang2.project_service.project.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class InviteMemberRequest {

    @NotNull(message = "userId is required")
    private UUID userId;
}

