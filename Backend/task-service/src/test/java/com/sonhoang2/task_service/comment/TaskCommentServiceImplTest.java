package com.sonhoang2.task_service.comment;

import com.sonhoang2.task_service.common.dto.PageResponse;
import com.sonhoang2.task_service.common.exception.ResourceNotFoundException;
import com.sonhoang2.task_service.common.security.ProjectAccessGuard;
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
import org.springframework.security.access.AccessDeniedException;

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

    @Mock
    private com.sonhoang2.task_service.task.TaskRepository taskRepository;

    @Mock
    private ProjectAccessGuard projectAccessGuard;

    @InjectMocks
    private TaskCommentServiceImpl taskCommentService;

    private TaskComment comment;
    private TaskCommentCreateRequest createRequest;
    private TaskCommentUpdateRequest updateRequest;
    private UUID commentId;
    private UUID taskId;
    private UUID userId;
    private UUID otherUserId;
    private UUID projectId;

    @BeforeEach
    void setUp() {
        commentId = UUID.randomUUID();
        taskId = UUID.randomUUID();
        userId = UUID.randomUUID();
        otherUserId = UUID.randomUUID();
        projectId = UUID.randomUUID();

        comment = TaskComment.builder()
                .id(commentId)
                .taskId(taskId)
                .userId(userId)
                .content("Test comment content")
                .createdAt(Instant.now())
                .build();

        createRequest = new TaskCommentCreateRequest();
        createRequest.setTaskId(taskId);
        createRequest.setContent("Test comment content");

        updateRequest = new TaskCommentUpdateRequest();
        updateRequest.setContent("Updated comment content");
    }

    @Test
    void create_ShouldUseCallerAsAuthor_WhenCallerIsMember() {
        when(taskCommentRepository.save(any(TaskComment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskCommentResponse result = taskCommentService.create(createRequest, userId);

        assertEquals(taskId, result.getTaskId());
        assertEquals(userId, result.getUserId());
        assertEquals("Test comment content", result.getContent());
        verify(projectAccessGuard).requireTaskMember(taskId, userId);
    }

    @Test
    void create_ShouldThrowAccessDenied_WhenCallerIsNotMember() {
        when(projectAccessGuard.requireTaskMember(taskId, userId)).thenThrow(new AccessDeniedException("denied"));

        assertThrows(AccessDeniedException.class, () -> taskCommentService.create(createRequest, userId));

        verify(taskCommentRepository, never()).save(any(TaskComment.class));
    }

    @Test
    void findAll_WithTaskIdFilter_ShouldReturnFilteredComments() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<TaskComment> commentPage = new PageImpl<>(java.util.List.of(comment));
        when(taskCommentRepository.findByTaskId(taskId, pageable)).thenReturn(commentPage);

        PageResponse<TaskCommentResponse> result = taskCommentService.findAll(taskId, null, pageable, userId);

        assertEquals(1, result.content().size());
        verify(projectAccessGuard).requireTaskMember(taskId, userId);
    }

    @Test
    void findAll_WithTaskIdAndUserIdFilters_ShouldReturnFilteredComments() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<TaskComment> commentPage = new PageImpl<>(java.util.List.of(comment));
        when(taskCommentRepository.findByTaskIdAndUserId(taskId, userId, pageable)).thenReturn(commentPage);

        PageResponse<TaskCommentResponse> result = taskCommentService.findAll(taskId, userId, pageable, userId);

        assertEquals(1, result.content().size());
    }

    @Test
    void findAll_ShouldThrowAccessDenied_WhenCallerIsNotMember() {
        Pageable pageable = PageRequest.of(0, 10);
        when(projectAccessGuard.requireTaskMember(taskId, userId)).thenThrow(new AccessDeniedException("denied"));

        assertThrows(AccessDeniedException.class, () -> taskCommentService.findAll(taskId, null, pageable, userId));

        verifyNoInteractions(taskCommentRepository);
    }

    @Test
    void findById_ShouldReturnCommentResponse_WhenCallerIsMember() {
        when(taskCommentRepository.findById(commentId)).thenReturn(Optional.of(comment));

        TaskCommentResponse result = taskCommentService.findById(commentId, userId);

        assertEquals(commentId, result.getId());
        verify(projectAccessGuard).requireTaskMember(taskId, userId);
    }

    @Test
    void findById_ShouldThrowResourceNotFoundException_WhenCommentNotFound() {
        when(taskCommentRepository.findById(commentId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> taskCommentService.findById(commentId, userId));
    }

    @Test
    void update_ShouldReturnUpdatedCommentResponse_WhenCallerIsAuthor() {
        when(taskCommentRepository.findById(commentId)).thenReturn(Optional.of(comment));
        doNothing().when(modelMapper).map(updateRequest, comment);

        TaskCommentResponse result = taskCommentService.update(commentId, updateRequest, userId);

        assertNotNull(result);
        verify(modelMapper).map(updateRequest, comment);
    }

    @Test
    void update_ShouldThrowAccessDenied_WhenCallerIsNotAuthor() {
        when(taskCommentRepository.findById(commentId)).thenReturn(Optional.of(comment));

        assertThrows(AccessDeniedException.class,
                () -> taskCommentService.update(commentId, updateRequest, otherUserId));

        verify(modelMapper, never()).map(any(), any());
    }

    @Test
    void update_ShouldThrowResourceNotFoundException_WhenCommentNotFound() {
        when(taskCommentRepository.findById(commentId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> taskCommentService.update(commentId, updateRequest, userId));

        verify(modelMapper, never()).map(any(), any());
    }

    @Test
    void delete_ShouldDeleteComment_WhenCallerIsAuthor() {
        when(taskCommentRepository.findById(commentId)).thenReturn(Optional.of(comment));

        taskCommentService.delete(commentId, userId);

        verify(taskCommentRepository).delete(comment);
    }

    @Test
    void delete_ShouldDeleteComment_WhenCallerIsProjectManager() {
        when(taskCommentRepository.findById(commentId)).thenReturn(Optional.of(comment));
        when(projectAccessGuard.requireTaskMember(taskId, otherUserId)).thenReturn(projectId);
        when(projectAccessGuard.isManager(projectId, otherUserId)).thenReturn(true);

        taskCommentService.delete(commentId, otherUserId);

        verify(taskCommentRepository).delete(comment);
    }

    @Test
    void delete_ShouldThrowAccessDenied_WhenCallerIsNeitherAuthorNorManager() {
        when(taskCommentRepository.findById(commentId)).thenReturn(Optional.of(comment));
        when(projectAccessGuard.requireTaskMember(taskId, otherUserId)).thenReturn(projectId);
        when(projectAccessGuard.isManager(projectId, otherUserId)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> taskCommentService.delete(commentId, otherUserId));

        verify(taskCommentRepository, never()).delete(any(TaskComment.class));
    }

    @Test
    void delete_ShouldThrowResourceNotFoundException_WhenCommentNotFound() {
        when(taskCommentRepository.findById(commentId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> taskCommentService.delete(commentId, userId));

        verify(taskCommentRepository, never()).delete(any(TaskComment.class));
    }
}
