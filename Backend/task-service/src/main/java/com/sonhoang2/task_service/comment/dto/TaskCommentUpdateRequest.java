package com.sonhoang2.task_service.comment.dto;
import lombok.Data;

import java.util.UUID;

@Data
public class TaskCommentUpdateRequest {
    private UUID userId;
    private String content;
}