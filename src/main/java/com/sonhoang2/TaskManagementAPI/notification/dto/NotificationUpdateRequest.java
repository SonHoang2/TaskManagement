package com.sonhoang2.TaskManagementAPI.notification.dto;

import com.sonhoang2.TaskManagementAPI.notification.entity.NotificationType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class NotificationUpdateRequest {
    private UUID userId;
    private NotificationType type;

    @Size(max = 500, message = "content length must be <= 500")
    private String content;
    private Boolean isRead;
}

