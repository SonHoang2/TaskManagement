package com.sonhoang2.TaskManagementAPI.user.dto;

import com.sonhoang2.TaskManagementAPI.common.dto.BaseUserRequest;
import com.sonhoang2.TaskManagementAPI.user.entity.UserRole;
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

