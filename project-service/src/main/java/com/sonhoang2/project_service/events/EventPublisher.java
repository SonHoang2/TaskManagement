package com.sonhoang2.project_service.events;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${app.rabbitmq.exchange}")
    private String exchange;

    @Value("${app.rabbitmq.routing-key.project-invitation-created}")
    private String projectInvitationCreatedRoutingKey;

    public void publishProjectInvitationCreatedEvent(ProjectInvitationCreatedEvent event) {
        log.info("Publishing ProjectInvitationCreatedEvent: {}", event);
        rabbitTemplate.convertAndSend(exchange, projectInvitationCreatedRoutingKey, event);
    }
}
