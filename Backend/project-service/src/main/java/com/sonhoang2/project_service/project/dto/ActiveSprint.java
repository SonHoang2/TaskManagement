package com.sonhoang2.project_service.project.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class ActiveSprint {
    private UUID id;
    private String name;
    private LocalDate endDate;
}
