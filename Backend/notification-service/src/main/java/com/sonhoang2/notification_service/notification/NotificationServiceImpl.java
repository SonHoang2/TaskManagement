package com.sonhoang2.notification_service.notification;

import com.sonhoang2.notification_service.common.dto.PageResponse;
import com.sonhoang2.notification_service.common.exception.ResourceNotFoundException;
import com.sonhoang2.notification_service.notification.dto.NotificationCreateRequest;
import com.sonhoang2.notification_service.notification.dto.NotificationResponse;
import com.sonhoang2.notification_service.notification.dto.NotificationUpdateRequest;
import com.sonhoang2.notification_service.notification.entity.Notification;
import com.sonhoang2.notification_service.notification.entity.NotificationType;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final ModelMapper modelMapper;

    private PageResponse<NotificationResponse> toPageResponse(Page<Notification> page) {
        return new PageResponse<>(page.getContent().stream().map(this::toResponse).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext(),
                page.hasPrevious(),
                page.getNumberOfElements());
    }

    @Override
    public NotificationResponse create(NotificationCreateRequest request) {
        Boolean isRead = request.getIsRead() != null ? request.getIsRead() : false;
        Notification notification = Notification.builder()
                .userId(request.getUserId())
                .type(request.getType())
                .content(request.getContent())
                .isRead(isRead)
                .build();

        return toResponse(notificationRepository.save(notification));
    }

    @Override
    public PageResponse<NotificationResponse> findAll(UUID userId,
                                                      Boolean isRead,
                                                      NotificationType type,
                                                      Pageable pageable) {
        Page<Notification> page;

        if (userId != null && isRead != null && type != null) {
            page = notificationRepository.findByUserIdAndIsReadAndType(userId, isRead, type, pageable);
        } else if (userId != null && isRead != null) {
            page = notificationRepository.findByUserIdAndIsRead(userId, isRead, pageable);
        } else if (userId != null && type != null) {
            page = notificationRepository.findByUserIdAndType(userId, type, pageable);
        } else if (isRead != null && type != null) {
            page = notificationRepository.findByIsReadAndType(isRead, type, pageable);
        } else if (userId != null) {
            page = notificationRepository.findByUserId(userId, pageable);
        } else if (isRead != null) {
            page = notificationRepository.findByIsRead(isRead, pageable);
        } else if (type != null) {
            page = notificationRepository.findByType(type, pageable);
        } else {
            page = notificationRepository.findAll(pageable);
        }

        return toPageResponse(page);
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationResponse findById(UUID id) {
        return toResponse(findNotificationByIdOrThrow(id));
    }

    @Override
    public NotificationResponse update(UUID id, NotificationUpdateRequest request) {
        Notification notification = findNotificationByIdOrThrow(id);

        modelMapper.map(request, notification);

        return toResponse(notification);
    }

    @Override
    public void delete(UUID id) {
        notificationRepository.delete(findNotificationByIdOrThrow(id));
    }

    private Notification findNotificationByIdOrThrow(UUID id) {
        return notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification with id " + id + " not found"));
    }

    private NotificationResponse toResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .userId(notification.getUserId())
                .type(notification.getType())
                .content(notification.getContent())
                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
