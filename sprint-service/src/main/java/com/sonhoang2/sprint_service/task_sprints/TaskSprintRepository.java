package com.sonhoang2.sprint_service.task_sprints;

import com.sonhoang2.sprint_service.task_sprints.entity.TaskSprint;
import com.sonhoang2.sprint_service.task_sprints.entity.TaskSprintId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TaskSprintRepository extends JpaRepository<TaskSprint, TaskSprintId> {
    Page<TaskSprint> findByIdTaskId(UUID taskId, Pageable pageable);

    Page<TaskSprint> findByIdSprintId(UUID sprintId, Pageable pageable);

    Page<TaskSprint> findByIdTaskIdAndIdSprintId(UUID taskId, UUID sprintId, Pageable pageable);
}



