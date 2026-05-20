package com.sonhoang2.TaskManagementAPI.tasklabel;

import com.sonhoang2.TaskManagementAPI.common.dto.PageResponse;
import com.sonhoang2.TaskManagementAPI.tasklabel.dto.TaskLabelCreateRequest;
import com.sonhoang2.TaskManagementAPI.tasklabel.dto.TaskLabelResponse;
import com.sonhoang2.TaskManagementAPI.tasklabel.dto.TaskLabelUpdateRequest;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface TaskLabelService {

    TaskLabelResponse create(TaskLabelCreateRequest request);

    PageResponse<TaskLabelResponse> findAll(UUID taskId, UUID labelId, Pageable pageable);

    TaskLabelResponse findById(UUID taskId, UUID labelId);

    TaskLabelResponse update(UUID taskId, UUID labelId, TaskLabelUpdateRequest request);

    void delete(UUID taskId, UUID labelId);
}



