package com.sonhoang2.sprint_service.common.security;

import com.sonhoang2.sprint_service.common.dto.ProjectMembership;
import com.sonhoang2.sprint_service.feign.ProjectServiceClient;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Project-level authorization for sprint-service; membership is owned by project-service. */
@Component
@RequiredArgsConstructor
public class ProjectAccessGuard {

    private final ProjectServiceClient projectServiceClient;

    public void requireMember(UUID projectId, UUID userId) {
        if (!memberProjectIds(userId).contains(projectId)) {
            throw new AccessDeniedException("You are not a member of this project");
        }
    }

    public Set<UUID> memberProjectIds(UUID userId) {
        List<ProjectMembership> memberships =
                projectServiceClient.listMyMemberships(userId).data().get("memberships");
        return memberships.stream().map(ProjectMembership::getProjectId).collect(Collectors.toUnmodifiableSet());
    }
}
