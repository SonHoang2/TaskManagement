package com.sonhoang2.task_service.history.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TaskHistoryUpdateRequest {

    @Size(max = 50, message = "field length must be <= 50")
    private String field;

    private String oldValue;

    private String newValue;
}



