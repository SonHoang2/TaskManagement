package com.sonhoang2.TaskManagementAPI.comment;

import com.sonhoang2.TaskManagementAPI.common.dto.PageResponse;
import com.sonhoang2.TaskManagementAPI.comment.dto.TaskCommentCreateRequest;
import com.sonhoang2.TaskManagementAPI.comment.dto.TaskCommentResponse;
import com.sonhoang2.TaskManagementAPI.comment.dto.TaskCommentUpdateRequest;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface TaskCommentService {

    TaskCommentResponse create(TaskCommentCreateRequest request);

    PageResponse<TaskCommentResponse> findAll(UUID taskId, UUID userId, Pageable pageable);

    TaskCommentResponse findById(UUID id);

    TaskCommentResponse update(UUID id, TaskCommentUpdateRequest request);

    void delete(UUID id);
}



