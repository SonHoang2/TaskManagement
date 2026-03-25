package com.sonhoang2.TaskManagementAPI.user.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Builder
@Getter
public class UserResponse {

    private UUID id;
    private String fullName;
    private String email;
    private String avatarUrl;
    private Instant createdAt;
    private Instant updatedAt;
}

