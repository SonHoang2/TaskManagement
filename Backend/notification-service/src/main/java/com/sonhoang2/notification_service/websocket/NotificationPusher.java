package com.sonhoang2.notification_service.websocket;

import com.sonhoang2.notification_service.notification.dto.NotificationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/** Pushes a notification to its recipient's open sessions once it has been committed. */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationPusher {

    /** Clients subscribe to {@code /user/topic/notifications}. */
    public static final String USER_DESTINATION = "/topic/notifications";

    private final SimpMessagingTemplate messagingTemplate;

    @TransactionalEventListener
    public void onNotificationCreated(NotificationResponse notification) {
        try {
            messagingTemplate.convertAndSendToUser(notification.getUserId().toString(), USER_DESTINATION, notification);
        } catch (Exception e) {
            // The notification is already saved; clients still see it on their next refresh.
            log.warn("Could not push notification {}: {}", notification.getId(), e.getMessage());
        }
    }
}
