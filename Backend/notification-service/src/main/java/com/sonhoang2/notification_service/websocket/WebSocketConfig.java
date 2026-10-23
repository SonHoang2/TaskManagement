package com.sonhoang2.notification_service.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * STOMP over WebSocket at {@code /ws}. RabbitMQ (rabbitmq_stomp plugin) is the broker, so a
 * notification produced on any instance reaches the instance holding the user's connection.
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompAuthChannelInterceptor stompAuthChannelInterceptor;

    @Value("${app.websocket.relay-host}")
    private String relayHost;

    @Value("${app.websocket.relay-port}")
    private int relayPort;

    @Value("${app.websocket.relay-virtual-host}")
    private String relayVirtualHost;

    @Value("${spring.rabbitmq.username}")
    private String relayLogin;

    @Value("${spring.rabbitmq.password}")
    private String relayPasscode;

    // Auth travels in the STOMP CONNECT frame, not in cookies, so cross-origin handshakes carry no ambient credentials.
    @Value("${app.websocket.allowed-origin-patterns}")
    private String[] allowedOriginPatterns;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOriginPatterns(allowedOriginPatterns);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
        registry.enableStompBrokerRelay("/topic")
                .setRelayHost(relayHost)
                .setRelayPort(relayPort)
                // Pin the vhost; otherwise a client's own STOMP "host" header would pick the RabbitMQ vhost
                .setVirtualHost(relayVirtualHost)
                .setClientLogin(relayLogin)
                .setClientPasscode(relayPasscode)
                .setSystemLogin(relayLogin)
                .setSystemPasscode(relayPasscode)
                // Share sessions between instances so /user destinations resolve wherever the user is connected
                .setUserDestinationBroadcast("/topic/unresolved-user-destination")
                .setUserRegistryBroadcast("/topic/simp-user-registry");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompAuthChannelInterceptor);
    }
}
