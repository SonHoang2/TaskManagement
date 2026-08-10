package com.sonhoang2.task_service.history;

import com.sonhoang2.task_service.common.dto.PageResponse;
import com.sonhoang2.task_service.history.dto.TaskHistoryCreateRequest;
import com.sonhoang2.task_service.history.dto.TaskHistoryResponse;
import com.sonhoang2.task_service.history.dto.TaskHistoryUpdateRequest;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface TaskHistoryService {

    TaskHistoryResponse create(TaskHistoryCreateRequest request);

    PageResponse<TaskHistoryResponse> findAll(UUID taskId, UUID changedBy, Pageable pageable);

    TaskHistoryResponse findById(UUID id);

    TaskHistoryResponse update(UUID id, TaskHistoryUpdateRequest request);

    void delete(UUID id);
}



