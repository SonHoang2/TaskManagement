package com.sonhoang2.notification_service.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${app.rabbitmq.exchange}")
    private String exchange;

    @Value("${app.rabbitmq.queue.task-assigned}")
    private String taskAssignedQueue;

    @Value("${app.rabbitmq.queue.task-comment-created}")
    private String taskCommentCreatedQueue;

    @Value("${app.rabbitmq.queue.project-invitation-created}")
    private String projectInvitationCreatedQueue;

    @Value("${app.rabbitmq.routing-key.task-assigned}")
    private String taskAssignedRoutingKey;

    @Value("${app.rabbitmq.routing-key.task-comment-created}")
    private String taskCommentCreatedRoutingKey;

    @Value("${app.rabbitmq.routing-key.project-invitation-created}")
    private String projectInvitationCreatedRoutingKey;

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public TopicExchange taskEventsExchange() {
        return new TopicExchange(exchange);
    }

    @Bean
    public Queue taskAssignedQueue() {
        return QueueBuilder.durable(taskAssignedQueue).build();
    }

    @Bean
    public Queue taskCommentCreatedQueue() {
        return QueueBuilder.durable(taskCommentCreatedQueue).build();
    }

    @Bean
    public Queue projectInvitationCreatedQueue() {
        return QueueBuilder.durable(projectInvitationCreatedQueue).build();
    }

    @Bean
    public Binding taskAssignedBinding() {
        return BindingBuilder.bind(taskAssignedQueue())
                .to(taskEventsExchange())
                .with(taskAssignedRoutingKey);
    }

    @Bean
    public Binding taskCommentCreatedBinding() {
        return BindingBuilder.bind(taskCommentCreatedQueue())
                .to(taskEventsExchange())
                .with(taskCommentCreatedRoutingKey);
    }

    @Bean
    public Binding projectInvitationCreatedBinding() {
        return BindingBuilder.bind(projectInvitationCreatedQueue())
                .to(taskEventsExchange())
                .with(projectInvitationCreatedRoutingKey);
    }
}
