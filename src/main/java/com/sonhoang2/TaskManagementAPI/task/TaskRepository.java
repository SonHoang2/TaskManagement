package com.sonhoang2.TaskManagementAPI.task;

import com.sonhoang2.TaskManagementAPI.task.entity.Task;
import com.sonhoang2.TaskManagementAPI.task.entity.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID> {
    Page<Task> findByStatus(TaskStatus status, Pageable pageable);

    Page<Task> findByTitleContainingIgnoreCase(String keyword, Pageable pageable);

    Page<Task> findByStatusAndTitleContainingIgnoreCase(
            TaskStatus status,
            String keyword,
            Pageable pageable
    );
}
