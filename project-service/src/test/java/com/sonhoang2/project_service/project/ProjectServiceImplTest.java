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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceImplTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private ProjectInvitationRepository projectInvitationRepository;

    @Mock
    private UserServiceClient userServiceClient;

    @InjectMocks
    private ProjectServiceImpl projectService;

    private UUID projectId;
    private UUID userId;
    private UUID inviteeId;
    private UUID invitationId;
    private Project project;
    private ProjectMember projectMember;
    private ProjectInvitation projectInvitation;

    @BeforeEach
    void setUp() {
        projectId = UUID.randomUUID();
        userId = UUID.randomUUID();
        inviteeId = UUID.randomUUID();
        invitationId = UUID.randomUUID();

        project = Project.builder()
                .id(projectId)
                .name("Test Project")
                .description("Test Description")
                .ownerId(userId)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        projectMember = ProjectMember.builder()
                .id(UUID.randomUUID())
                .project(project)
                .userId(userId)
                .role(ProjectMemberRole.OWNER)
                .joinedAt(Instant.now())
                .build();

        projectInvitation = ProjectInvitation.builder()
                .id(invitationId)
                .project(project)
                .invitedById(userId)
                .inviteeId(inviteeId)
                .status(ProjectInvitationStatus.PENDING)
                .createdAt(Instant.now())
                .build();
    }

