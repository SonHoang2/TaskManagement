package com.sonhoang2.task_service.task.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class TaskHistoryResponse {

    private UUID id;
    private UUID taskId;
    private UUID changedBy;
    private String field;
    private String oldValue;
    private String newValue;
    private Instant createdAt;
}

