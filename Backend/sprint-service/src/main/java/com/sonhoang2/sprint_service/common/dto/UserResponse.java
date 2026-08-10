package com.sonhoang2.sprint_service.common.dto;

import com.sonhoang2.sprint_service.common.enums.UserRole;
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
    private UserRole role;
    private Instant createdAt;
    private Instant updatedAt;
}

