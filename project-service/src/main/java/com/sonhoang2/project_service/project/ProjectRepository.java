package com.sonhoang2.project_service.project;

import com.sonhoang2.project_service.project.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID> {
}

