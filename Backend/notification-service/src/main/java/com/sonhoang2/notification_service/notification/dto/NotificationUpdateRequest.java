package com.sonhoang2.notification_service.notification.dto;

import lombok.Data;

/** Recipients may only mark their notifications as read or unread. */
@Data
public class NotificationUpdateRequest {
    private Boolean isRead;
}
