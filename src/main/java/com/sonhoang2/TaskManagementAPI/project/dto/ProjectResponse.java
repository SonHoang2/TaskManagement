package com.sonhoang2.TaskManagementAPI.project.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class ProjectResponse {

    private UUID id;
    private String name;
    private String description;
    private UUID ownerId;
    private Instant createdAt;
    private Instant updatedAt;
}

