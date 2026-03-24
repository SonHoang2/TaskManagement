package com.sonhoang2.TaskManagementAPI.task;

import com.sonhoang2.TaskManagementAPI.common.dto.PageResponse;
import com.sonhoang2.TaskManagementAPI.task.dto.TaskCreateRequest;
import com.sonhoang2.TaskManagementAPI.task.dto.TaskResponse;
import com.sonhoang2.TaskManagementAPI.task.dto.TaskUpdateRequest;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface TaskService {

    TaskResponse create(TaskCreateRequest request);

    PageResponse<TaskResponse> findAll(String status, String keyword, Pageable pageable);

    TaskResponse findById(UUID id);

    TaskResponse update(UUID id, TaskUpdateRequest request);

    void delete(UUID id);
}
