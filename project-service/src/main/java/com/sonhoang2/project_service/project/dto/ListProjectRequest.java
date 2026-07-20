package com.sonhoang2.project_service.project.dto;

import lombok.Data;

@Data
public class ListProjectRequest {
    private String search;
    private String sortBy;
    private String sortDirection;
}
