package com.sonhoang2.userservice.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sonhoang2.userservice.common.config.SecurityConfig;
import com.sonhoang2.common.dto.PageResponse;
import com.sonhoang2.common.exception.ResourceNotFoundException;
import com.sonhoang2.userservice.user.dto.AdminCreateUserRequest;
import com.sonhoang2.userservice.user.dto.AdminUpdateUserRequest;
import com.sonhoang2.userservice.user.dto.UserResponse;
import com.sonhoang2.userservice.user.entity.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private UserResponse userResponse;
    private AdminCreateUserRequest createRequest;
    private AdminUpdateUserRequest updateRequest;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        userResponse = UserResponse.builder()
                .id(userId)
                .fullName("Test User")
                .email("test@example.com")
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

        updateRequest = new AdminUpdateUserRequest();
        updateRequest.setFullName("Updated User");
        updateRequest.setEmail("updated@example.com");
        updateRequest.setPassword("newpassword123");
        updateRequest.setAvatarUrl("https://example.com/new-avatar.jpg");
        updateRequest.setRole(UserRole.ADMIN);
    }

    @Test
    @WithMockUser
    void create_Success() throws Exception {
        when(userService.create(any(AdminCreateUserRequest.class))).thenReturn(userResponse);

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.user.id").value(userId.toString()))
                .andExpect(jsonPath("$.data.user.fullName").value("Test User"))
                .andExpect(jsonPath("$.data.user.email").value("test@example.com"))
                .andExpect(jsonPath("$.data.user.role").value("USER"));
    }

    @Test
    @WithMockUser
    void create_ValidationError() throws Exception {
        createRequest.setEmail(""); // Invalid email

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void findAll_Success() throws Exception {
        Pageable pageable = PageRequest.of(0, 10);
        PageResponse<UserResponse> pageResponse = new PageResponse<>(
                List.of(userResponse),
                0,
                10,
                1,
                1,
                false,
                false,
                1
        );

        when(userService.findAll(eq(null), any(Pageable.class))).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/users")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.page.content[0].id").value(userId.toString()))
                .andExpect(jsonPath("$.data.page.totalElements").value(1))
                .andExpect(jsonPath("$.data.page.totalPages").value(1));
    }

    @Test
    @WithMockUser
    void findAll_WithKeyword_Success() throws Exception {
        Pageable pageable = PageRequest.of(0, 10);
        PageResponse<UserResponse> pageResponse = new PageResponse<>(
                List.of(userResponse),
                0,
                10,
                1,
                1,
                false,
                false,
                1
        );

        when(userService.findAll(eq("test"), any(Pageable.class))).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/users")
                        .param("keyword", "test")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.page.content[0].id").value(userId.toString()));
    }

    @Test
    @WithMockUser
    void findById_Success() throws Exception {
        when(userService.findById(userId)).thenReturn(userResponse);

        mockMvc.perform(get("/api/v1/users/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.user.id").value(userId.toString()))
                .andExpect(jsonPath("$.data.user.fullName").value("Test User"))
                .andExpect(jsonPath("$.data.user.email").value("test@example.com"));
    }

    @Test
    @WithMockUser
    void findById_NotFound() throws Exception {
        when(userService.findById(userId))
                .thenThrow(new ResourceNotFoundException("User with id " + userId + " not found"));

        mockMvc.perform(get("/api/v1/users/{id}", userId))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void update_Success() throws Exception {
        UserResponse updatedResponse = UserResponse.builder()
                .id(userId)
                .fullName("Updated User")
                .email("updated@example.com")
                .avatarUrl("https://example.com/new-avatar.jpg")
                .role(UserRole.ADMIN)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(userService.update(eq(userId), any(AdminUpdateUserRequest.class))).thenReturn(updatedResponse);

        mockMvc.perform(patch("/api/v1/users/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.user.fullName").value("Updated User"))
                .andExpect(jsonPath("$.data.user.email").value("updated@example.com"))
                .andExpect(jsonPath("$.data.user.role").value("ADMIN"));
    }

    @Test
    @WithMockUser
    void update_ValidationError() throws Exception {
        updateRequest.setEmail("invalid-email"); // Invalid email format

        mockMvc.perform(patch("/api/v1/users/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void delete_Success() throws Exception {
        mockMvc.perform(delete("/api/v1/users/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    @WithMockUser
    void delete_NotFound() throws Exception {
        org.mockito.Mockito.doThrow(new ResourceNotFoundException("User with id " + userId + " not found"))
                .when(userService).delete(userId);

        mockMvc.perform(delete("/api/v1/users/{id}", userId))
                .andExpect(status().isNotFound());
    }
}
