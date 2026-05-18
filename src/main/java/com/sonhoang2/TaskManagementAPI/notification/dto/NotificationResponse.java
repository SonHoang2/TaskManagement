package com.sonhoang2.TaskManagementAPI.notification.dto;

import com.sonhoang2.TaskManagementAPI.notification.entity.NotificationType;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class NotificationResponse {

    private UUID id;
    private UUID userId;
    private NotificationType type;
    private String content;
    private Boolean isRead;
    private Instant createdAt;
}

