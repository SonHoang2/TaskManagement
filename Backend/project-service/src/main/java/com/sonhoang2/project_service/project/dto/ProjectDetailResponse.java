package com.sonhoang2.project_service.project.dto;

import com.sonhoang2.project_service.project.entity.ProjectMemberRole;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class ProjectDetailResponse {
    private UUID id;
    private String name;
    private String description;
    private OwnerInfo owner;
    private ProjectMemberRole myRole;
    private int memberCount;
    private List<MemberInfo> members;
    private TaskStats taskStats;
    private ActiveSprint activeSprint;
    private Instant createdAt;
    private Instant updatedAt;
}
