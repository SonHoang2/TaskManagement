package com.sonhoang2.notification_service.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskAssignedEvent {
    private UUID taskId;
    private UUID projectId;
    private String taskTitle;
    private UUID assigneeId;
    private UUID reporterId;
    private String eventType;
}
