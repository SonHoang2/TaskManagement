package com.sonhoang2.task_service.comment;

import com.sonhoang2.task_service.comment.dto.TaskCommentCreateRequest;
import com.sonhoang2.task_service.comment.dto.TaskCommentResponse;
import com.sonhoang2.task_service.comment.dto.TaskCommentUpdateRequest;
import com.sonhoang2.task_service.comment.entity.TaskComment;
import com.sonhoang2.task_service.common.dto.PageResponse;
import com.sonhoang2.task_service.common.exception.ResourceNotFoundException;
import com.sonhoang2.task_service.common.security.ProjectAccessGuard;
import com.sonhoang2.task_service.events.EventPublisher;
import com.sonhoang2.task_service.events.TaskCommentCreatedEvent;
import com.sonhoang2.task_service.task.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class TaskCommentServiceImpl implements TaskCommentService {

    private final TaskCommentRepository taskCommentRepository;
    private final ModelMapper modelMapper;
    private final EventPublisher eventPublisher;
    private final TaskRepository taskRepository;
    private final ProjectAccessGuard projectAccessGuard;

    private PageResponse<TaskCommentResponse> toPageResponse(Page<TaskComment> page) {
        return new PageResponse<>(page.getContent().stream().map(this::toResponse).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext(),
                page.hasPrevious(),
                page.getNumberOfElements());
    }

    @Override
    public TaskCommentResponse create(TaskCommentCreateRequest request, UUID currentUserId) {
        projectAccessGuard.requireTaskMember(request.getTaskId(), currentUserId);

        TaskComment comment = TaskComment.builder()
                .taskId(request.getTaskId())
                .userId(currentUserId)
                .content(request.getContent())
                .createdAt(Instant.now())
                .build();

        TaskComment savedComment = taskCommentRepository.save(comment);
        
        // Publish TaskCommentCreatedEvent
        var task = taskRepository.findById(request.getTaskId());
        if (task.isPresent()) {
            TaskCommentCreatedEvent event = TaskCommentCreatedEvent.builder()
                    .commentId(savedComment.getId())
                    .taskId(savedComment.getTaskId())
                    .projectId(task.get().getProjectId())
                    .taskTitle(task.get().getTitle())
                    .userId(savedComment.getUserId())
                    .assigneeId(task.get().getAssigneeId())
                    .reporterId(task.get().getReporterId())
                    .content(savedComment.getContent())
                    .eventType("TASK_COMMENT_CREATED")
                    .build();
            eventPublisher.publishTaskCommentCreatedEvent(event);
        }

        return toResponse(savedComment);
    }

    @Override
    public PageResponse<TaskCommentResponse> findAll(UUID taskId, UUID userId, Pageable pageable, UUID currentUserId) {
        projectAccessGuard.requireTaskMember(taskId, currentUserId);

        Page<TaskComment> page = userId != null
                ? taskCommentRepository.findByTaskIdAndUserId(taskId, userId, pageable)
                : taskCommentRepository.findByTaskId(taskId, pageable);

        return toPageResponse(page);
    }

    @Override
    @Transactional(readOnly = true)
    public TaskCommentResponse findById(UUID id, UUID currentUserId) {
        TaskComment comment = findCommentByIdOrThrow(id);
        projectAccessGuard.requireTaskMember(comment.getTaskId(), currentUserId);
        return toResponse(comment);
    }

    @Override
    public TaskCommentResponse update(UUID id, TaskCommentUpdateRequest request, UUID currentUserId) {
        TaskComment comment = findCommentByIdOrThrow(id);
        projectAccessGuard.requireTaskMember(comment.getTaskId(), currentUserId);
        if (!comment.getUserId().equals(currentUserId)) {
            throw new AccessDeniedException("You can only edit your own comments");
        }
        modelMapper.map(request, comment);
        return toResponse(comment);
    }

    @Override
    public void delete(UUID id, UUID currentUserId) {
        TaskComment comment = findCommentByIdOrThrow(id);
        UUID projectId = projectAccessGuard.requireTaskMember(comment.getTaskId(), currentUserId);
        if (!comment.getUserId().equals(currentUserId) && !projectAccessGuard.isManager(projectId, currentUserId)) {
            throw new AccessDeniedException("Only the author or a project owner/admin can delete this comment");
        }
        taskCommentRepository.delete(comment);
    }

    private TaskComment findCommentByIdOrThrow(UUID id) {
        return taskCommentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TaskComment with id " + id + " not found"));
    }

    private TaskCommentResponse toResponse(TaskComment comment) {
        return TaskCommentResponse.builder()
                .id(comment.getId())
                .taskId(comment.getTaskId())
                .userId(comment.getUserId())
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .build();
    }
}
