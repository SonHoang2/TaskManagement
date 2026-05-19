package com.sonhoang2.TaskManagementAPI.user.dto;

import com.sonhoang2.TaskManagementAPI.user.entity.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminUpdateUserRequest {

    @Size(max = 120, message = "fullName length must be <= 120")
    private String fullName;

    @Email(message = "email is invalid")
    @Size(max = 255, message = "email length must be <= 255")
    private String email;

    @Size(min = 8, max = 255, message = "password length must be between 8 and 255")
    private String password;

    @Size(max = 500, message = "avatarUrl length must be <= 500")
    private String avatarUrl;

    private UserRole role;
}

