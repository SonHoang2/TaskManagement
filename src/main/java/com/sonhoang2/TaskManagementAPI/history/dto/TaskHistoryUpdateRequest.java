package com.sonhoang2.TaskManagementAPI.history.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class TaskHistoryUpdateRequest {

    private UUID taskId;

    private UUID changedBy;

    @Size(max = 50, message = "field length must be <= 50")
    private String field;

    private String oldValue;

    private String newValue;
}



