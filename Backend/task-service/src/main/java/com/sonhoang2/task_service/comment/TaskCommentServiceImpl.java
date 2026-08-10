package com.sonhoang2.task_service.comment;

import com.sonhoang2.task_service.common.dto.PageResponse;
import com.sonhoang2.task_service.common.exception.ResourceNotFoundException;
import com.sonhoang2.task_service.events.EventPublisher;
import com.sonhoang2.task_service.events.TaskCommentCreatedEvent;
import com.sonhoang2.task_service.comment.dto.TaskCommentCreateRequest;
import com.sonhoang2.task_service.comment.dto.TaskCommentResponse;
import com.sonhoang2.task_service.comment.dto.TaskCommentUpdateRequest;
import com.sonhoang2.task_service.comment.entity.TaskComment;
import com.sonhoang2.task_service.task.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.modelmapper.ModelMapper;

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
    public TaskCommentResponse create(TaskCommentCreateRequest request) {
        TaskComment comment = TaskComment.builder()
                .taskId(request.getTaskId())
                .userId(request.getUserId())
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
                    .content(savedComment.getContent())
                    .eventType("TASK_COMMENT_CREATED")
                    .build();
            eventPublisher.publishTaskCommentCreatedEvent(event);
        }

        return toResponse(savedComment);
    }

    @Override
    public PageResponse<TaskCommentResponse> findAll(UUID taskId, UUID userId, Pageable pageable) {
        Page<TaskComment> page;

        if (taskId != null && userId != null) {
            page = taskCommentRepository.findByTaskIdAndUserId(taskId, userId, pageable);
        } else if (taskId != null) {
            page = taskCommentRepository.findByTaskId(taskId, pageable);
        } else if (userId != null) {
            page = taskCommentRepository.findByUserId(userId, pageable);
        } else {
            page = taskCommentRepository.findAll(pageable);
        }

        return toPageResponse(page);
    }

    @Override
    @Transactional(readOnly = true)
    public TaskCommentResponse findById(UUID id) {
        return toResponse(findCommentByIdOrThrow(id));
    }

    @Override
    public TaskCommentResponse update(UUID id, TaskCommentUpdateRequest request) {
        TaskComment comment = findCommentByIdOrThrow(id);
        modelMapper.map(request, comment);
        return toResponse(comment);
    }

    @Override
    public void delete(UUID id) {
        taskCommentRepository.delete(findCommentByIdOrThrow(id));
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
