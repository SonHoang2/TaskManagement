package com.sonhoang2.userservice.common.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private String jwtSecret;
    private long jwtExpirationMs;
    private String testEmail;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        jwtSecret = "test-secret-key-for-jwt-token-generation-and-validation-must-be-long-enough";
        jwtExpirationMs = 3600000L; // 1 hour
        jwtService = new JwtService(jwtSecret, jwtExpirationMs);
        testEmail = "test@example.com";
        testUserId = UUID.randomUUID();
    }

    @Test
    void generateToken_Success() {
        String token = jwtService.generateToken(testEmail, testUserId);

        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(token.split("\\.").length == 3); // JWT has 3 parts
    }

    @Test
    void extractUsername_Success() {
        String token = jwtService.generateToken(testEmail, testUserId);
        String extractedEmail = jwtService.extractUsername(token);

        assertEquals(testEmail, extractedEmail);
    }

    @Test
    void extractUserId_Success() {
        String token = jwtService.generateToken(testEmail, testUserId);
        UUID extractedUserId = jwtService.extractUserId(token);

        assertEquals(testUserId, extractedUserId);
    }

    @Test
    void isTokenValid_ValidToken() {
        String token = jwtService.generateToken(testEmail, testUserId);
        UserDetails userDetails = User.withUsername(testEmail)
                .password("password")
                .roles("USER")
                .build();

        boolean isValid = jwtService.isTokenValid(token, userDetails);

        assertTrue(isValid);
    }

    @Test
    void isTokenValid_InvalidUsername() {
        String token = jwtService.generateToken(testEmail, testUserId);
        UserDetails userDetails = User.withUsername("different@example.com")
                .password("password")
                .roles("USER")
                .build();

        boolean isValid = jwtService.isTokenValid(token, userDetails);

        assertFalse(isValid);
    }

    @Test
    void getJwtExpirationMs_Success() {
        long expirationMs = jwtService.getJwtExpirationMs();

        assertEquals(jwtExpirationMs, expirationMs);
    }

    @Test
    void generateToken_DifferentEmailsProduceDifferentTokens() {
        String token1 = jwtService.generateToken("user1@example.com", UUID.randomUUID());
        String token2 = jwtService.generateToken("user2@example.com", UUID.randomUUID());

        assertNotEquals(token1, token2);
    }

    @Test
    void extractUsername_CaseInsensitive() {
        String token = jwtService.generateToken(testEmail.toLowerCase(), testUserId);
        String extractedEmail = jwtService.extractUsername(token);

        assertEquals(testEmail.toLowerCase(), extractedEmail);
    }

    @Test
    void extractUserId_NullForInvalidToken() {
        String invalidToken = "invalid.token.here";

        assertThrows(Exception.class, () -> jwtService.extractUserId(invalidToken));
    }

    @Test
    void generateToken_WithNullUserId() {
        String token = jwtService.generateToken(testEmail, null);
        UUID extractedUserId = jwtService.extractUserId(token);

        assertNull(extractedUserId);
    }

    @Test
    void generateToken_WithValidUserId() {
        UUID userId = UUID.randomUUID();
        String token = jwtService.generateToken(testEmail, userId);
        UUID extractedUserId = jwtService.extractUserId(token);

        assertEquals(userId, extractedUserId);
    }
}
