package com.sonhoang2.TaskManagementAPI.task;

import com.sonhoang2.TaskManagementAPI.common.dto.PageResponse;
import com.sonhoang2.TaskManagementAPI.task.dto.TaskCreateRequest;
import com.sonhoang2.TaskManagementAPI.task.dto.TaskResponse;
import com.sonhoang2.TaskManagementAPI.task.dto.TaskUpdateRequest;
import org.springframework.data.domain.Pageable;

public interface TaskService {

    TaskResponse create(TaskCreateRequest request);

    PageResponse<TaskResponse> findAll(String status, String keyword, Pageable pageable);

    TaskResponse findById(Long id);

    TaskResponse update(Long id, TaskUpdateRequest request);

    void delete(Long id);
}

