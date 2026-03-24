package com.sonhoang2.TaskManagementAPI.task.dto;

import com.sonhoang2.TaskManagementAPI.task.entity.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class TaskUpdateRequest {

    @NotBlank(message = "title is required")
    @Size(max = 200, message = "title length must be <= 200")
    private String title;

    @Size(max = 2000, message = "description length must be <= 2000")
    private String description;

    @NotNull(message = "status is required")
    private TaskStatus status;

    private LocalDate dueDate;
}