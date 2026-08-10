package com.sonhoang2.userservice.auth;

import com.sonhoang2.userservice.auth.dto.LoginRequest;
import com.sonhoang2.userservice.auth.dto.LoginResponse;
import com.sonhoang2.userservice.auth.dto.RegisterRequest;

public interface AuthService {

    LoginResponse login(LoginRequest request);

    LoginResponse signup(RegisterRequest request);

    void logout();
}

