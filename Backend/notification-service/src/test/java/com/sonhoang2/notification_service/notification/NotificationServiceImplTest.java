package com.sonhoang2.notification_service.notification;

import com.sonhoang2.notification_service.common.dto.PageResponse;
import com.sonhoang2.notification_service.common.exception.ResourceNotFoundException;
import com.sonhoang2.notification_service.notification.dto.NotificationCreateRequest;
import com.sonhoang2.notification_service.notification.dto.NotificationResponse;
import com.sonhoang2.notification_service.notification.dto.NotificationUpdateRequest;
import com.sonhoang2.notification_service.notification.entity.Notification;
import com.sonhoang2.notification_service.notification.entity.NotificationType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private final UUID userId = UUID.randomUUID();
    private final UUID notificationId = UUID.randomUUID();
    private final Pageable pageable = PageRequest.of(0, 20);

    @Test
    void create_ShouldPublishTheSavedNotificationForPush() {
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification());

        NotificationResponse response = notificationService.create(NotificationCreateRequest.builder()
                .userId(userId)
                .type(NotificationType.TASK_ASSIGNED)
                .content("You were assigned a task")
                .build());

        verify(eventPublisher).publishEvent(response);
        assertEquals(userId, response.getUserId());
    }

    @Test
    void findAll_ShouldOnlyQueryTheCallersNotifications() {
        when(notificationRepository.findByUserId(userId, pageable))
                .thenReturn(new PageImpl<>(List.of(notification())));

        PageResponse<NotificationResponse> result = notificationService.findAll(userId, null, null, pageable);

        assertEquals(1, result.content().size());
        verify(notificationRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void findAll_WithUnreadFilter_ShouldStayScopedToTheCaller() {
        when(notificationRepository.findByUserIdAndIsRead(userId, false, pageable))
                .thenReturn(new PageImpl<>(List.of(notification())));

        PageResponse<NotificationResponse> result = notificationService.findAll(userId, false, null, pageable);

        assertEquals(1, result.content().size());
    }

    @Test
    void update_ShouldMarkOwnNotificationAsRead() {
        Notification notification = notification();
        NotificationUpdateRequest request = new NotificationUpdateRequest();
        request.setIsRead(true);
        when(notificationRepository.findByIdAndUserId(notificationId, userId)).thenReturn(Optional.of(notification));

        notificationService.update(notificationId, request, userId);

        verify(modelMapper).map(request, notification);
    }

    @Test
    void update_ShouldReportSomeoneElsesNotificationAsNotFound() {
        UUID otherUser = UUID.randomUUID();
        when(notificationRepository.findByIdAndUserId(notificationId, otherUser)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> notificationService.update(notificationId, new NotificationUpdateRequest(), otherUser));

        verifyNoInteractions(modelMapper);
    }

    @Test
    void delete_ShouldNotDeleteSomeoneElsesNotification() {
        UUID otherUser = UUID.randomUUID();
        when(notificationRepository.findByIdAndUserId(notificationId, otherUser)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> notificationService.delete(notificationId, otherUser));

        verify(notificationRepository, never()).delete(any());
    }

    @Test
    void findById_ShouldReturnOwnNotification() {
        when(notificationRepository.findByIdAndUserId(notificationId, userId))
                .thenReturn(Optional.of(notification()));

        assertEquals(notificationId, notificationService.findById(notificationId, userId).getId());
    }

    private Notification notification() {
        return Notification.builder()
                .id(notificationId)
                .userId(userId)
                .type(NotificationType.TASK_ASSIGNED)
                .content("You were assigned a task")
                .isRead(false)
                .build();
    }
}
