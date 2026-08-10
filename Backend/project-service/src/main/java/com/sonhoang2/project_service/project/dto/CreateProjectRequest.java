package com.sonhoang2.project_service.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateProjectRequest {

    @NotBlank(message = "name is required")
    @Size(max = 200, message = "name length must be <= 200")
    private String name;

    private String description;
}

