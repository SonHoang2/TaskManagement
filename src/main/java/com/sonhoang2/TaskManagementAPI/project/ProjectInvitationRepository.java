package com.sonhoang2.TaskManagementAPI.project;

import com.sonhoang2.TaskManagementAPI.project.entity.ProjectInvitation;
import com.sonhoang2.TaskManagementAPI.project.entity.ProjectInvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProjectInvitationRepository extends JpaRepository<ProjectInvitation, UUID> {

    boolean existsByProjectIdAndInviteeIdAndStatus(UUID projectId, UUID inviteeId, ProjectInvitationStatus status);
}

