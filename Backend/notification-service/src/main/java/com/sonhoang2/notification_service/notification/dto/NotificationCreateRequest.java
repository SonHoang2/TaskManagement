package com.sonhoang2.notification_service.notification.dto;

import com.sonhoang2.notification_service.notification.entity.NotificationType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class NotificationCreateRequest {

    @NotNull(message = "userId is required")
    private UUID userId;

    @NotNull(message = "type is required")
    private NotificationType type;

    @Size(max = 500, message = "content length must be <= 500")
    private String content;

    @Builder.Default
    private Boolean isRead = false;
}

