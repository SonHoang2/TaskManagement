package com.sonhoang2.sprint_service.sprints.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SprintUpdateRequest {

    @NotBlank(message = "Name is required")
    private String name;

    private LocalDate startDate;

    private LocalDate endDate;
}
