package com.sonhoang2.task_service.task;

import com.sonhoang2.task_service.task.entity.Task;
import com.sonhoang2.task_service.task.entity.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID> {
    Page<Task> findByStatus(TaskStatus status, Pageable pageable);

    Page<Task> findByTitleContainingIgnoreCase(String keyword, Pageable pageable);

    Page<Task> findByStatusAndTitleContainingIgnoreCase(
            TaskStatus status,
            String keyword,
            Pageable pageable
    );

    long countByStatus(TaskStatus status);

    @Query("SELECT t.status, COUNT(t) FROM Task t GROUP BY t.status")
    List<Object[]> countByStatusGrouped();

    @Query("SELECT DISTINCT t FROM Task t " +
           "LEFT JOIN FETCH t.comments " +
           "WHERE t.projectId = :projectId")
    List<Task> findByProjectIdWithComments(UUID projectId);

    @Query("SELECT DISTINCT t FROM Task t " +
           "LEFT JOIN FETCH t.attachments " +
           "WHERE t.projectId = :projectId")
    List<Task> findByProjectIdWithAttachments(UUID projectId);

    @Query("SELECT DISTINCT t FROM Task t " +
           "LEFT JOIN FETCH t.taskLabels " +
           "WHERE t.projectId = :projectId")
    List<Task> findByProjectIdWithLabels(UUID projectId);

    Page<Task> findByProjectId(UUID projectId, Pageable pageable);
}