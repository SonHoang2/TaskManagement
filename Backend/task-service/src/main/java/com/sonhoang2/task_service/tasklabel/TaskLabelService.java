package com.sonhoang2.task_service.tasklabel;

import com.sonhoang2.task_service.common.dto.PageResponse;
import com.sonhoang2.task_service.tasklabel.dto.TaskLabelCreateRequest;
import com.sonhoang2.task_service.tasklabel.dto.TaskLabelResponse;
import com.sonhoang2.task_service.tasklabel.dto.TaskLabelUpdateRequest;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface TaskLabelService {

    TaskLabelResponse create(TaskLabelCreateRequest request, UUID currentUserId);

    PageResponse<TaskLabelResponse> findAll(UUID taskId, UUID labelId, Pageable pageable, UUID currentUserId);

    TaskLabelResponse findById(UUID taskId, UUID labelId, UUID currentUserId);

    TaskLabelResponse update(UUID taskId, UUID labelId, TaskLabelUpdateRequest request, UUID currentUserId);

    void delete(UUID taskId, UUID labelId, UUID currentUserId);
}



