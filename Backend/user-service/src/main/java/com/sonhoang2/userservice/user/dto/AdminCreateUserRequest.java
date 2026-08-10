package com.sonhoang2.userservice.user.dto;

import com.sonhoang2.userservice.common.dto.BaseUserRequest;
import com.sonhoang2.userservice.user.entity.UserRole;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AdminCreateUserRequest extends BaseUserRequest {
    private String avatarUrl;
    private UserRole role;
}

