package com.sonhoang2.TaskManagementAPI.user;

import com.sonhoang2.TaskManagementAPI.common.exception.ResourceConflictException;
import com.sonhoang2.TaskManagementAPI.user.dto.AdminCreateUserRequest;
import com.sonhoang2.TaskManagementAPI.user.dto.UserResponse;
import com.sonhoang2.TaskManagementAPI.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void createShouldHashPasswordAndNormalizeEmail() {
        AdminCreateUserRequest request = new AdminCreateUserRequest();
        request.setFullName("Son Hoang");
        request.setEmail("  Son@example.com ");
        request.setPassword("secret123");
        request.setAvatarUrl("https://cdn.example.com/avatar.png");

        when(userRepository.existsByEmailIgnoreCase("son@example.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("hashed-secret");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });

        UserResponse response = userService.create(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertEquals("son@example.com", savedUser.getEmail());
        assertEquals("hashed-secret", savedUser.getPassword());
        assertEquals("son@example.com", response.getEmail());
    }

    @Test
    void createShouldThrowConflictWhenEmailAlreadyExists() {
        AdminCreateUserRequest request = new AdminCreateUserRequest();
        request.setFullName("Son Hoang");
        request.setEmail("son@example.com");
        request.setPassword("secret123");

        when(userRepository.existsByEmailIgnoreCase("son@example.com")).thenReturn(true);

        assertThrows(ResourceConflictException.class, () -> userService.create(request));
        verify(userRepository, never()).save(any(User.class));
    }
}

