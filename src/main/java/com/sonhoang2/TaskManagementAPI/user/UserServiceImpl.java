package com.sonhoang2.TaskManagementAPI.user;

import com.sonhoang2.TaskManagementAPI.common.dto.PageResponse;
import com.sonhoang2.TaskManagementAPI.common.exception.ResourceConflictException;
import com.sonhoang2.TaskManagementAPI.common.exception.ResourceNotFoundException;
import com.sonhoang2.TaskManagementAPI.user.dto.UserCreateRequest;
import com.sonhoang2.TaskManagementAPI.user.dto.UserResponse;
import com.sonhoang2.TaskManagementAPI.user.dto.UserUpdateRequest;
import com.sonhoang2.TaskManagementAPI.user.entity.User;
import lombok.RequiredArgsConstructor;
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

    @Override
    public UserResponse create(UserCreateRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ResourceConflictException("Email already exists");
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(normalizedEmail)
                .password(passwordEncoder.encode(request.getPassword()))
                .avatarUrl(request.getAvatarUrl())
                .build();

        return toResponse(userRepository.save(user));
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
    public UserResponse update(UUID id, UserUpdateRequest request) {
        User user = findUserByIdOrThrow(id);
        String normalizedEmail = normalizeEmail(request.getEmail());

        if (userRepository.existsByEmailIgnoreCaseAndIdNot(normalizedEmail, id)) {
            throw new ResourceConflictException("Email already exists");
        }

        user.setFullName(request.getFullName());
        user.setEmail(normalizedEmail);
        user.setAvatarUrl(request.getAvatarUrl());

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
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}

