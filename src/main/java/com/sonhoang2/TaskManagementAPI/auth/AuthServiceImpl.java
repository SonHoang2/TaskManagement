package com.sonhoang2.TaskManagementAPI.auth;

import com.sonhoang2.TaskManagementAPI.auth.dto.LoginRequest;
import com.sonhoang2.TaskManagementAPI.auth.dto.LoginResponse;
import com.sonhoang2.TaskManagementAPI.auth.dto.RegisterRequest;
import com.sonhoang2.TaskManagementAPI.common.security.JwtService;
import com.sonhoang2.TaskManagementAPI.user.UserService;
import com.sonhoang2.TaskManagementAPI.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserService userService;

    @Override
    public LoginResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (AuthenticationException ex) {
            throw new BadCredentialsException("Invalid email or password");
        }

        String token = jwtService.generateToken(request.getEmail());
        return buildLoginResponse(token);
    }

    @Override
    public LoginResponse signup(RegisterRequest request) {
        UserResponse userResponse = userService.register(request);
        String token = jwtService.generateToken(userResponse.getEmail());
        return buildLoginResponse(token);
    }

    @Override
    public void logout() {
        // Stateless JWT logout is handled on client side by discarding the token.
    }

    private LoginResponse buildLoginResponse(String token) {
        return LoginResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresInMs(jwtService.getJwtExpirationMs())
                .build();
    }
}

