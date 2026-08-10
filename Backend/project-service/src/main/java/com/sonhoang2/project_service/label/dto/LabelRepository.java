package com.sonhoang2.project_service.label.dto;

import com.sonhoang2.project_service.label.entity.Label;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LabelRepository extends JpaRepository<Label, UUID> {

    Page<Label> findByProjectId(UUID projectId, Pageable pageable);

    Page<Label> findByNameContainingIgnoreCase(String name, Pageable pageable);

    Page<Label> findByProjectIdAndNameContainingIgnoreCase(UUID projectId, String name, Pageable pageable);
}
