package com.sonhoang2.userservice.auth.dto;

import com.sonhoang2.userservice.user.dto.UserResponse;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponse {
    private String accessToken;
    private String tokenType;
    private Long expiresInMs;
    private UserResponse user;
}