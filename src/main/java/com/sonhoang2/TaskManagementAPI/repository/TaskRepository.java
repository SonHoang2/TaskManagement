package com.sonhoang2.TaskManagementAPI.repository;

import com.sonhoang2.TaskManagementAPI.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, Long> {
}

