package com.sonhoang2.notification_service.websocket;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class StompAuthChannelInterceptorTest {

    private static final String SECRET = "test-secret-test-secret-test-secret-test-secret";

    private final StompAuthChannelInterceptor interceptor =
            new StompAuthChannelInterceptor(new JwtVerifier(SECRET));

    @Test
    void connect_ShouldAuthenticateUserFromBearerToken() {
        String userId = UUID.randomUUID().toString();
        StompHeaderAccessor accessor = accessor(StompCommand.CONNECT);
        accessor.addNativeHeader("Authorization", "Bearer " + token(userId, SECRET, 3600));

        Message<?> result = interceptor.preSend(message(accessor), null);

        StompHeaderAccessor out = MessageHeaderAccessor.getAccessor(result, StompHeaderAccessor.class);
        assertEquals(userId, out.getUser().getName());
    }

    @Test
    void connect_ShouldRejectMissingToken() {
        assertThrows(MessageDeliveryException.class,
                () -> interceptor.preSend(message(accessor(StompCommand.CONNECT)), null));
    }

    @Test
    void connect_ShouldRejectTokenSignedWithAnotherKey() {
        StompHeaderAccessor accessor = accessor(StompCommand.CONNECT);
        accessor.addNativeHeader("Authorization",
                "Bearer " + token(UUID.randomUUID().toString(), SECRET + "-other", 3600));

        assertThrows(MessageDeliveryException.class, () -> interceptor.preSend(message(accessor), null));
    }

    @Test
    void connect_ShouldRejectExpiredToken() {
        StompHeaderAccessor accessor = accessor(StompCommand.CONNECT);
        accessor.addNativeHeader("Authorization", "Bearer " + token(UUID.randomUUID().toString(), SECRET, -60));

        assertThrows(MessageDeliveryException.class, () -> interceptor.preSend(message(accessor), null));
    }

    @Test
    void subscribe_ShouldAllowOwnUserDestination() {
        StompHeaderAccessor accessor = accessor(StompCommand.SUBSCRIBE);
        accessor.setUser(new StompPrincipal(UUID.randomUUID().toString()));
        accessor.setDestination("/user/topic/notifications");

        assertNotNull(interceptor.preSend(message(accessor), null));
    }

    @Test
    void subscribe_ShouldRejectSharedTopics() {
        StompHeaderAccessor accessor = accessor(StompCommand.SUBSCRIBE);
        accessor.setUser(new StompPrincipal(UUID.randomUUID().toString()));
        accessor.setDestination("/topic/simp-user-registry");

        assertThrows(MessageDeliveryException.class, () -> interceptor.preSend(message(accessor), null));
    }

    @Test
    void send_ShouldBeRejected() {
        StompHeaderAccessor accessor = accessor(StompCommand.SEND);
        accessor.setUser(new StompPrincipal(UUID.randomUUID().toString()));
        accessor.setDestination("/app/anything");

        assertThrows(MessageDeliveryException.class, () -> interceptor.preSend(message(accessor), null));
    }

    private static StompHeaderAccessor accessor(StompCommand command) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setLeaveMutable(true);
        return accessor;
    }

    private static Message<byte[]> message(StompHeaderAccessor accessor) {
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private static String token(String userId, String secret, long ttlSeconds) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject("user@example.com")
                .claim("userId", userId)
                .issuedAt(Date.from(now.minusSeconds(120)))
                .expiration(Date.from(now.plusSeconds(ttlSeconds)))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS256)
                .compact();
    }
}
