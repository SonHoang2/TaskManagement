package com.sonhoang2.userservice.user;

import com.sonhoang2.userservice.auth.dto.RegisterRequest;
import com.sonhoang2.userservice.common.dto.PageResponse;
import com.sonhoang2.userservice.user.dto.AdminCreateUserRequest;
import com.sonhoang2.userservice.user.dto.AdminUpdateUserRequest;
import com.sonhoang2.userservice.user.dto.UserResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface UserService {

    UserResponse create(AdminCreateUserRequest request);

    PageResponse<UserResponse> findAll(String keyword, Pageable pageable);

    UserResponse findById(UUID id);

    UserResponse update(UUID id, AdminUpdateUserRequest request);

    void delete(UUID id);

    UserResponse register(RegisterRequest request);
}

