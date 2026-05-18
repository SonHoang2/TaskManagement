package com.sonhoang2.TaskManagementAPI.comment;

import com.sonhoang2.TaskManagementAPI.comment.entity.TaskComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TaskCommentRepository extends JpaRepository<TaskComment, UUID> {
    Page<TaskComment> findByTaskId(UUID taskId, Pageable pageable);

    Page<TaskComment> findByUserId(UUID userId, Pageable pageable);

    Page<TaskComment> findByTaskIdAndUserId(UUID taskId, UUID userId, Pageable pageable);
}



