package com.sonhoang2.notification_service.events;

import com.sonhoang2.notification_service.notification.NotificationService;
import com.sonhoang2.notification_service.notification.dto.NotificationCreateRequest;
import com.sonhoang2.notification_service.notification.entity.NotificationType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventConsumerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private EventConsumer eventConsumer;

    private final UUID author = UUID.randomUUID();
    private final UUID assignee = UUID.randomUUID();
    private final UUID reporter = UUID.randomUUID();

    @Test
    void commentCreated_ShouldNotifyAssigneeAndReporter() {
        eventConsumer.handleTaskCommentCreatedEvent(comment(author, assignee, reporter));

        assertThat(recipients()).containsExactlyInAnyOrder(assignee, reporter);
    }

    @Test
    void commentCreated_ShouldNeverNotifyTheAuthor() {
        eventConsumer.handleTaskCommentCreatedEvent(comment(assignee, assignee, reporter));

        assertThat(recipients()).containsExactly(reporter);
    }

    @Test
    void commentCreated_ShouldNotifyOnce_WhenAssigneeIsAlsoReporter() {
        eventConsumer.handleTaskCommentCreatedEvent(comment(author, assignee, assignee));

        assertThat(recipients()).containsExactly(assignee);
    }

    @Test
    void commentCreated_ShouldNotifyNobody_WhenAuthorIsTheOnlyPersonInvolved() {
        eventConsumer.handleTaskCommentCreatedEvent(comment(author, null, author));

        verify(notificationService, never()).create(any());
    }

    private List<UUID> recipients() {
        ArgumentCaptor<NotificationCreateRequest> captor = ArgumentCaptor.forClass(NotificationCreateRequest.class);
        verify(notificationService, atLeastOnce()).create(captor.capture());
        captor.getAllValues().forEach(r -> assertThat(r.getType()).isEqualTo(NotificationType.COMMENT));
        return captor.getAllValues().stream().map(NotificationCreateRequest::getUserId).toList();
    }

    private static TaskCommentCreatedEvent comment(UUID author, UUID assignee, UUID reporter) {
        return TaskCommentCreatedEvent.builder()
                .commentId(UUID.randomUUID())
                .taskId(UUID.randomUUID())
                .taskTitle("Fix login")
                .userId(author)
                .assigneeId(assignee)
                .reporterId(reporter)
                .content("Looks good")
                .build();
    }
}
