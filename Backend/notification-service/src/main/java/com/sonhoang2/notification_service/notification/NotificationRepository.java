package com.sonhoang2.notification_service.notification;

import com.sonhoang2.notification_service.notification.entity.Notification;
import com.sonhoang2.notification_service.notification.entity.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    Page<Notification> findByUserId(UUID userId, Pageable pageable);

    Page<Notification> findByIsRead(Boolean isRead, Pageable pageable);

    Page<Notification> findByType(NotificationType type, Pageable pageable);

    Page<Notification> findByUserIdAndIsRead(UUID userId, Boolean isRead, Pageable pageable);

    Page<Notification> findByUserIdAndType(UUID userId, NotificationType type, Pageable pageable);

    Page<Notification> findByIsReadAndType(Boolean isRead, NotificationType type, Pageable pageable);

    Page<Notification> findByUserIdAndIsReadAndType(UUID userId, Boolean isRead, NotificationType type, Pageable pageable);
}

