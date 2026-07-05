package com.sonhoang2.task_service.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskCommentCreatedEvent {
    private UUID commentId;
    private UUID taskId;
    private UUID projectId;
    private String taskTitle;
    private UUID userId;
    private String content;
    private String eventType;
}
