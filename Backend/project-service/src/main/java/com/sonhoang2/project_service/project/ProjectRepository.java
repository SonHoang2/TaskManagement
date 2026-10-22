package com.sonhoang2.project_service.project;

import com.sonhoang2.project_service.project.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID> {

    @Query("SELECT p FROM Project p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%'))")
    org.springframework.data.domain.Page<Project> searchByNameOrDescription(@Param("search") String search,
                                                                            org.springframework.data.domain.Pageable pageable);

    @Query("SELECT p FROM Project p WHERE p.id IN (SELECT m.project.id FROM ProjectMember m WHERE m.userId = :userId)")
    org.springframework.data.domain.Page<Project> findByMemberUserId(@Param("userId") UUID userId,
                                                                     org.springframework.data.domain.Pageable pageable);

    @Query("SELECT p FROM Project p WHERE p.id IN (SELECT m.project.id FROM ProjectMember m WHERE m.userId = :userId) " +
           "AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%')))")
    org.springframework.data.domain.Page<Project> searchByMemberUserId(@Param("userId") UUID userId,
                                                                       @Param("search") String search,
                                                                       org.springframework.data.domain.Pageable pageable);
}

