package com.sonhoang2.task_service.common.security;

import com.sonhoang2.task_service.common.dto.JSendResponse;
import com.sonhoang2.task_service.common.dto.ProjectMembership;
import com.sonhoang2.task_service.common.exception.ResourceNotFoundException;
import com.sonhoang2.task_service.feign.ProjectServiceClient;
import com.sonhoang2.task_service.task.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectAccessGuardTest {

    @Mock
    private ProjectServiceClient projectServiceClient;

    @Mock
    private TaskRepository taskRepository;

    private ProjectAccessGuard guard;

    private final UUID userId = UUID.randomUUID();
    private final UUID projectId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        guard = new ProjectAccessGuard(projectServiceClient, taskRepository);
    }

    @Test
    void requireMember_ShouldReturnRoleAndReuseCachedMemberships() {
        when(projectServiceClient.listMyMemberships(userId)).thenReturn(memberships(projectId, "MEMBER"));

        assertEquals("MEMBER", guard.requireMember(projectId, userId));
        assertEquals("MEMBER", guard.requireMember(projectId, userId));

        verify(projectServiceClient, times(1)).listMyMemberships(userId);
    }

    @Test
    void requireMember_ShouldRefetchOnMiss_SoNewlyJoinedProjectsWorkImmediately() {
        UUID otherProjectId = UUID.randomUUID();
        when(projectServiceClient.listMyMemberships(userId))
                .thenReturn(memberships(otherProjectId, "OWNER"))
                .thenReturn(memberships(projectId, "OWNER"));

        guard.requireMember(otherProjectId, userId);

        assertEquals("OWNER", guard.requireMember(projectId, userId));
        verify(projectServiceClient, times(2)).listMyMemberships(userId);
    }

    @Test
    void requireMember_ShouldThrowAccessDenied_WhenUserIsNotMember() {
        when(projectServiceClient.listMyMemberships(userId)).thenReturn(JSendResponse.success(
                Map.of("memberships", List.of())));

        assertThrows(AccessDeniedException.class, () -> guard.requireMember(projectId, userId));
    }

    @Test
    void isManager_ShouldBeTrueOnlyForOwnerOrAdmin() {
        UUID adminProject = UUID.randomUUID();
        UUID memberProject = UUID.randomUUID();
        when(projectServiceClient.listMyMemberships(userId)).thenReturn(JSendResponse.success(Map.of(
                "memberships", List.of(
                        new ProjectMembership(adminProject, "ADMIN"),
                        new ProjectMembership(memberProject, "MEMBER")))));

        assertTrue(guard.isManager(adminProject, userId));
        assertFalse(guard.isManager(memberProject, userId));
        assertThrows(AccessDeniedException.class, () -> guard.requireManager(memberProject, userId));
    }

    @Test
    void requireTaskMember_ShouldReturnTasksProject_WhenUserIsMember() {
        UUID taskId = UUID.randomUUID();
        when(taskRepository.findProjectIdById(taskId)).thenReturn(Optional.of(projectId));
        when(projectServiceClient.listMyMemberships(userId)).thenReturn(memberships(projectId, "MEMBER"));

        assertEquals(projectId, guard.requireTaskMember(taskId, userId));
    }

    @Test
    void requireTaskMember_ShouldThrowNotFound_WhenTaskDoesNotExist() {
        UUID taskId = UUID.randomUUID();
        when(taskRepository.findProjectIdById(taskId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> guard.requireTaskMember(taskId, userId));
        verifyNoInteractions(projectServiceClient);
    }

    private static JSendResponse<Map<String, List<ProjectMembership>>> memberships(UUID projectId, String role) {
        return JSendResponse.success(Map.of("memberships", List.of(new ProjectMembership(projectId, role))));
    }
}
