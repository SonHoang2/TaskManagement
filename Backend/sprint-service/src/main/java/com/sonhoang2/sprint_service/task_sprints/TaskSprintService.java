package com.sonhoang2.sprint_service.task_sprints;

import com.sonhoang2.sprint_service.common.dto.PageResponse;
import com.sonhoang2.sprint_service.task_sprints.dto.TaskSprintCreateRequest;
import com.sonhoang2.sprint_service.task_sprints.dto.TaskSprintResponse;
import com.sonhoang2.sprint_service.task_sprints.dto.TaskSprintUpdateRequest;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface TaskSprintService {

    TaskSprintResponse create(TaskSprintCreateRequest request);

    PageResponse<TaskSprintResponse> findAll(UUID taskId, UUID sprintId, Pageable pageable);

    TaskSprintResponse findById(UUID taskId, UUID sprintId);

    TaskSprintResponse update(UUID taskId, UUID sprintId, TaskSprintUpdateRequest request);

    void delete(UUID taskId, UUID sprintId);
}



