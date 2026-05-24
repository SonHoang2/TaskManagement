package com.sonhoang2.userservice.user;

import com.sonhoang2.userservice.auth.dto.RegisterRequest;
import com.sonhoang2.userservice.common.dto.BaseUserRequest;
import com.sonhoang2.userservice.common.dto.PageResponse;
import com.sonhoang2.userservice.common.exception.ResourceConflictException;
import com.sonhoang2.userservice.common.exception.ResourceNotFoundException;
import com.sonhoang2.userservice.user.dto.AdminCreateUserRequest;
import com.sonhoang2.userservice.user.dto.AdminUpdateUserRequest;
import com.sonhoang2.userservice.user.dto.UserResponse;
import com.sonhoang2.userservice.user.entity.User;
import com.sonhoang2.userservice.user.entity.UserRole;
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

    @Override
    public UserResponse create(AdminCreateUserRequest request) {
        UserRole role = request.getRole() != null ? request.getRole() : UserRole.USER;
        User user = persistUser(request, role, request.getAvatarUrl());
        return toResponse(user);
    }

    @Override
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

        if (StringUtils.hasText(request.getEmail())) {
            String normalizedEmail = normalizeEmail(request.getEmail());
            if (userRepository.existsByEmailIgnoreCaseAndIdNot(normalizedEmail, id)) {
                throw new ResourceConflictException("Email already exists");
            }
            user.setEmail(normalizedEmail);
        }

        modelMapper.map(request, user);

        if (StringUtils.hasText(request.getPassword())) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

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

