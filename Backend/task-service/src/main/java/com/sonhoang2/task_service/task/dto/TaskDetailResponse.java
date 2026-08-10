package com.sonhoang2.task_service.task.dto;

import com.sonhoang2.task_service.task.entity.TaskPriority;
import com.sonhoang2.task_service.task.entity.TaskStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class TaskDetailResponse {

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
    private List<TaskCommentResponse> comments;
    private List<TaskAttachmentResponse> attachments;
    private List<TaskLabelResponse> labels;
}
