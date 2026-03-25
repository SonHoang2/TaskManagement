package com.sonhoang2.TaskManagementAPI.user;

import com.sonhoang2.TaskManagementAPI.common.dto.PageResponse;
import com.sonhoang2.TaskManagementAPI.user.dto.UserCreateRequest;
import com.sonhoang2.TaskManagementAPI.user.dto.UserResponse;
import com.sonhoang2.TaskManagementAPI.user.dto.UserUpdateRequest;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface UserService {

    UserResponse create(UserCreateRequest request);

    PageResponse<UserResponse> findAll(String keyword, Pageable pageable);

    UserResponse findById(UUID id);

    UserResponse update(UUID id, UserUpdateRequest request);

    void delete(UUID id);
}

