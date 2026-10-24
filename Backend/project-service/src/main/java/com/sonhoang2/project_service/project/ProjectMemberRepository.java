package com.sonhoang2.project_service.project;

import com.sonhoang2.project_service.project.entity.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, UUID> {

    boolean existsByProjectIdAndUserId(UUID projectId, UUID userId);

    Optional<ProjectMember> findByProjectIdAndUserId(UUID projectId, UUID userId);

    List<ProjectMember> findByProjectIdOrderByJoinedAtAsc(UUID projectId);

    List<ProjectMember> findByUserId(UUID userId);

    // Bulk delete; clears the persistence context so no managed entity is left pointing at the deleted project
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM ProjectMember e WHERE e.project.id = :projectId")
    void deleteAllByProjectIdInBulk(@Param("projectId") UUID projectId);
}
