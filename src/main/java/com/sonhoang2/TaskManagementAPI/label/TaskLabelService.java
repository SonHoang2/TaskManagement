package com.sonhoang2.TaskManagementAPI.label;

import com.sonhoang2.TaskManagementAPI.common.dto.PageResponse;
import com.sonhoang2.TaskManagementAPI.label.dto.TaskLabelCreateRequest;
import com.sonhoang2.TaskManagementAPI.label.dto.TaskLabelResponse;
import com.sonhoang2.TaskManagementAPI.label.dto.TaskLabelUpdateRequest;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface TaskLabelService {

    TaskLabelResponse create(TaskLabelCreateRequest request);

    PageResponse<TaskLabelResponse> findAll(UUID taskId, UUID labelId, Pageable pageable);

    TaskLabelResponse findById(UUID taskId, UUID labelId);

    TaskLabelResponse update(UUID taskId, UUID labelId, TaskLabelUpdateRequest request);

    void delete(UUID taskId, UUID labelId);
}



