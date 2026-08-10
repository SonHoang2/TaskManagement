package com.sonhoang2.userservice.auth;

import com.sonhoang2.userservice.auth.dto.LoginRequest;
import com.sonhoang2.userservice.auth.dto.LoginResponse;
import com.sonhoang2.userservice.auth.dto.RegisterRequest;
import com.sonhoang2.userservice.common.security.JwtService;
import com.sonhoang2.userservice.user.UserService;
import com.sonhoang2.userservice.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

import java.util.UUID;

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

        // Get user to retrieve userId
        UserResponse user = userService.findByEmail(request.getEmail());
        String token = jwtService.generateToken(user.getEmail(), user.getId()); // pass userId
        return buildLoginResponse(token, user.getId());
    }

    @Override
    public LoginResponse signup(RegisterRequest request) {
        UserResponse userResponse = userService.register(request);
        String token = jwtService.generateToken(userResponse.getEmail(), userResponse.getId());
        return buildLoginResponse(token, userResponse.getId());
    }

    @Override
    public void logout() {
        // Stateless
    }

    private LoginResponse buildLoginResponse(String token, UUID userId) {
        return LoginResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresInMs(jwtService.getJwtExpirationMs())
                .userId(userId)   // include userId in response
                .build();
    }
}