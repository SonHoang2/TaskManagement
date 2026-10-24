package com.sonhoang2.task_service.common.security;

import com.sonhoang2.common.membership.MembershipCache;
import com.sonhoang2.task_service.common.dto.ProjectMembership;
import com.sonhoang2.task_service.common.exception.ResourceNotFoundException;
import com.sonhoang2.task_service.feign.ProjectServiceClient;
import com.sonhoang2.task_service.task.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Project-level authorization for task-service. Membership lives in project-service, so it is
 * fetched per user and cached in Redis (shared by all replicas; project-service evicts it on change).
 */
@Component
@RequiredArgsConstructor
public class ProjectAccessGuard {

    private static final Set<String> MANAGER_ROLES = Set.of("OWNER", "ADMIN");

    private final ProjectServiceClient projectServiceClient;
    private final TaskRepository taskRepository;
    private final MembershipCache membershipCache;

    /** Returns the caller's role in the project, or throws if they are not a member. */
    public String requireMember(UUID projectId, UUID userId) {
        String role = roleIn(projectId, userId);
        if (role == null) {
            throw new AccessDeniedException("You are not a member of this project");
        }
        return role;
    }

    /** Checks membership in the task's project and returns that project's id. */
    public UUID requireTaskMember(UUID taskId, UUID userId) {
        UUID projectId = taskRepository.findProjectIdById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task with id " + taskId + " not found"));
        requireMember(projectId, userId);
        return projectId;
    }

    public boolean isManager(UUID projectId, UUID userId) {
        String role = roleIn(projectId, userId);
        return role != null && MANAGER_ROLES.contains(role);
    }

    public void requireManager(UUID projectId, UUID userId) {
        requireMember(projectId, userId);
        if (!isManager(projectId, userId)) {
            throw new AccessDeniedException("Only the project owner or an admin can do this");
        }
    }

    public Set<UUID> memberProjectIds(UUID userId) {
        return memberships(userId, false).keySet();
    }

    private String roleIn(UUID projectId, UUID userId) {
        String role = memberships(userId, false).get(projectId);
        // A miss may only mean the user joined or created the project after we cached; refetch once.
        return role != null ? role : memberships(userId, true).get(projectId);
    }

    // userId -> (projectId -> role)
    private Map<UUID, String> memberships(UUID userId, boolean refresh) {
        if (!refresh) {
            Optional<Map<UUID, String>> cached = membershipCache.get(userId);
            if (cached.isPresent()) {
                return cached.get();
            }
        }
        Map<UUID, String> fresh = fetchMemberships(userId);
        membershipCache.put(userId, fresh);
        return fresh;
    }

    private Map<UUID, String> fetchMemberships(UUID userId) {
        List<ProjectMembership> list = projectServiceClient.listMyMemberships(userId).data().get("memberships");
        return list.stream()
                .collect(Collectors.toUnmodifiableMap(ProjectMembership::getProjectId, ProjectMembership::getRole));
    }
}
