package com.sonhoang2.project_service.label.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabelUpdateRequest {

    @Size(max = 50, message = "Name must not exceed 50 characters")
    private String name;

    @Size(max = 20, message = "Color must not exceed 20 characters")
    private String color;
}
