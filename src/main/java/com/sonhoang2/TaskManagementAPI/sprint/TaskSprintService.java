package com.sonhoang2.TaskManagementAPI.sprint;

import com.sonhoang2.TaskManagementAPI.common.dto.PageResponse;
import com.sonhoang2.TaskManagementAPI.sprint.dto.TaskSprintCreateRequest;
import com.sonhoang2.TaskManagementAPI.sprint.dto.TaskSprintResponse;
import com.sonhoang2.TaskManagementAPI.sprint.dto.TaskSprintUpdateRequest;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface TaskSprintService {

    TaskSprintResponse create(TaskSprintCreateRequest request);

    PageResponse<TaskSprintResponse> findAll(UUID taskId, UUID sprintId, Pageable pageable);

    TaskSprintResponse findById(UUID taskId, UUID sprintId);

    TaskSprintResponse update(UUID taskId, UUID sprintId, TaskSprintUpdateRequest request);

    void delete(UUID taskId, UUID sprintId);
}



