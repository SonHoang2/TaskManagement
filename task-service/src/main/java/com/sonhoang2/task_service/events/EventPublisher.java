package com.sonhoang2.task_service.events;

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

    @Value("${app.rabbitmq.routing-key.task-assigned}")
    private String taskAssignedRoutingKey;

    @Value("${app.rabbitmq.routing-key.task-comment-created}")
    private String taskCommentCreatedRoutingKey;

    public void publishTaskAssignedEvent(TaskAssignedEvent event) {
        log.info("Publishing TaskAssignedEvent: {}", event);
        rabbitTemplate.convertAndSend(exchange, taskAssignedRoutingKey, event);
    }

    public void publishTaskCommentCreatedEvent(TaskCommentCreatedEvent event) {
        log.info("Publishing TaskCommentCreatedEvent: {}", event);
        rabbitTemplate.convertAndSend(exchange, taskCommentCreatedRoutingKey, event);
    }
}
