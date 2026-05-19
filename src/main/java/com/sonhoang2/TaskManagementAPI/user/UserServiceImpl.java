package com.sonhoang2.TaskManagementAPI.user;

import com.sonhoang2.TaskManagementAPI.auth.dto.RegisterRequest;
import com.sonhoang2.TaskManagementAPI.common.dto.BaseUserRequest;
import com.sonhoang2.TaskManagementAPI.common.dto.PageResponse;
import com.sonhoang2.TaskManagementAPI.common.exception.ResourceConflictException;
import com.sonhoang2.TaskManagementAPI.common.exception.ResourceNotFoundException;
import com.sonhoang2.TaskManagementAPI.user.dto.AdminCreateUserRequest;
import com.sonhoang2.TaskManagementAPI.user.dto.UserResponse;
import com.sonhoang2.TaskManagementAPI.user.dto.AdminUpdateUserRequest;
import com.sonhoang2.TaskManagementAPI.user.entity.User;
import com.sonhoang2.TaskManagementAPI.user.entity.UserRole;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ModelMapper modelMapper;

    private User persistUser(BaseUserRequest request, UserRole defaultRole, String avatarUrl) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ResourceConflictException("Email already exists");
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(normalizedEmail)
                .password(passwordEncoder.encode(request.getPassword()))
                .avatarUrl(avatarUrl)
                .role(defaultRole)
                .build();
        return userRepository.save(user);
    }

    // using for admin
    public UserResponse create(AdminCreateUserRequest request) {
        User user = persistUser(request, request.getRole(), request.getAvatarUrl());
        return toResponse(user);
    }

    // using for user
    public UserResponse register(RegisterRequest request) {
        User user = persistUser(request, UserRole.USER, null);
        return toResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> findAll(String keyword, Pageable pageable) {
        Page<User> page;

        if (StringUtils.hasText(keyword)) {
            page = userRepository.findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(keyword,
                    keyword,
                    pageable);
        } else {
            page = userRepository.findAll(pageable);
        }

        return new PageResponse<>(page.getContent().stream().map(this::toResponse).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext(),
                page.hasPrevious(),
                page.getNumberOfElements());
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse findById(UUID id) {
        return toResponse(findUserByIdOrThrow(id));
    }

    @Override
    public UserResponse update(UUID id, AdminUpdateUserRequest request) {
        User user = findUserByIdOrThrow(id);
        String normalizedEmail = normalizeEmail(request.getEmail());

        if (userRepository.existsByEmailIgnoreCaseAndIdNot(normalizedEmail, id)) {
            throw new ResourceConflictException("Email already exists");
        }

        modelMapper.map(request, user);
        user.setEmail(normalizedEmail);
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        return toResponse(user);
    }

    @Override
    public void delete(UUID id) {
        userRepository.delete(findUserByIdOrThrow(id));
    }

    private User findUserByIdOrThrow(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User with id " + id + " not found"));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}

