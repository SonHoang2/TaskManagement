package com.sonhoang2.task_service.task.dto;

import com.sonhoang2.task_service.task.entity.TaskPriority;
import com.sonhoang2.task_service.task.entity.TaskStatus;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class TaskUpdateRequest {

    private UUID projectId;

    @Size(max = 200, message = "title length must be <= 200")
    private String title;

    private String description;

    private TaskStatus status;

    private TaskPriority priority;

    private UUID assigneeId;

    private UUID reporterId;

    private Instant dueDate;

    private Instant startDate;

    private UUID parentTaskId;
}