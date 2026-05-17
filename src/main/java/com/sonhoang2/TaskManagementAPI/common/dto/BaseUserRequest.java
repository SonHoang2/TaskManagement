package com.sonhoang2.TaskManagementAPI.common.dto;

import lombok.Data;
import jakarta.validation.constraints.*;

@Data
public abstract class BaseUserRequest {
    @NotBlank(message = "fullName is required")
    private String fullName;

    @NotBlank(message = "email is required")
    @Email(message = "email is invalid")
    private String email;

    @NotBlank(message = "password is required")
    @Size(min = 8, message = "password must be at least 8 characters")
    private String password;
}