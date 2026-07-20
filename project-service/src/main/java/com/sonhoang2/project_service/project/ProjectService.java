package com.sonhoang2.project_service.project;

import com.sonhoang2.project_service.common.dto.PageResponse;
import com.sonhoang2.project_service.project.dto.CreateProjectRequest;
import com.sonhoang2.project_service.project.dto.InvitationDecisionRequest;
import com.sonhoang2.project_service.project.dto.InviteMemberRequest;
import com.sonhoang2.project_service.project.dto.ListProjectRequest;
import com.sonhoang2.project_service.project.dto.ProjectDetailResponse;
import com.sonhoang2.project_service.project.dto.ProjectInvitationResponse;
import com.sonhoang2.project_service.project.dto.ProjectMemberResponse;
import com.sonhoang2.project_service.project.dto.ProjectResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface ProjectService {
    PageResponse<ProjectDetailResponse> listAllProject(Pageable pageable, UUID userId, ListProjectRequest request);

    ProjectResponse create(CreateProjectRequest request, UUID userId);          // added userId

    ProjectResponse getProjectById(UUID id, UUID userId);

    ProjectInvitationResponse inviteMember(UUID projectId, InviteMemberRequest request, UUID userId);

    ProjectInvitationResponse decideInvitation(UUID invitationId, InvitationDecisionRequest request, UUID userId);

    List<ProjectMemberResponse> listMembers(UUID projectId, UUID userId);       // added userId

    List<ProjectInvitationResponse> listInvitations(UUID projectId, UUID userId);

    List<ProjectInvitationResponse> listInvitationsByInvitee(UUID userId);
}