package com.sonhoang2.project_service.label.dto;

import com.sonhoang2.project_service.label.entity.Label;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface LabelRepository extends JpaRepository<Label, UUID> {

    Page<Label> findByProjectId(UUID projectId, Pageable pageable);

    Page<Label> findByProjectIdAndNameContainingIgnoreCase(UUID projectId, String name, Pageable pageable);

    // Bulk delete; clears the persistence context so no managed entity is left pointing at the deleted project
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM Label e WHERE e.projectId = :projectId")
    void deleteAllByProjectIdInBulk(@Param("projectId") UUID projectId);
}
