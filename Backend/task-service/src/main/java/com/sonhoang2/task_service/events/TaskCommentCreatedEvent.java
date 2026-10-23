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
    /** The comment's author. */
    private UUID userId;
    /** The task's assignee and reporter: the people to notify (minus the author). */
    private UUID assigneeId;
    private UUID reporterId;
    private String content;
    private String eventType;
}
