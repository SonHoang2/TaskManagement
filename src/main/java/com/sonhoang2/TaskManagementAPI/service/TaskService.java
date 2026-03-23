package com.sonhoang2.TaskManagementAPI.service;

import com.sonhoang2.TaskManagementAPI.dto.common.PageResponse;
import com.sonhoang2.TaskManagementAPI.dto.task.TaskCreateRequest;
import com.sonhoang2.TaskManagementAPI.dto.task.TaskResponse;
import com.sonhoang2.TaskManagementAPI.dto.task.TaskUpdateRequest;
import org.springframework.data.domain.Pageable;

public interface TaskService {

    TaskResponse create(TaskCreateRequest request);

    PageResponse<TaskResponse> findAll(Pageable pageable);

    TaskResponse findById(Long id);

    TaskResponse update(Long id, TaskUpdateRequest request);

    void delete(Long id);
}

