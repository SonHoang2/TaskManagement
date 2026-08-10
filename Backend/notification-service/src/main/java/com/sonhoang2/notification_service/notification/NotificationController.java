package com.sonhoang2.notification_service.notification;

import com.sonhoang2.notification_service.common.dto.JSendResponse;
import com.sonhoang2.notification_service.common.dto.PageResponse;
import com.sonhoang2.notification_service.notification.dto.NotificationCreateRequest;
import com.sonhoang2.notification_service.notification.dto.NotificationResponse;
import com.sonhoang2.notification_service.notification.dto.NotificationUpdateRequest;
import com.sonhoang2.notification_service.notification.entity.NotificationType;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping
    public ResponseEntity<JSendResponse<Map<String, NotificationResponse>>> create(
            @Valid @RequestBody NotificationCreateRequest request
    ) {
        NotificationResponse notification = notificationService.create(request);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(notification.getId())
                .toUri();

        return ResponseEntity.created(location)
                .body(JSendResponse.success(Map.of("notification", notification)));
    }

    @GetMapping
    public ResponseEntity<JSendResponse<Map<String, PageResponse<NotificationResponse>>>> findAll(
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) Boolean isRead,
            @RequestParam(required = false) NotificationType type,
            Pageable pageable
    ) {
        PageResponse<NotificationResponse> pageResponse = notificationService.findAll(userId, isRead, type, pageable);
        return ResponseEntity.ok(JSendResponse.success(Map.of("page", pageResponse)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<JSendResponse<Map<String, NotificationResponse>>> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(JSendResponse.success(Map.of("notification", notificationService.findById(id))));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<JSendResponse<Map<String, NotificationResponse>>> update(
            @PathVariable UUID id,
            @Valid @RequestBody NotificationUpdateRequest request
    ) {
        return ResponseEntity.ok(JSendResponse.success(Map.of("notification", notificationService.update(id, request))));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<JSendResponse<Void>> delete(@PathVariable UUID id) {
        notificationService.delete(id);
        return ResponseEntity.ok(JSendResponse.success(null));
    }
}

