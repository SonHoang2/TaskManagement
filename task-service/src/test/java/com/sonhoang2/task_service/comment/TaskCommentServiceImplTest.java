package com.sonhoang2.task_service.comment;

import com.sonhoang2.task_service.common.dto.PageResponse;
import com.sonhoang2.task_service.common.exception.ResourceNotFoundException;
import com.sonhoang2.task_service.comment.dto.TaskCommentCreateRequest;
import com.sonhoang2.task_service.comment.dto.TaskCommentResponse;
import com.sonhoang2.task_service.comment.dto.TaskCommentUpdateRequest;
import com.sonhoang2.task_service.comment.entity.TaskComment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskCommentServiceImplTest {

    @Mock
    private TaskCommentRepository taskCommentRepository;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private TaskCommentServiceImpl taskCommentService;

    private TaskComment comment;
    private TaskCommentCreateRequest createRequest;
    private TaskCommentUpdateRequest updateRequest;
    private UUID commentId;
    private UUID taskId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        commentId = UUID.randomUUID();
        taskId = UUID.randomUUID();
        userId = UUID.randomUUID();

        comment = TaskComment.builder()
                .id(commentId)
                .taskId(taskId)
                .userId(userId)
                .content("Test comment content")
                .createdAt(Instant.now())
                .build();

        createRequest = new TaskCommentCreateRequest();
        createRequest.setTaskId(taskId);
        createRequest.setUserId(userId);
        createRequest.setContent("Test comment content");

        updateRequest = new TaskCommentUpdateRequest();
        updateRequest.setContent("Updated comment content");
    }

    @Test
    void create_ShouldReturnCommentResponse() {
        when(taskCommentRepository.save(any(TaskComment.class))).thenReturn(comment);

        TaskCommentResponse result = taskCommentService.create(createRequest);

        assertNotNull(result);
        assertEquals(commentId, result.getId());
        assertEquals(taskId, result.getTaskId());
        assertEquals(userId, result.getUserId());
        assertEquals("Test comment content", result.getContent());
        verify(taskCommentRepository).save(any(TaskComment.class));
    }

    @Test
    void findAll_WithoutFilters_ShouldReturnAllComments() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<TaskComment> commentPage = new PageImpl<>(java.util.List.of(comment));
        when(taskCommentRepository.findAll(pageable)).thenReturn(commentPage);

        PageResponse<TaskCommentResponse> result = taskCommentService.findAll(null, null, pageable);

        assertNotNull(result);
        assertEquals(1, result.content().size());
        verify(taskCommentRepository).findAll(pageable);
    }

    @Test
    void findAll_WithTaskIdFilter_ShouldReturnFilteredComments() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<TaskComment> commentPage = new PageImpl<>(java.util.List.of(comment));
        when(taskCommentRepository.findByTaskId(taskId, pageable)).thenReturn(commentPage);

        PageResponse<TaskCommentResponse> result = taskCommentService.findAll(taskId, null, pageable);

        assertNotNull(result);
        assertEquals(1, result.content().size());
        verify(taskCommentRepository).findByTaskId(taskId, pageable);
    }

    @Test
    void findAll_WithUserIdFilter_ShouldReturnFilteredComments() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<TaskComment> commentPage = new PageImpl<>(java.util.List.of(comment));
        when(taskCommentRepository.findByUserId(userId, pageable)).thenReturn(commentPage);

        PageResponse<TaskCommentResponse> result = taskCommentService.findAll(null, userId, pageable);

        assertNotNull(result);
        assertEquals(1, result.content().size());
        verify(taskCommentRepository).findByUserId(userId, pageable);
    }

    @Test
    void findAll_WithTaskIdAndUserIdFilters_ShouldReturnFilteredComments() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<TaskComment> commentPage = new PageImpl<>(java.util.List.of(comment));
        when(taskCommentRepository.findByTaskIdAndUserId(taskId, userId, pageable)).thenReturn(commentPage);

        PageResponse<TaskCommentResponse> result = taskCommentService.findAll(taskId, userId, pageable);

        assertNotNull(result);
        assertEquals(1, result.content().size());
        verify(taskCommentRepository).findByTaskIdAndUserId(taskId, userId, pageable);
    }

    @Test
    void findById_ShouldReturnCommentResponse_WhenCommentExists() {
        when(taskCommentRepository.findById(commentId)).thenReturn(Optional.of(comment));

        TaskCommentResponse result = taskCommentService.findById(commentId);

        assertNotNull(result);
        assertEquals(commentId, result.getId());
        verify(taskCommentRepository).findById(commentId);
    }

    @Test
    void findById_ShouldThrowResourceNotFoundException_WhenCommentNotFound() {
        when(taskCommentRepository.findById(commentId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> taskCommentService.findById(commentId));

        verify(taskCommentRepository).findById(commentId);
    }

    @Test
    void update_ShouldReturnUpdatedCommentResponse_WhenCommentExists() {
        when(taskCommentRepository.findById(commentId)).thenReturn(Optional.of(comment));
        doNothing().when(modelMapper).map(updateRequest, comment);

        TaskCommentResponse result = taskCommentService.update(commentId, updateRequest);

        assertNotNull(result);
        verify(taskCommentRepository).findById(commentId);
        verify(modelMapper).map(updateRequest, comment);
    }

    @Test
    void update_ShouldThrowResourceNotFoundException_WhenCommentNotFound() {
        when(taskCommentRepository.findById(commentId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> taskCommentService.update(commentId, updateRequest));

        verify(taskCommentRepository).findById(commentId);
        verify(modelMapper, never()).map(any(), any());
    }

    @Test
    void delete_ShouldDeleteComment_WhenCommentExists() {
        when(taskCommentRepository.findById(commentId)).thenReturn(Optional.of(comment));
        doNothing().when(taskCommentRepository).delete(comment);

        taskCommentService.delete(commentId);

        verify(taskCommentRepository).findById(commentId);
        verify(taskCommentRepository).delete(comment);
    }

    @Test
    void delete_ShouldThrowResourceNotFoundException_WhenCommentNotFound() {
        when(taskCommentRepository.findById(commentId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> taskCommentService.delete(commentId));

        verify(taskCommentRepository).findById(commentId);
        verify(taskCommentRepository, never()).delete(any(TaskComment.class));
    }
}
