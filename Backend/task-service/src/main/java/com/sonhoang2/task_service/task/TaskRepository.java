package com.sonhoang2.task_service.task;

import com.sonhoang2.task_service.task.entity.Task;
import com.sonhoang2.task_service.task.entity.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID> {
    Page<Task> findByProjectIdIn(Collection<UUID> projectIds, Pageable pageable);

    Page<Task> findByProjectIdInAndStatus(Collection<UUID> projectIds, TaskStatus status, Pageable pageable);

    Page<Task> findByProjectIdInAndTitleContainingIgnoreCase(Collection<UUID> projectIds,
                                                             String keyword,
                                                             Pageable pageable);

    Page<Task> findByProjectIdInAndStatusAndTitleContainingIgnoreCase(
            Collection<UUID> projectIds,
            TaskStatus status,
            String keyword,
            Pageable pageable
    );

    @Query("SELECT t.projectId FROM Task t WHERE t.id = :id")
    Optional<UUID> findProjectIdById(UUID id);

    @Query("SELECT t.status, COUNT(t) FROM Task t WHERE t.projectId IN :projectIds GROUP BY t.status")
    List<Object[]> countByStatusGroupedByProjectIdIn(Collection<UUID> projectIds);

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

    @Query("SELECT t.status, COUNT(t) FROM Task t WHERE t.projectId = :projectId GROUP BY t.status")
    List<Object[]> countByStatusGroupedByProjectId(UUID projectId);
}