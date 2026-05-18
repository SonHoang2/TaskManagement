package com.sonhoang2.TaskManagementAPI.label;

import com.sonhoang2.TaskManagementAPI.label.entity.TaskLabel;
import com.sonhoang2.TaskManagementAPI.label.entity.TaskLabelId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TaskLabelRepository extends JpaRepository<TaskLabel, TaskLabelId> {
    Page<TaskLabel> findByIdTaskId(UUID taskId, Pageable pageable);

    Page<TaskLabel> findByIdLabelId(UUID labelId, Pageable pageable);

    Page<TaskLabel> findByIdTaskIdAndIdLabelId(UUID taskId, UUID labelId, Pageable pageable);
}



