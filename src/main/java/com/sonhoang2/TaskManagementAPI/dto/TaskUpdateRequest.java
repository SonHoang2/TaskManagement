package com.sonhoang2.TaskManagementAPI.dto;

import com.sonhoang2.TaskManagementAPI.entity.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public class TaskUpdateRequest {

    @NotBlank(message = "title is required")
    @Size(max = 200, message = "title length must be <= 200")
    private String title;

    @Size(max = 2000, message = "description length must be <= 2000")
    private String description;

    @NotNull(message = "status is required")
    private TaskStatus status;

    private LocalDate dueDate;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }
}

