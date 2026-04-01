package com.sonhoang2.TaskManagementAPI.project;

import com.sonhoang2.TaskManagementAPI.project.dto.CreateProjectRequest;
import com.sonhoang2.TaskManagementAPI.project.dto.InvitationDecisionRequest;
import com.sonhoang2.TaskManagementAPI.project.dto.InviteMemberRequest;
import com.sonhoang2.TaskManagementAPI.project.dto.ProjectInvitationResponse;
import com.sonhoang2.TaskManagementAPI.project.dto.ProjectMemberResponse;
import com.sonhoang2.TaskManagementAPI.project.dto.ProjectResponse;

import java.util.List;
import java.util.UUID;

public interface ProjectService {

    ProjectResponse create(CreateProjectRequest request);

    ProjectInvitationResponse inviteMember(UUID projectId, InviteMemberRequest request);

    ProjectInvitationResponse decideInvitation(UUID invitationId, InvitationDecisionRequest request);

    List<ProjectMemberResponse> listMembers(UUID projectId);
}

