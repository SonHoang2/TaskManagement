package com.sonhoang2.TaskManagementAPI.tasklabel.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class TaskLabelUpdateRequest {

    private UUID taskId;

    private UUID labelId;
}



