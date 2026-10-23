package com.sonhoang2.notification_service.websocket;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * Verifies the access tokens issued by user-service. HTTP calls are authenticated by the gateway,
 * but the WebSocket handshake cannot carry an Authorization header, so STOMP CONNECT is checked here.
 */
@Component
public class JwtVerifier {

    private final SecretKey signingKey;

    public JwtVerifier(@Value("${jwt.secret}") String secret) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /** Returns the token's userId claim; throws {@link JwtException} if the token is invalid or expired. */
    public String userIdFrom(String token) {
        Claims claims = Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
        String userId = claims.get("userId", String.class);
        if (userId == null) {
            throw new JwtException("Token has no userId claim");
        }
        return userId;
    }
}
