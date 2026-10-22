package com.sonhoang2.userservice.user;

import com.sonhoang2.userservice.auth.dto.RegisterRequest;
import com.sonhoang2.common.dto.PageResponse;
import com.sonhoang2.userservice.user.dto.AdminCreateUserRequest;
import com.sonhoang2.userservice.user.dto.AdminUpdateUserRequest;
import com.sonhoang2.userservice.user.dto.UserResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface UserService {

    UserResponse create(AdminCreateUserRequest request, UUID actorId);

    PageResponse<UserResponse> findAll(String keyword, Pageable pageable);

    UserResponse findById(UUID id);

    UserResponse update(UUID id, AdminUpdateUserRequest request, UUID actorId);

    void delete(UUID id, UUID actorId);

    UserResponse register(RegisterRequest request);

    UserResponse findByEmail(String email);
}

