package com.sonhoang2.TaskManagementAPI.tasklabel;

import com.sonhoang2.TaskManagementAPI.tasklabel.entity.TaskLabel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TaskLabelRepository extends JpaRepository<TaskLabel, UUID> {
    Page<TaskLabel> findByTaskId(UUID taskId, Pageable pageable);

    Page<TaskLabel> findByLabelId(UUID labelId, Pageable pageable);

    Page<TaskLabel> findByTaskIdAndLabelId(UUID taskId, UUID labelId, Pageable pageable);

    Optional<TaskLabel> findByTaskIdAndLabelId(UUID taskId, UUID labelId);
}
