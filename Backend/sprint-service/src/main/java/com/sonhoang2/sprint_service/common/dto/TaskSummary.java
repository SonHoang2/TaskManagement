package com.sonhoang2.sprint_service.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** The subset of task-service's task we need to check which project a task belongs to. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskSummary {
    private UUID id;
    private UUID projectId;
}
