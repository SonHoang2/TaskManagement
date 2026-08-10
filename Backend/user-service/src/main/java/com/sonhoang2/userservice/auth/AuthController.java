package com.sonhoang2.userservice.auth;

import com.sonhoang2.userservice.auth.dto.LoginRequest;
import com.sonhoang2.userservice.auth.dto.LoginResponse;
import com.sonhoang2.userservice.auth.dto.RegisterRequest;
import com.sonhoang2.userservice.common.dto.JSendResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<JSendResponse<Map<String, LoginResponse>>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(JSendResponse.success(Map.of("auth", response)));
    }

    @PostMapping("/signup")
    public ResponseEntity<JSendResponse<Map<String, LoginResponse>>> signup(@Valid @RequestBody RegisterRequest request) {
        LoginResponse response = authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(JSendResponse.success(Map.of("auth", response)));
    }

    @PostMapping("/logout")
    public ResponseEntity<JSendResponse<Map<String, String>>> logout() {
        authService.logout();
        return ResponseEntity.ok(JSendResponse.success(Map.of("message", "Logged out successfully")));
    }
}

