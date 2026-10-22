package com.sonhoang2.notification_service.notification;

import com.sonhoang2.notification_service.common.dto.JSendResponse;
import com.sonhoang2.notification_service.common.dto.PageResponse;
import com.sonhoang2.notification_service.notification.dto.NotificationResponse;
import com.sonhoang2.notification_service.notification.dto.NotificationUpdateRequest;
import com.sonhoang2.notification_service.notification.entity.NotificationType;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    // Notifications are only created from domain events (see EventConsumer), never over HTTP.

    @GetMapping
    public ResponseEntity<JSendResponse<Map<String, PageResponse<NotificationResponse>>>> findAll(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam(required = false) Boolean isRead,
            @RequestParam(required = false) NotificationType type,
            Pageable pageable
    ) {
        PageResponse<NotificationResponse> pageResponse = notificationService.findAll(userId, isRead, type, pageable);
        return ResponseEntity.ok(JSendResponse.success(Map.of("page", pageResponse)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<JSendResponse<Map<String, NotificationResponse>>> findById(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID userId
    ) {
        return ResponseEntity.ok(JSendResponse.success(Map.of("notification",
                notificationService.findById(id, userId))));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<JSendResponse<Map<String, NotificationResponse>>> update(
            @PathVariable UUID id,
            @Valid @RequestBody NotificationUpdateRequest request,
            @RequestHeader("X-User-Id") UUID userId
    ) {
        return ResponseEntity.ok(JSendResponse.success(Map.of("notification",
                notificationService.update(id, request, userId))));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<JSendResponse<Void>> delete(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID userId
    ) {
        notificationService.delete(id, userId);
        return ResponseEntity.ok(JSendResponse.success(null));
    }
}

