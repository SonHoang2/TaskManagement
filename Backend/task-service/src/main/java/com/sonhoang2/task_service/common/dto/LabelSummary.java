package com.sonhoang2.task_service.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** The subset of project-service's label we need to check which project a label belongs to. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabelSummary {
    private UUID id;
    private UUID projectId;
}
