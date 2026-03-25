package com.sonhoang2.TaskManagementAPI.auth;

import com.sonhoang2.TaskManagementAPI.auth.dto.LoginRequest;
import com.sonhoang2.TaskManagementAPI.auth.dto.LoginResponse;
import com.sonhoang2.TaskManagementAPI.user.dto.UserCreateRequest;

public interface AuthService {
    LoginResponse login(LoginRequest request);

    LoginResponse signup(UserCreateRequest request);

    void logout();
}

