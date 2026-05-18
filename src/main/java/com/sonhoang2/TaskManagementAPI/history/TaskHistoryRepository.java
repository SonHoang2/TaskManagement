package com.sonhoang2.TaskManagementAPI.history;

import com.sonhoang2.TaskManagementAPI.history.entity.TaskHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TaskHistoryRepository extends JpaRepository<TaskHistory, UUID> {
    Page<TaskHistory> findByTaskId(UUID taskId, Pageable pageable);

    Page<TaskHistory> findByChangedBy(UUID changedBy, Pageable pageable);

    Page<TaskHistory> findByTaskIdAndChangedBy(UUID taskId, UUID changedBy, Pageable pageable);
}



