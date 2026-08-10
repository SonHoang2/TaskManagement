package com.sonhoang2.project_service.project.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TaskStats {
    private int total;
    private int todo;
    private int inProgress;
    private int done;
}
