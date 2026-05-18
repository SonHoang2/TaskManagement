package com.sonhoang2.TaskManagementAPI.notification;

import com.sonhoang2.TaskManagementAPI.common.dto.PageResponse;
import com.sonhoang2.TaskManagementAPI.notification.dto.NotificationCreateRequest;
import com.sonhoang2.TaskManagementAPI.notification.dto.NotificationResponse;
import com.sonhoang2.TaskManagementAPI.notification.dto.NotificationUpdateRequest;
import com.sonhoang2.TaskManagementAPI.notification.entity.NotificationType;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface NotificationService {

    NotificationResponse create(NotificationCreateRequest request);

    PageResponse<NotificationResponse> findAll(UUID userId, Boolean isRead, NotificationType type, Pageable pageable);

    NotificationResponse findById(UUID id);

    NotificationResponse update(UUID id, NotificationUpdateRequest request);

    void delete(UUID id);
}

