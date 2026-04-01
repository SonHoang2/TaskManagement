package com.sonhoang2.TaskManagementAPI.project;

import com.sonhoang2.TaskManagementAPI.common.exception.ResourceConflictException;
import com.sonhoang2.TaskManagementAPI.project.dto.CreateProjectRequest;
import com.sonhoang2.TaskManagementAPI.project.dto.InvitationDecision;
import com.sonhoang2.TaskManagementAPI.project.dto.InvitationDecisionRequest;
import com.sonhoang2.TaskManagementAPI.project.dto.InviteMemberRequest;
import com.sonhoang2.TaskManagementAPI.project.dto.ProjectInvitationResponse;
import com.sonhoang2.TaskManagementAPI.project.dto.ProjectResponse;
import com.sonhoang2.TaskManagementAPI.project.entity.Project;
import com.sonhoang2.TaskManagementAPI.project.entity.ProjectInvitation;
import com.sonhoang2.TaskManagementAPI.project.entity.ProjectInvitationStatus;
import com.sonhoang2.TaskManagementAPI.project.entity.ProjectMember;
import com.sonhoang2.TaskManagementAPI.project.entity.ProjectMemberRole;
import com.sonhoang2.TaskManagementAPI.user.UserRepository;
import com.sonhoang2.TaskManagementAPI.user.entity.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectServiceImplTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private ProjectInvitationRepository projectInvitationRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ProjectServiceImpl projectService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createShouldAutoInsertOwnerMember() {
        UUID ownerId = UUID.randomUUID();
        authenticateAs("owner@example.com");

        User owner = User.builder().id(ownerId).email("owner@example.com").build();
        when(userRepository.findByEmailIgnoreCase("owner@example.com")).thenReturn(Optional.of(owner));
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> {
            Project project = invocation.getArgument(0);
            project.setId(UUID.randomUUID());
            return project;
        });

        CreateProjectRequest request = new CreateProjectRequest();
        request.setName("Platform");
        request.setDescription("Internal tooling");

        ProjectResponse response = projectService.create(request);

        ArgumentCaptor<ProjectMember> memberCaptor = ArgumentCaptor.forClass(ProjectMember.class);
        verify(projectMemberRepository).save(memberCaptor.capture());

        ProjectMember savedMember = memberCaptor.getValue();
        assertEquals(response.getId(), savedMember.getProjectId());
        assertEquals(ownerId, savedMember.getUserId());
        assertEquals(ProjectMemberRole.OWNER, savedMember.getRole());
    }

    @Test
    void inviteMemberShouldCreatePendingInvitation() {
        UUID projectId = UUID.randomUUID();
        UUID inviterId = UUID.randomUUID();
        UUID inviteeId = UUID.randomUUID();
        authenticateAs("owner@example.com");

        when(userRepository.findByEmailIgnoreCase("owner@example.com"))
                .thenReturn(Optional.of(User.builder().id(inviterId).email("owner@example.com").build()));
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(Project.builder().id(projectId).build()));
        when(projectMemberRepository.findByProjectIdAndUserId(projectId, inviterId))
                .thenReturn(Optional.of(ProjectMember.builder().projectId(projectId).userId(inviterId).role(ProjectMemberRole.OWNER).build()));
        when(userRepository.findById(inviteeId)).thenReturn(Optional.of(User.builder().id(inviteeId).build()));
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, inviteeId)).thenReturn(false);
        when(projectInvitationRepository.existsByProjectIdAndInviteeIdAndStatus(
                projectId,
                inviteeId,
                ProjectInvitationStatus.PENDING
        )).thenReturn(false);
        when(projectInvitationRepository.save(any(ProjectInvitation.class))).thenAnswer(invocation -> {
            ProjectInvitation invitation = invocation.getArgument(0);
            invitation.setId(UUID.randomUUID());
            return invitation;
        });

        InviteMemberRequest request = new InviteMemberRequest();
        request.setUserId(inviteeId);

        ProjectInvitationResponse response = projectService.inviteMember(projectId, request);

        assertEquals(projectId, response.getProjectId());
        assertEquals(inviteeId, response.getInviteeId());
        assertEquals(ProjectInvitationStatus.PENDING, response.getStatus());
    }

    @Test
    void decideInvitationShouldAddMemberWhenAccepted() {
        UUID invitationId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID inviteeId = UUID.randomUUID();
        authenticateAs("member@example.com");

        when(userRepository.findByEmailIgnoreCase("member@example.com"))
                .thenReturn(Optional.of(User.builder().id(inviteeId).email("member@example.com").build()));

        ProjectInvitation invitation = ProjectInvitation.builder()
                .id(invitationId)
                .projectId(projectId)
                .inviteeId(inviteeId)
                .status(ProjectInvitationStatus.PENDING)
                .build();
        when(projectInvitationRepository.findById(invitationId)).thenReturn(Optional.of(invitation));
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, inviteeId)).thenReturn(false);

        InvitationDecisionRequest request = new InvitationDecisionRequest();
        request.setDecision(InvitationDecision.ACCEPT);

        ProjectInvitationResponse response = projectService.decideInvitation(invitationId, request);

        verify(projectMemberRepository).save(any(ProjectMember.class));
        assertEquals(ProjectInvitationStatus.ACCEPTED, response.getStatus());
    }

    @Test
    void decideInvitationShouldRejectAlreadyProcessedInvitation() {
        UUID invitationId = UUID.randomUUID();
        UUID inviteeId = UUID.randomUUID();
        authenticateAs("member@example.com");

        when(userRepository.findByEmailIgnoreCase("member@example.com"))
                .thenReturn(Optional.of(User.builder().id(inviteeId).email("member@example.com").build()));

        ProjectInvitation invitation = ProjectInvitation.builder()
                .id(invitationId)
                .projectId(UUID.randomUUID())
                .inviteeId(inviteeId)
                .status(ProjectInvitationStatus.REJECTED)
                .build();
        when(projectInvitationRepository.findById(invitationId)).thenReturn(Optional.of(invitation));

        InvitationDecisionRequest request = new InvitationDecisionRequest();
        request.setDecision(InvitationDecision.ACCEPT);

        assertThrows(ResourceConflictException.class, () -> projectService.decideInvitation(invitationId, request));
        verify(projectMemberRepository, never()).save(any(ProjectMember.class));
    }

    @Test
    void listMembersShouldDenyWhenUserIsNotMember() {
        UUID projectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        authenticateAs("outsider@example.com");

        when(userRepository.findByEmailIgnoreCase("outsider@example.com"))
                .thenReturn(Optional.of(User.builder().id(userId).email("outsider@example.com").build()));
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, userId)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> projectService.listMembers(projectId));
        verify(projectMemberRepository, never()).findByProjectIdOrderByJoinedAtAsc(projectId);
    }

    @Test
    void listMembersShouldReturnMembersWhenAuthorized() {
        UUID projectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        authenticateAs("member@example.com");

        when(userRepository.findByEmailIgnoreCase("member@example.com"))
                .thenReturn(Optional.of(User.builder().id(userId).email("member@example.com").build()));
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, userId)).thenReturn(true);
        when(projectMemberRepository.findByProjectIdOrderByJoinedAtAsc(projectId)).thenReturn(List.of(
                ProjectMember.builder().projectId(projectId).userId(userId).role(ProjectMemberRole.MEMBER).build()
        ));

        assertEquals(1, projectService.listMembers(projectId).size());
    }

    private void authenticateAs(String email) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, "n/a", AuthorityUtils.NO_AUTHORITIES)
        );
    }
}


