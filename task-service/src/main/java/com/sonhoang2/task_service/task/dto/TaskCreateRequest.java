package com.sonhoang2.task_service.task.dto;

import com.sonhoang2.task_service.task.entity.TaskPriority;
import com.sonhoang2.task_service.task.entity.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class TaskCreateRequest {

    @NotNull(message = "projectId is required")
    private UUID projectId;

    @NotBlank(message = "title is required")
    @Size(max = 200, message = "title length must be <= 200")
    private String title;

    private String description;

    @NotNull(message = "status is required")
    private TaskStatus status;

    private TaskPriority priority;

    private UUID assigneeId;

    private UUID reporterId;

    private Instant dueDate;

    private Instant startDate;

    private UUID parentTaskId;
}
