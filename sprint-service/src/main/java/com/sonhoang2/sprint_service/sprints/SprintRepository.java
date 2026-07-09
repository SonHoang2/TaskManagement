package com.sonhoang2.sprint_service.sprints;

import com.sonhoang2.sprint_service.sprints.entity.Sprint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SprintRepository extends JpaRepository<Sprint, UUID> {
    Page<Sprint> findByProjectId(UUID projectId, Pageable pageable);
}
