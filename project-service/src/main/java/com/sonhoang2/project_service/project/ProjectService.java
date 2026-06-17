package com.sonhoang2.project_service.project;

import com.sonhoang2.project_service.project.dto.CreateProjectRequest;
import com.sonhoang2.project_service.project.dto.InvitationDecisionRequest;
import com.sonhoang2.project_service.project.dto.InviteMemberRequest;
import com.sonhoang2.project_service.project.dto.ProjectInvitationResponse;
import com.sonhoang2.project_service.project.dto.ProjectMemberResponse;
import com.sonhoang2.project_service.project.dto.ProjectResponse;

import java.util.List;
import java.util.UUID;

public interface ProjectService {
    List<ProjectResponse> listAllProject();

    ProjectResponse create(CreateProjectRequest request, UUID userId);          // added userId

    ProjectInvitationResponse inviteMember(UUID projectId, InviteMemberRequest request, UUID userId);

    ProjectInvitationResponse decideInvitation(UUID invitationId, InvitationDecisionRequest request, UUID userId);

    List<ProjectMemberResponse> listMembers(UUID projectId, UUID userId);       // added userId
}