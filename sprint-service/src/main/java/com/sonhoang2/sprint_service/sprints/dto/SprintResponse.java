package com.sonhoang2.sprint_service.sprints.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SprintResponse {

    private UUID id;
    private UUID projectId;
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    private Instant createdAt;
}
