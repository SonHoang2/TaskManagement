package com.sonhoang2.notification_service.websocket;

import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

/**
 * Authenticates STOMP sessions and limits what clients may do: the JWT is checked on CONNECT,
 * clients may only subscribe to their own {@code /user/...} destinations, and they may not send.
 */
@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String USER_DESTINATION_PREFIX = "/user/";

    private final JwtVerifier jwtVerifier;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        switch (accessor.getCommand()) {
            case CONNECT -> accessor.setUser(new StompPrincipal(authenticate(message, accessor)));
            case SUBSCRIBE -> {
                String destination = accessor.getDestination();
                if (accessor.getUser() == null || destination == null
                        || !destination.startsWith(USER_DESTINATION_PREFIX)) {
                    throw new MessageDeliveryException(message, "Subscription not allowed: " + destination);
                }
            }
            case SEND -> throw new MessageDeliveryException(message, "Clients cannot send messages");
            default -> {
            }
        }
        return message;
    }

    private String authenticate(Message<?> message, StompHeaderAccessor accessor) {
        String header = accessor.getFirstNativeHeader("Authorization");
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            throw new MessageDeliveryException(message, "Missing bearer token");
        }
        try {
            return jwtVerifier.userIdFrom(header.substring(BEARER_PREFIX.length()));
        } catch (JwtException | IllegalArgumentException e) {
            throw new MessageDeliveryException(message, "Invalid token");
        }
    }
}
