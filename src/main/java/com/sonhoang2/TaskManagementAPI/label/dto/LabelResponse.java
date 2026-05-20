package com.sonhoang2.TaskManagementAPI.label.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabelResponse {

    private UUID id;
    private UUID projectId;
    private String name;
    private String color;
    private Instant createdAt;
}
