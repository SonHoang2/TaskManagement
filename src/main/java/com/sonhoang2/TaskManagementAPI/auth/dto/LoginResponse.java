package com.sonhoang2.TaskManagementAPI.auth.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class LoginResponse {
    String accessToken;
    String tokenType;
    long expiresInMs;
}

