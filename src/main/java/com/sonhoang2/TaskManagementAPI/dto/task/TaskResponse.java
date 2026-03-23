package com.sonhoang2.TaskManagementAPI.dto.task;

import com.sonhoang2.TaskManagementAPI.entity.TaskStatus;
import lombok.Builder;
import lombok.Getter;


import java.time.Instant;
import java.time.LocalDate;

@Builder
@Getter
public class TaskResponse {

    private Long id;
    private String title;
    private String description;
    private TaskStatus status;
    private LocalDate dueDate;
    private Instant createdAt;
    private Instant updatedAt;
}

