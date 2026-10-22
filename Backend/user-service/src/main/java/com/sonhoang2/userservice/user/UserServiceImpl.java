package com.sonhoang2.userservice.user;

import com.sonhoang2.userservice.auth.dto.RegisterRequest;
import com.sonhoang2.common.dto.BaseUserRequest;
import com.sonhoang2.common.dto.PageResponse;
import com.sonhoang2.common.exception.ResourceConflictException;
import com.sonhoang2.common.exception.ResourceNotFoundException;
import com.sonhoang2.userservice.user.dto.AdminCreateUserRequest;
import com.sonhoang2.userservice.user.dto.AdminUpdateUserRequest;
import com.sonhoang2.userservice.user.dto.UserResponse;
import com.sonhoang2.userservice.user.entity.User;
import com.sonhoang2.userservice.user.entity.UserRole;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
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
    public UserResponse create(AdminCreateUserRequest request, UUID actorId) {
        User actor = findActorOrThrow(actorId);
        requireAdmin(actor);
        UserRole role = request.getRole() != null ? request.getRole() : UserRole.USER;
        requireCanAssign(actor, role);

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
    public UserResponse update(UUID id, AdminUpdateUserRequest request, UUID actorId) {
        User user = findUserByIdOrThrow(id);
        User actor = findActorOrThrow(actorId);

        // Users may edit their own profile; editing anyone else, or any role change, is an admin action.
        boolean roleChange = request.getRole() != null && request.getRole() != user.getRole();
        if (!actor.getId().equals(id) || roleChange) {
            requireAdmin(actor);
            requireCanManage(actor, user);
        }
        if (roleChange) {
            requireCanAssign(actor, request.getRole());
        }

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
    public void delete(UUID id, UUID actorId) {
        User user = findUserByIdOrThrow(id);
        User actor = findActorOrThrow(actorId);
        requireAdmin(actor);
        requireCanManage(actor, user);

        userRepository.delete(user);
    }

    // The caller's role is read from the database rather than the JWT so a demotion takes effect immediately.
    private User findActorOrThrow(UUID actorId) {
        return userRepository.findById(actorId)
                .orElseThrow(() -> new AccessDeniedException("Unknown user"));
    }

    private static boolean isAdmin(User user) {
        return user.getRole() == UserRole.ADMIN || user.getRole() == UserRole.SUPER_ADMIN;
    }

    private static void requireAdmin(User actor) {
        if (!isAdmin(actor)) {
            throw new AccessDeniedException("Only an admin can do this");
        }
    }

    /** Only a super admin may act on another super admin's account. */
    private static void requireCanManage(User actor, User target) {
        if (target.getRole() == UserRole.SUPER_ADMIN && actor.getRole() != UserRole.SUPER_ADMIN) {
            throw new AccessDeniedException("Only a super admin can manage a super admin");
        }
    }

    /** Only a super admin may hand out the super admin role. */
    private static void requireCanAssign(User actor, UserRole role) {
        if (role == UserRole.SUPER_ADMIN && actor.getRole() != UserRole.SUPER_ADMIN) {
            throw new AccessDeniedException("Only a super admin can grant the super admin role");
        }
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

    @Override
    @Transactional(readOnly = true)
    public UserResponse findByEmail(String email) {
        String normalizedEmail = normalizeEmail(email);
        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        return toResponse(user);
    }
}