//    @Test
//    void listAllProject_ShouldReturnAllProjects() {
//        // Arrange
//        List<Project> projects = List.of(project);
//        when(projectRepository.findAll()).thenReturn(projects);
//
//        // Act
//        List<ProjectResponse> result = projectService.listAllProject();
//
//        // Assert
//        assertNotNull(result);
//        assertEquals(1, result.size());
//        assertEquals(projectId, result.get(0).getId());
//        assertEquals("Test Project", result.get(0).getName());
//        verify(projectRepository, times(1)).findAll();
//    }

    @Test
    void create_ShouldCreateProjectAndAddOwnerAsMember() {
        // Arrange
        CreateProjectRequest request = new CreateProjectRequest();
        request.setName("New Project");
        request.setDescription("New Description");

        when(projectRepository.save(any(Project.class))).thenReturn(project);
        when(projectMemberRepository.save(any(ProjectMember.class))).thenReturn(projectMember);

        // Act
        ProjectResponse result = projectService.create(request, userId);

        // Assert
        assertNotNull(result);
        assertEquals(projectId, result.getId());
        assertEquals("Test Project", result.getName());
        assertEquals(userId, result.getOwnerId());
        verify(projectRepository, times(1)).save(any(Project.class));
        verify(projectMemberRepository, times(1)).save(any(ProjectMember.class));
    }

    @Test
    void inviteMember_ShouldCreateInvitation_WhenUserIsOwner() {
        // Arrange
        InviteMemberRequest request = new InviteMemberRequest();
        request.setUserId(inviteeId);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectMemberRepository.findByProjectIdAndUserId(projectId, userId))
                .thenReturn(Optional.of(projectMember));
        when(projectInvitationRepository.existsByProjectIdAndInviteeIdAndStatus(
                projectId, inviteeId, ProjectInvitationStatus.PENDING)).thenReturn(false);
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, inviteeId)).thenReturn(false);
        when(projectInvitationRepository.save(any(ProjectInvitation.class))).thenReturn(projectInvitation);

        // Act
        ProjectInvitationResponse result = projectService.inviteMember(projectId, request, userId);

        // Assert
        assertNotNull(result);
        assertEquals(invitationId, result.getId());
        assertEquals(projectId, result.getProjectId());
        assertEquals(userId, result.getInvitedById());
        assertEquals(inviteeId, result.getInviteeId());
        assertEquals(ProjectInvitationStatus.PENDING, result.getStatus());
        verify(projectInvitationRepository, times(1)).save(any(ProjectInvitation.class));
    }

    @Test
    void inviteMember_ShouldCreateInvitation_WhenUserIsAdmin() {
        // Arrange
        ProjectMember adminMember = ProjectMember.builder()
                .id(UUID.randomUUID())
                .project(project)
                .userId(userId)
                .role(ProjectMemberRole.ADMIN)
                .joinedAt(Instant.now())
                .build();

        InviteMemberRequest request = new InviteMemberRequest();
        request.setUserId(inviteeId);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectMemberRepository.findByProjectIdAndUserId(projectId, userId))
                .thenReturn(Optional.of(adminMember));
        when(projectInvitationRepository.existsByProjectIdAndInviteeIdAndStatus(
                projectId, inviteeId, ProjectInvitationStatus.PENDING)).thenReturn(false);
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, inviteeId)).thenReturn(false);
        when(projectInvitationRepository.save(any(ProjectInvitation.class))).thenReturn(projectInvitation);

        // Act
        ProjectInvitationResponse result = projectService.inviteMember(projectId, request, userId);

        // Assert
        assertNotNull(result);
        verify(projectInvitationRepository, times(1)).save(any(ProjectInvitation.class));
    }

    @Test
    void inviteMember_ShouldThrowException_WhenProjectNotFound() {
        // Arrange
        InviteMemberRequest request = new InviteMemberRequest();
        request.setUserId(inviteeId);

        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () ->
                projectService.inviteMember(projectId, request, userId));
        verify(projectInvitationRepository, never()).save(any(ProjectInvitation.class));
    }

    @Test
    void inviteMember_ShouldThrowException_WhenUserIsNotMember() {
        // Arrange
        InviteMemberRequest request = new InviteMemberRequest();
        request.setUserId(inviteeId);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectMemberRepository.findByProjectIdAndUserId(projectId, userId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(AccessDeniedException.class, () ->
                projectService.inviteMember(projectId, request, userId));
        verify(projectInvitationRepository, never()).save(any(ProjectInvitation.class));
    }

    @Test
    void inviteMember_ShouldThrowException_WhenUserIsRegularMember() {
        // Arrange
        ProjectMember regularMember = ProjectMember.builder()
                .id(UUID.randomUUID())
                .project(project)
                .userId(userId)
                .role(ProjectMemberRole.MEMBER)
                .joinedAt(Instant.now())
                .build();

        InviteMemberRequest request = new InviteMemberRequest();
        request.setUserId(inviteeId);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectMemberRepository.findByProjectIdAndUserId(projectId, userId))
                .thenReturn(Optional.of(regularMember));

        // Act & Assert
        assertThrows(AccessDeniedException.class, () ->
                projectService.inviteMember(projectId, request, userId));
        verify(projectInvitationRepository, never()).save(any(ProjectInvitation.class));
    }

    @Test
    void inviteMember_ShouldThrowException_WhenUserNotFound() {
        // Arrange
        InviteMemberRequest request = new InviteMemberRequest();
        request.setUserId(inviteeId);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectMemberRepository.findByProjectIdAndUserId(projectId, userId))
                .thenReturn(Optional.of(projectMember));
        when(userServiceClient.findById(inviteeId)).thenThrow(FeignException.NotFound.class);

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () ->
                projectService.inviteMember(projectId, request, userId));
        verify(projectInvitationRepository, never()).save(any(ProjectInvitation.class));
    }

    @Test
    void inviteMember_ShouldThrowException_WhenUserAlreadyMember() {
        // Arrange
        InviteMemberRequest request = new InviteMemberRequest();
        request.setUserId(inviteeId);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectMemberRepository.findByProjectIdAndUserId(projectId, userId))
                .thenReturn(Optional.of(projectMember));
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, inviteeId)).thenReturn(true);

        // Act & Assert
        assertThrows(ResourceConflictException.class, () ->
                projectService.inviteMember(projectId, request, userId));
        verify(projectInvitationRepository, never()).save(any(ProjectInvitation.class));
    }

    @Test
    void inviteMember_ShouldThrowException_WhenPendingInvitationExists() {
        // Arrange
        InviteMemberRequest request = new InviteMemberRequest();
        request.setUserId(inviteeId);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectMemberRepository.findByProjectIdAndUserId(projectId, userId))
                .thenReturn(Optional.of(projectMember));
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, inviteeId)).thenReturn(false);
        when(projectInvitationRepository.existsByProjectIdAndInviteeIdAndStatus(
                projectId, inviteeId, ProjectInvitationStatus.PENDING)).thenReturn(true);

        // Act & Assert
        assertThrows(ResourceConflictException.class, () ->
                projectService.inviteMember(projectId, request, userId));
        verify(projectInvitationRepository, never()).save(any(ProjectInvitation.class));
    }

    @Test
    void decideInvitation_ShouldAcceptInvitation_WhenDecisionIsAccept() {
        // Arrange
        InvitationDecisionRequest request = new InvitationDecisionRequest();
        request.setDecision(InvitationDecision.ACCEPT);

        when(projectInvitationRepository.findById(invitationId)).thenReturn(Optional.of(projectInvitation));
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, inviteeId)).thenReturn(false);
        when(projectMemberRepository.save(any(ProjectMember.class))).thenReturn(projectMember);
        when(projectInvitationRepository.save(any(ProjectInvitation.class))).thenReturn(projectInvitation); // Thêm dòng này

        // Act
        ProjectInvitationResponse result = projectService.decideInvitation(invitationId, request, inviteeId);

        // Assert
        assertNotNull(result);
        assertEquals(ProjectInvitationStatus.ACCEPTED, result.getStatus());
        assertNotNull(result.getRespondedAt());
        verify(projectMemberRepository, times(1)).save(any(ProjectMember.class));
        verify(projectInvitationRepository, times(1)).save(any(ProjectInvitation.class)); // Thêm verify này
    }

    @Test
    void decideInvitation_ShouldRejectInvitation_WhenDecisionIsReject() {
        // Arrange
        InvitationDecisionRequest request = new InvitationDecisionRequest();
        request.setDecision(InvitationDecision.REJECT);

        when(projectInvitationRepository.findById(invitationId)).thenReturn(Optional.of(projectInvitation));
        when(projectInvitationRepository.save(any(ProjectInvitation.class))).thenReturn(projectInvitation);

        // Act
        ProjectInvitationResponse result = projectService.decideInvitation(invitationId, request, inviteeId);

        // Assert
        assertNotNull(result);
        assertEquals(ProjectInvitationStatus.REJECTED, result.getStatus());
        assertNotNull(result.getRespondedAt());
        verify(projectMemberRepository, never()).save(any(ProjectMember.class));
        verify(projectInvitationRepository, times(1)).save(any(ProjectInvitation.class));
    }

    @Test
    void decideInvitation_ShouldThrowException_WhenInvitationNotFound() {
        // Arrange
        InvitationDecisionRequest request = new InvitationDecisionRequest();
        request.setDecision(InvitationDecision.ACCEPT);

        when(projectInvitationRepository.findById(invitationId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () ->
                projectService.decideInvitation(invitationId, request, inviteeId));
        verify(projectInvitationRepository, never()).save(any(ProjectInvitation.class));
    }

    @Test
    void decideInvitation_ShouldThrowException_WhenUserIsNotInvitee() {
        // Arrange
        UUID differentUserId = UUID.randomUUID();
        InvitationDecisionRequest request = new InvitationDecisionRequest();
        request.setDecision(InvitationDecision.ACCEPT);

        when(projectInvitationRepository.findById(invitationId)).thenReturn(Optional.of(projectInvitation));

        // Act & Assert
        assertThrows(AccessDeniedException.class, () ->
                projectService.decideInvitation(invitationId, request, differentUserId));
        verify(projectInvitationRepository, never()).save(any(ProjectInvitation.class));
    }

    @Test
    void decideInvitation_ShouldThrowException_WhenInvitationNotPending() {
        // Arrange
        projectInvitation.setStatus(ProjectInvitationStatus.ACCEPTED);
        InvitationDecisionRequest request = new InvitationDecisionRequest();
        request.setDecision(InvitationDecision.ACCEPT);

        when(projectInvitationRepository.findById(invitationId)).thenReturn(Optional.of(projectInvitation));

        // Act & Assert
        assertThrows(ResourceConflictException.class, () ->
                projectService.decideInvitation(invitationId, request, inviteeId));
        verify(projectInvitationRepository, never()).save(any(ProjectInvitation.class));
    }

    @Test
    void decideInvitation_ShouldThrowException_WhenUserAlreadyMember() {
        // Arrange
        InvitationDecisionRequest request = new InvitationDecisionRequest();
        request.setDecision(InvitationDecision.ACCEPT);

        when(projectInvitationRepository.findById(invitationId)).thenReturn(Optional.of(projectInvitation));
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, inviteeId)).thenReturn(true);

        // Act & Assert
        assertThrows(ResourceConflictException.class, () ->
                projectService.decideInvitation(invitationId, request, inviteeId));
        verify(projectMemberRepository, never()).save(any(ProjectMember.class));
    }

    @Test
    void listMembers_ShouldReturnMembers_WhenUserIsMember() {
        // Arrange
        List<ProjectMember> members = List.of(projectMember);
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, userId)).thenReturn(true);
        when(projectMemberRepository.findByProjectIdOrderByJoinedAtAsc(projectId)).thenReturn(members);

        // Act
        List<ProjectMemberResponse> result = projectService.listMembers(projectId, userId);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(userId, result.get(0).getUserId());
        assertEquals(ProjectMemberRole.OWNER, result.get(0).getRole());
        verify(projectMemberRepository, times(1)).findByProjectIdOrderByJoinedAtAsc(projectId);
    }

    @Test
    void listMembers_ShouldThrowException_WhenUserIsNotMember() {
        // Arrange
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, userId)).thenReturn(false);

        // Act & Assert
        assertThrows(AccessDeniedException.class, () ->
                projectService.listMembers(projectId, userId));
        verify(projectMemberRepository, never()).findByProjectIdOrderByJoinedAtAsc(any());
    }
}
