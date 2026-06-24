package com.sonhoang2.userservice.user;

import com.sonhoang2.userservice.auth.dto.RegisterRequest;
import com.sonhoang2.userservice.common.exception.ResourceConflictException;
import com.sonhoang2.userservice.common.exception.ResourceNotFoundException;
import com.sonhoang2.userservice.user.dto.AdminCreateUserRequest;
import com.sonhoang2.userservice.user.dto.AdminUpdateUserRequest;
import com.sonhoang2.userservice.user.dto.UserResponse;
import com.sonhoang2.userservice.user.entity.User;
import com.sonhoang2.userservice.user.entity.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private UserServiceImpl userService;

    private User user;
    private UUID userId;
    private AdminCreateUserRequest createRequest;
    private RegisterRequest registerRequest;
    private AdminUpdateUserRequest updateRequest;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        user = User.builder()
                .id(userId)
                .fullName("Test User")
                .email("test@example.com")
                .password("encodedPassword")
                .avatarUrl("https://example.com/avatar.jpg")
                .role(UserRole.USER)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        createRequest = new AdminCreateUserRequest();
        createRequest.setFullName("Test User");
        createRequest.setEmail("test@example.com");
        createRequest.setPassword("password123");
        createRequest.setAvatarUrl("https://example.com/avatar.jpg");
        createRequest.setRole(UserRole.USER);

        registerRequest = new RegisterRequest();
        registerRequest.setFullName("Test User");
        registerRequest.setEmail("test@example.com");
        registerRequest.setPassword("password123");

        updateRequest = new AdminUpdateUserRequest();
        updateRequest.setFullName("Updated User");
        updateRequest.setEmail("updated@example.com");
        updateRequest.setPassword("newpassword123");
        updateRequest.setAvatarUrl("https://example.com/new-avatar.jpg");
        updateRequest.setRole(UserRole.ADMIN);
    }

    @Test
    void create_Success() {
        when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);

        UserResponse response = userService.create(createRequest);

        assertNotNull(response);
        assertEquals(userId, response.getId());
        assertEquals("Test User", response.getFullName());
        assertEquals("test@example.com", response.getEmail());
        assertEquals(UserRole.USER, response.getRole());

        verify(userRepository).existsByEmailIgnoreCase("test@example.com");
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void create_EmailConflict() {
        when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(true);

        assertThrows(ResourceConflictException.class, () -> userService.create(createRequest));

        verify(userRepository).existsByEmailIgnoreCase("test@example.com");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_Success() {
        when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);

        UserResponse response = userService.register(registerRequest);

        assertNotNull(response);
        assertEquals(userId, response.getId());
        assertEquals("Test User", response.getFullName());
        assertEquals(UserRole.USER, response.getRole());

        verify(userRepository).existsByEmailIgnoreCase("test@example.com");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_EmailConflict() {
        when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(true);

        assertThrows(ResourceConflictException.class, () -> userService.register(registerRequest));

        verify(userRepository).existsByEmailIgnoreCase("test@example.com");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void findAll_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> userPage = new PageImpl<>(List.of(user), pageable, 1);

        when(userRepository.findAll(pageable)).thenReturn(userPage);

        var response = userService.findAll(null, pageable);

        assertNotNull(response);
        assertEquals(1, response.content().size());
        assertEquals(1, response.totalElements());
        assertEquals(1, response.totalPages());

        verify(userRepository).findAll(pageable);
    }

    @Test
    void findAll_WithKeyword_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> userPage = new PageImpl<>(List.of(user), pageable, 1);

        when(userRepository.findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                anyString(), anyString(), any(Pageable.class))).thenReturn(userPage);

        var response = userService.findAll("test", pageable);

        assertNotNull(response);
        assertEquals(1, response.content().size());

        verify(userRepository).findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                "test", "test", pageable);
    }

    @Test
    void findById_Success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UserResponse response = userService.findById(userId);

        assertNotNull(response);
        assertEquals(userId, response.getId());
        assertEquals("Test User", response.getFullName());

        verify(userRepository).findById(userId);
    }

    @Test
    void findById_NotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.findById(userId));

        verify(userRepository).findById(userId);
    }

    @Test
    void update_Success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmailIgnoreCaseAndIdNot(anyString(), any(UUID.class))).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("newEncodedPassword");

        UserResponse response = userService.update(userId, updateRequest);

        assertNotNull(response);
        verify(userRepository).findById(userId);
        verify(modelMapper).map(updateRequest, user);
        verify(passwordEncoder).encode("newpassword123");
    }

    @Test
    void update_EmailConflict() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmailIgnoreCaseAndIdNot(anyString(), any(UUID.class))).thenReturn(true);

        assertThrows(ResourceConflictException.class, () -> userService.update(userId, updateRequest));

        verify(userRepository).findById(userId);
        verify(userRepository).existsByEmailIgnoreCaseAndIdNot("updated@example.com", userId);
        verify(modelMapper, never()).map(any(), any());
    }

    @Test
    void update_NotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.update(userId, updateRequest));

        verify(userRepository).findById(userId);
        verify(modelMapper, never()).map(any(), any());
    }

    @Test
    void delete_Success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        doNothing().when(userRepository).delete(any(User.class));

        userService.delete(userId);

        verify(userRepository).findById(userId);
        verify(userRepository).delete(user);
    }

    @Test
    void delete_NotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.delete(userId));

        verify(userRepository).findById(userId);
        verify(userRepository, never()).delete(any(User.class));
    }

    @Test
    void findByEmail_Success() {
        when(userRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.of(user));

        UserResponse response = userService.findByEmail("test@example.com");

        assertNotNull(response);
        assertEquals(userId, response.getId());
        assertEquals("test@example.com", response.getEmail());

        verify(userRepository).findByEmailIgnoreCase("test@example.com");
    }

    @Test
    void findByEmail_NotFound() {
        when(userRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.findByEmail("test@example.com"));

        verify(userRepository).findByEmailIgnoreCase("test@example.com");
    }

    @Test
    void findByEmail_CaseInsensitive() {
        when(userRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.of(user));

        UserResponse response = userService.findByEmail("TEST@EXAMPLE.COM");

        assertNotNull(response);
        assertEquals("test@example.com", response.getEmail());

        verify(userRepository).findByEmailIgnoreCase("test@example.com");
    }
}
