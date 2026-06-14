package com.sonhoang2.project_service.project;

import com.sonhoang2.TaskManagementAPI.common.exception.ResourceConflictException;
import com.sonhoang2.TaskManagementAPI.common.exception.ResourceNotFoundException;
import com.sonhoang2.TaskManagementAPI.project.dto.CreateProjectRequest;
import com.sonhoang2.TaskManagementAPI.project.dto.InvitationDecision;
import com.sonhoang2.TaskManagementAPI.project.dto.InvitationDecisionRequest;
import com.sonhoang2.TaskManagementAPI.project.dto.InviteMemberRequest;
import com.sonhoang2.TaskManagementAPI.project.dto.ProjectInvitationResponse;
import com.sonhoang2.TaskManagementAPI.project.dto.ProjectMemberResponse;
import com.sonhoang2.TaskManagementAPI.project.dto.ProjectResponse;
import com.sonhoang2.TaskManagementAPI.project.entity.Project;
import com.sonhoang2.TaskManagementAPI.project.entity.ProjectInvitation;
import com.sonhoang2.TaskManagementAPI.project.entity.ProjectInvitationStatus;
import com.sonhoang2.TaskManagementAPI.project.entity.ProjectMember;
import com.sonhoang2.TaskManagementAPI.project.entity.ProjectMemberRole;
import com.sonhoang2.TaskManagementAPI.user.UserRepository;
import com.sonhoang2.TaskManagementAPI.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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
    private final UserRepository userRepository;

    @Override
    public List<ProjectResponse> listAllProject() {
        return projectRepository.findAll()
                .stream()
                .map(this::toProjectResponse)
                .toList();
    }

    @Override
    public ProjectResponse create(CreateProjectRequest request) {
        User currentUser = getCurrentUserOrThrow();

        Project project = projectRepository.save(Project.builder()
                .name(request.getName())
                .description(request.getDescription())
                .ownerId(currentUser.getId())
                .build());

        projectMemberRepository.save(ProjectMember.builder()
                .projectId(project.getId())
                .userId(currentUser.getId())
                .role(ProjectMemberRole.OWNER)
                .build());

        return toProjectResponse(project);
    }

    @Override
    public ProjectInvitationResponse inviteMember(UUID projectId, InviteMemberRequest request) {
        User currentUser = getCurrentUserOrThrow();
        assertProjectExists(projectId);
        ProjectMember currentMembership = findMembership(projectId, currentUser.getId());

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
                .projectId(projectId)
                .invitedById(currentUser.getId())
                .inviteeId(inviteeId)
                .status(ProjectInvitationStatus.PENDING)
                .build());

        return toInvitationResponse(invitation);
    }

    @Override
    public ProjectInvitationResponse decideInvitation(UUID invitationId, InvitationDecisionRequest request) {
        User currentUser = getCurrentUserOrThrow();
        ProjectInvitation invitation = findInvitationByIdOrThrow(invitationId);

        if (!invitation.getInviteeId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Only the invited user can respond to this invitation");
        }

        if (invitation.getStatus() != ProjectInvitationStatus.PENDING) {
            throw new ResourceConflictException("Invitation is no longer pending");
        }

        if (request.getDecision() == InvitationDecision.ACCEPT) {
            if (projectMemberRepository.existsByProjectIdAndUserId(invitation.getProjectId(), currentUser.getId())) {
                throw new ResourceConflictException("User is already a project member");
            }

            projectMemberRepository.save(ProjectMember.builder()
                    .projectId(invitation.getProjectId())
                    .userId(currentUser.getId())
                    .role(ProjectMemberRole.MEMBER)
                    .build());
            invitation.setStatus(ProjectInvitationStatus.ACCEPTED);
        } else {
            invitation.setStatus(ProjectInvitationStatus.REJECTED);
        }

        invitation.setRespondedAt(Instant.now());
        return toInvitationResponse(invitation);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectMemberResponse> listMembers(UUID projectId) {
        User currentUser = getCurrentUserOrThrow();

        if (!projectMemberRepository.existsByProjectIdAndUserId(projectId, currentUser.getId())) {
            throw new AccessDeniedException("You are not a member of this project");
        }

        return projectMemberRepository.findByProjectIdOrderByJoinedAtAsc(projectId)
                .stream()
                .map(this::toMemberResponse)
                .toList();
    }

    private User getCurrentUserOrThrow() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || !StringUtils.hasText(authentication.getName())) {
            throw new AccessDeniedException("Unauthorized");
        }

        return userRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
    }

    private void assertProjectExists(UUID projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new ResourceNotFoundException("Project with id " + projectId + " not found");
        }
    }

    private void assertUserExists(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User with id " + userId + " not found");
        }
    }

    private ProjectMember findMembership(UUID projectId, UUID userId) {
        return projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new AccessDeniedException("You are not a member of this project"));
    }

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
                .projectId(invitation.getProjectId())
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


