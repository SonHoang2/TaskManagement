package com.sonhoang2.project_service.project.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class MemberInfo {
    private UUID id;
    private String avatarUrl;
}
