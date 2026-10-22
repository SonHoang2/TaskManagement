package com.sonhoang2.task_service.comment;

import com.sonhoang2.task_service.common.dto.PageResponse;
import com.sonhoang2.task_service.comment.dto.TaskCommentCreateRequest;
import com.sonhoang2.task_service.comment.dto.TaskCommentResponse;
import com.sonhoang2.task_service.comment.dto.TaskCommentUpdateRequest;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface TaskCommentService {

    TaskCommentResponse create(TaskCommentCreateRequest request, UUID currentUserId);

    PageResponse<TaskCommentResponse> findAll(UUID taskId, UUID userId, Pageable pageable, UUID currentUserId);

    TaskCommentResponse findById(UUID id, UUID currentUserId);

    TaskCommentResponse update(UUID id, TaskCommentUpdateRequest request, UUID currentUserId);

    void delete(UUID id, UUID currentUserId);
}



