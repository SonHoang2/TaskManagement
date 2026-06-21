package com.sonhoang2.task_service.task.dto;

import com.sonhoang2.task_service.task.entity.TaskPriority;
import com.sonhoang2.task_service.task.entity.TaskStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Builder
@Getter
public class TaskResponse {

    private UUID id;
    private UUID projectId;
    private String title;
    private String description;
    private TaskStatus status;
    private TaskPriority priority;
    private UUID assigneeId;
    private UUID reporterId;
    private Instant dueDate;
    private Instant startDate;
    private UUID parentTaskId;
    private Instant createdAt;
    private Instant updatedAt;
}
