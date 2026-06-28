package com.sonhoang2.project_service.project;

import com.sonhoang2.project_service.common.exception.ResourceConflictException;
import com.sonhoang2.project_service.common.exception.ResourceNotFoundException;
import com.sonhoang2.project_service.project.dto.CreateProjectRequest;
import com.sonhoang2.project_service.project.dto.InvitationDecision;
import com.sonhoang2.project_service.project.dto.InvitationDecisionRequest;
import com.sonhoang2.project_service.project.dto.InviteMemberRequest;
import com.sonhoang2.project_service.project.dto.ProjectInvitationResponse;
import com.sonhoang2.project_service.project.dto.ProjectMemberResponse;
import com.sonhoang2.project_service.project.dto.ProjectResponse;
import com.sonhoang2.project_service.project.entity.Project;
import com.sonhoang2.project_service.project.entity.ProjectInvitation;
import com.sonhoang2.project_service.project.entity.ProjectInvitationStatus;
import com.sonhoang2.project_service.project.entity.ProjectMember;
import com.sonhoang2.project_service.project.entity.ProjectMemberRole;
import com.sonhoang2.project_service.project.feign.UserServiceClient;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectInvitationRepository projectInvitationRepository;
    private final UserServiceClient userServiceClient;

    private void assertUserExists(UUID userId) {
        try {
            userServiceClient.findById(userId);
        } catch (FeignException.NotFound e) {
            throw new ResourceNotFoundException("User with id " + userId + " not found");
        }
    }

    @Override
    public List<ProjectResponse> listAllProject() {
        return projectRepository.findAll().stream().map(this::toProjectResponse).toList();
    }

    @Override
    public ProjectResponse create(CreateProjectRequest request, UUID userId) {
        // Use the passed userId for the owner
        Project project = projectRepository.save(Project.builder()
                .name(request.getName())
                .description(request.getDescription())
                .ownerId(userId)
                .build());

        projectMemberRepository.save(ProjectMember.builder()
                .project(project)
                .userId(userId)
                .role(ProjectMemberRole.OWNER)
                .build());

        return toProjectResponse(project);
    }

    @Override
    public ProjectInvitationResponse inviteMember(UUID projectId, InviteMemberRequest request, UUID userId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project with id " + projectId + " not found"));

        // Check current user's membership and role using the passed userId
        ProjectMember currentMembership = projectMemberRepository
                .findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new AccessDeniedException("You are not a member of this project"));

        if (currentMembership.getRole() != ProjectMemberRole.OWNER && currentMembership.getRole() != ProjectMemberRole.ADMIN) {
            throw new AccessDeniedException("Only owner or admin can invite members");
        }

        UUID inviteeId = request.getUserId();
        assertUserExists(inviteeId);

        if (projectMemberRepository.existsByProjectIdAndUserId(projectId, inviteeId)) {
            throw new ResourceConflictException("User is already a project member");
        }

        if (projectInvitationRepository.existsByProjectIdAndInviteeIdAndStatus(projectId,
                inviteeId,
                ProjectInvitationStatus.PENDING)) {
            throw new ResourceConflictException("A pending invitation already exists for this user");
        }

        ProjectInvitation invitation = projectInvitationRepository.save(ProjectInvitation.builder()
                .project(project)
                .invitedById(userId)          // the one who invites (current user)
                .inviteeId(inviteeId)
                .status(ProjectInvitationStatus.PENDING)
                .build());

        return toInvitationResponse(invitation);
    }

    @Override
    public ProjectInvitationResponse decideInvitation(UUID invitationId,
                                                      InvitationDecisionRequest request,
                                                      UUID userId) {
        ProjectInvitation invitation = findInvitationByIdOrThrow(invitationId);

        if (!invitation.getInviteeId().equals(userId)) {
            throw new AccessDeniedException("Only the invited user can respond to this invitation");
        }

        if (invitation.getStatus() != ProjectInvitationStatus.PENDING) {
            throw new ResourceConflictException("Invitation is no longer pending");
        }

        if (request.getDecision() == InvitationDecision.ACCEPT) {
            if (projectMemberRepository.existsByProjectIdAndUserId(invitation.getProject().getId(), userId)) {
                throw new ResourceConflictException("User is already a project member");
            }

            projectMemberRepository.save(ProjectMember.builder()
                    .project(invitation.getProject())
                    .userId(userId)
                    .role(ProjectMemberRole.MEMBER)
                    .build());

            invitation.setStatus(ProjectInvitationStatus.ACCEPTED);
        } else {
            invitation.setStatus(ProjectInvitationStatus.REJECTED);
        }

        invitation.setRespondedAt(Instant.now());

        projectInvitationRepository.save(invitation);

        return toInvitationResponse(invitation);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectMemberResponse> listMembers(UUID projectId, UUID userId) {
        // Verify that the requesting user is a member
        if (!projectMemberRepository.existsByProjectIdAndUserId(projectId, userId)) {
            throw new AccessDeniedException("You are not a member of this project");
        }

        return projectMemberRepository.findByProjectIdOrderByJoinedAtAsc(projectId)
                .stream()
                .map(this::toMemberResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectInvitationResponse> listInvitations(UUID projectId, UUID userId) {
        // Verify that the requesting user is a member
        if (!projectMemberRepository.existsByProjectIdAndUserId(projectId, userId)) {
            throw new AccessDeniedException("You are not a member of this project");
        }

        return projectInvitationRepository.findByProjectId(projectId)
                .stream()
                .map(this::toInvitationResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectInvitationResponse> listInvitationsByInvitee(UUID userId) {
        return projectInvitationRepository.findByInviteeId(userId)
                .stream()
                .map(this::toInvitationResponse)
                .toList();
    }

    // Helper methods (unchanged)
    private ProjectInvitation findInvitationByIdOrThrow(UUID invitationId) {
        return projectInvitationRepository.findById(invitationId)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation with id " + invitationId + " not found"));
    }

    private ProjectResponse toProjectResponse(Project project) {
        return ProjectResponse.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .ownerId(project.getOwnerId())
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .build();
    }

    private ProjectInvitationResponse toInvitationResponse(ProjectInvitation invitation) {
        return ProjectInvitationResponse.builder()
                .id(invitation.getId())
                .projectId(invitation.getProject().getId())
                .invitedById(invitation.getInvitedById())
                .inviteeId(invitation.getInviteeId())
                .status(invitation.getStatus())
                .createdAt(invitation.getCreatedAt())
                .respondedAt(invitation.getRespondedAt())
                .build();
    }

    private ProjectMemberResponse toMemberResponse(ProjectMember member) {
        return ProjectMemberResponse.builder()
                .userId(member.getUserId())
                .role(member.getRole())
                .joinedAt(member.getJoinedAt())
                .build();
    }
}