package com.sonhoang2.TaskManagementAPI.label.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class TaskLabelResponse {

    private UUID taskId;
    private UUID labelId;
}



