package com.sonhoang2.TaskManagementAPI.service;

import com.sonhoang2.TaskManagementAPI.dto.TaskCreateRequest;
import com.sonhoang2.TaskManagementAPI.dto.TaskResponse;
import com.sonhoang2.TaskManagementAPI.dto.TaskUpdateRequest;
import java.util.List;

public interface TaskService {

    TaskResponse create(TaskCreateRequest request);

    List<TaskResponse> findAll();

    TaskResponse findById(Long id);

    TaskResponse update(Long id, TaskUpdateRequest request);

    void delete(Long id);
}

