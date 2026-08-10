package com.sonhoang2.project_service.project;

import com.sonhoang2.project_service.project.entity.ProjectInvitation;
import com.sonhoang2.project_service.project.entity.ProjectInvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProjectInvitationRepository extends JpaRepository<ProjectInvitation, UUID> {

    boolean existsByProjectIdAndInviteeIdAndStatus(UUID projectId, UUID inviteeId, ProjectInvitationStatus status);

    List<ProjectInvitation> findByProjectId(UUID projectId);

    List<ProjectInvitation> findByInviteeId(UUID inviteeId);
}

