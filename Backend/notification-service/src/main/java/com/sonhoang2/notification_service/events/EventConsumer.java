package com.sonhoang2.notification_service.events;

import com.sonhoang2.notification_service.notification.NotificationService;
import com.sonhoang2.notification_service.notification.dto.NotificationCreateRequest;
import com.sonhoang2.notification_service.notification.entity.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventConsumer {

    private final NotificationService notificationService;

    @RabbitListener(queues = "${app.rabbitmq.queue.task-assigned}")
    public void handleTaskAssignedEvent(TaskAssignedEvent event) {
        log.info("Received TaskAssignedEvent: {}", event);
        
        try {
            NotificationCreateRequest request = NotificationCreateRequest.builder()
                    .userId(event.getAssigneeId())
                    .type(NotificationType.TASK_ASSIGNED)
                    .content("You have been assigned to task: " + event.getTaskTitle())
                    .isRead(false)
                    .build();
            
            notificationService.create(request);
            log.info("Successfully created notification for TaskAssignedEvent");
        } catch (Exception e) {
            log.error("Error processing TaskAssignedEvent: {}", e.getMessage(), e);
        }
    }

    @RabbitListener(queues = "${app.rabbitmq.queue.task-comment-created}")
    public void handleTaskCommentCreatedEvent(TaskCommentCreatedEvent event) {
        log.info("Received TaskCommentCreatedEvent: {}", event);
        
        // Notify the people involved in the task, never the author of the comment
        Set<UUID> recipients = new LinkedHashSet<>();
        recipients.add(event.getAssigneeId());
        recipients.add(event.getReporterId());
        recipients.remove(null);
        recipients.remove(event.getUserId());

        for (UUID recipient : recipients) {
            try {
                NotificationCreateRequest request = NotificationCreateRequest.builder()
                        .userId(recipient)
                        .type(NotificationType.COMMENT)
                        .content("New comment on task: " + event.getTaskTitle())
                        .isRead(false)
                        .build();

                notificationService.create(request);
            } catch (Exception e) {
                log.error("Error creating comment notification for {}: {}", recipient, e.getMessage(), e);
            }
        }
        log.info("Created {} notification(s) for TaskCommentCreatedEvent", recipients.size());
    }

    @RabbitListener(queues = "${app.rabbitmq.queue.project-invitation-created}")
    public void handleProjectInvitationCreatedEvent(ProjectInvitationCreatedEvent event) {
        log.info("Received ProjectInvitationCreatedEvent: {}", event);
        
        try {
            NotificationCreateRequest request = NotificationCreateRequest.builder()
                    .userId(event.getInviteeId())
                    .type(NotificationType.PROJECT_INVITATION)
                    .content("You have been invited to join project: " + event.getProjectName())
                    .isRead(false)
                    .build();
            
            notificationService.create(request);
            log.info("Successfully created notification for ProjectInvitationCreatedEvent");
        } catch (Exception e) {
            log.error("Error processing ProjectInvitationCreatedEvent: {}", e.getMessage(), e);
        }
    }
}
