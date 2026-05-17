package com.sonhoang2.TaskManagementAPI.auth;

import com.sonhoang2.TaskManagementAPI.auth.dto.LoginRequest;
import com.sonhoang2.TaskManagementAPI.auth.dto.LoginResponse;
import com.sonhoang2.TaskManagementAPI.auth.dto.RegisterRequest;
import com.sonhoang2.TaskManagementAPI.common.security.JwtService;
import com.sonhoang2.TaskManagementAPI.user.UserService;
import com.sonhoang2.TaskManagementAPI.user.dto.UserResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserService userService;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void loginShouldReturnTokenWhenCredentialsAreValid() {
        LoginRequest request = new LoginRequest();
        request.setEmail("son@example.com");
        request.setPassword("secret123");

        when(jwtService.generateToken("son@example.com")).thenReturn("jwt-token");
        when(jwtService.getJwtExpirationMs()).thenReturn(86_400_000L);

        LoginResponse response = authService.login(request);

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        assertEquals("jwt-token", response.getAccessToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(86_400_000L, response.getExpiresInMs());
    }

    @Test
    void signupShouldCreateUserAndReturnToken() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Son Hoang");
        request.setEmail("son@example.com");
        request.setPassword("secret123");

        UserResponse userResponse = UserResponse.builder()
                .email("son@example.com")
                .build();

        when(userService.register(request)).thenReturn(userResponse);
        when(jwtService.generateToken("son@example.com")).thenReturn("signup-token");
        when(jwtService.getJwtExpirationMs()).thenReturn(86_400_000L);

        LoginResponse response = authService.signup(request);

        verify(userService).register(request);
        assertEquals("signup-token", response.getAccessToken());
        assertEquals("Bearer", response.getTokenType());
    }

    @Test
    void logoutShouldNotThrow() {
        authService.logout();
    }
}

