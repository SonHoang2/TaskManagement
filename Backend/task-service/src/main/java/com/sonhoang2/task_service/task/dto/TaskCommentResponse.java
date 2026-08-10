package com.sonhoang2.task_service.task.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class TaskCommentResponse {

    private UUID id;
    private UUID taskId;
    private UUID userId;
    private String content;
    private Instant createdAt;
}

