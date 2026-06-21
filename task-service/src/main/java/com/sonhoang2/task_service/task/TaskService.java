package com.sonhoang2.task_service.task;

import com.sonhoang2.task_service.common.dto.PageResponse;
import com.sonhoang2.task_service.task.dto.TaskCreateRequest;
import com.sonhoang2.task_service.task.dto.TaskResponse;
import com.sonhoang2.task_service.task.dto.TaskUpdateRequest;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface TaskService {

    TaskResponse create(TaskCreateRequest request);

    PageResponse<TaskResponse> findAll(String status, String keyword, Pageable pageable);

    TaskResponse findById(UUID id);

    TaskResponse update(UUID id, TaskUpdateRequest request);

    void delete(UUID id);
}
