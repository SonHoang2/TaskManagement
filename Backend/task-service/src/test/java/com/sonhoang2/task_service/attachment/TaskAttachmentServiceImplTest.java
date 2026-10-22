package com.sonhoang2.task_service.attachment;

import com.sonhoang2.task_service.common.dto.PageResponse;
import com.sonhoang2.task_service.common.exception.ResourceNotFoundException;
import com.sonhoang2.task_service.common.security.ProjectAccessGuard;
import com.sonhoang2.task_service.attachment.dto.TaskAttachmentCreateRequest;
import com.sonhoang2.task_service.attachment.dto.TaskAttachmentResponse;
import com.sonhoang2.task_service.attachment.dto.TaskAttachmentUpdateRequest;
import com.sonhoang2.task_service.attachment.entity.TaskAttachment;
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
class TaskAttachmentServiceImplTest {

    @Mock
    private TaskAttachmentRepository taskAttachmentRepository;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private ProjectAccessGuard projectAccessGuard;

    @InjectMocks
    private TaskAttachmentServiceImpl taskAttachmentService;

    private TaskAttachment attachment;
    private TaskAttachmentCreateRequest createRequest;
    private TaskAttachmentUpdateRequest updateRequest;
    private UUID attachmentId;
    private UUID taskId;
    private UUID uploadedBy;
    private UUID otherUserId;
    private UUID projectId;

    @BeforeEach
    void setUp() {
        attachmentId = UUID.randomUUID();
        taskId = UUID.randomUUID();
        uploadedBy = UUID.randomUUID();
        otherUserId = UUID.randomUUID();
        projectId = UUID.randomUUID();

        attachment = TaskAttachment.builder()
                .id(attachmentId)
                .taskId(taskId)
                .fileUrl("https://example.com/file.pdf")
                .fileName("file.pdf")
                .uploadedBy(uploadedBy)
                .createdAt(Instant.now())
                .build();

        createRequest = new TaskAttachmentCreateRequest();
        createRequest.setTaskId(taskId);
        createRequest.setFileUrl("https://example.com/file.pdf");
        createRequest.setFileName("file.pdf");

        updateRequest = new TaskAttachmentUpdateRequest();
        updateRequest.setFileUrl("https://example.com/updated-file.pdf");
        updateRequest.setFileName("updated-file.pdf");
    }

    @Test
    void create_ShouldUseCallerAsUploader_WhenCallerIsMember() {
        when(taskAttachmentRepository.save(any(TaskAttachment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TaskAttachmentResponse result = taskAttachmentService.create(createRequest, uploadedBy);

        assertEquals(taskId, result.getTaskId());
        assertEquals(uploadedBy, result.getUploadedBy());
        assertEquals("file.pdf", result.getFileName());
        verify(projectAccessGuard).requireTaskMember(taskId, uploadedBy);
    }

    @Test
    void create_ShouldThrowAccessDenied_WhenCallerIsNotMember() {
        when(projectAccessGuard.requireTaskMember(taskId, uploadedBy)).thenThrow(new AccessDeniedException("denied"));

        assertThrows(AccessDeniedException.class, () -> taskAttachmentService.create(createRequest, uploadedBy));

        verify(taskAttachmentRepository, never()).save(any(TaskAttachment.class));
    }

    @Test
    void findAll_WithTaskIdFilter_ShouldReturnFilteredAttachments() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<TaskAttachment> attachmentPage = new PageImpl<>(java.util.List.of(attachment));
        when(taskAttachmentRepository.findByTaskId(taskId, pageable)).thenReturn(attachmentPage);

        PageResponse<TaskAttachmentResponse> result =
                taskAttachmentService.findAll(taskId, null, pageable, uploadedBy);

        assertEquals(1, result.content().size());
        verify(projectAccessGuard).requireTaskMember(taskId, uploadedBy);
    }

    @Test
    void findAll_WithTaskIdAndUploadedByFilters_ShouldReturnFilteredAttachments() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<TaskAttachment> attachmentPage = new PageImpl<>(java.util.List.of(attachment));
        when(taskAttachmentRepository.findByTaskIdAndUploadedBy(taskId, uploadedBy, pageable))
                .thenReturn(attachmentPage);

        PageResponse<TaskAttachmentResponse> result =
                taskAttachmentService.findAll(taskId, uploadedBy, pageable, uploadedBy);

        assertEquals(1, result.content().size());
    }

    @Test
    void findAll_ShouldThrowAccessDenied_WhenCallerIsNotMember() {
        Pageable pageable = PageRequest.of(0, 10);
        when(projectAccessGuard.requireTaskMember(taskId, otherUserId)).thenThrow(new AccessDeniedException("denied"));

        assertThrows(AccessDeniedException.class,
                () -> taskAttachmentService.findAll(taskId, null, pageable, otherUserId));

        verifyNoInteractions(taskAttachmentRepository);
    }

    @Test
    void findById_ShouldReturnAttachmentResponse_WhenCallerIsMember() {
        when(taskAttachmentRepository.findById(attachmentId)).thenReturn(Optional.of(attachment));

        TaskAttachmentResponse result = taskAttachmentService.findById(attachmentId, uploadedBy);

        assertEquals(attachmentId, result.getId());
        verify(projectAccessGuard).requireTaskMember(taskId, uploadedBy);
    }

    @Test
    void findById_ShouldThrowResourceNotFoundException_WhenAttachmentNotFound() {
        when(taskAttachmentRepository.findById(attachmentId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> taskAttachmentService.findById(attachmentId, uploadedBy));
    }

    @Test
    void update_ShouldReturnUpdatedAttachmentResponse_WhenCallerIsUploader() {
        when(taskAttachmentRepository.findById(attachmentId)).thenReturn(Optional.of(attachment));
        doNothing().when(modelMapper).map(updateRequest, attachment);

        TaskAttachmentResponse result = taskAttachmentService.update(attachmentId, updateRequest, uploadedBy);

        assertNotNull(result);
        verify(modelMapper).map(updateRequest, attachment);
    }

    @Test
    void update_ShouldThrowAccessDenied_WhenCallerIsNotUploader() {
        when(taskAttachmentRepository.findById(attachmentId)).thenReturn(Optional.of(attachment));

        assertThrows(AccessDeniedException.class,
                () -> taskAttachmentService.update(attachmentId, updateRequest, otherUserId));

        verify(modelMapper, never()).map(any(), any());
    }

    @Test
    void update_ShouldThrowResourceNotFoundException_WhenAttachmentNotFound() {
        when(taskAttachmentRepository.findById(attachmentId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> taskAttachmentService.update(attachmentId, updateRequest, uploadedBy));

        verify(modelMapper, never()).map(any(), any());
    }

    @Test
    void delete_ShouldDeleteAttachment_WhenCallerIsUploader() {
        when(taskAttachmentRepository.findById(attachmentId)).thenReturn(Optional.of(attachment));

        taskAttachmentService.delete(attachmentId, uploadedBy);

        verify(taskAttachmentRepository).delete(attachment);
    }

    @Test
    void delete_ShouldDeleteAttachment_WhenCallerIsProjectManager() {
        when(taskAttachmentRepository.findById(attachmentId)).thenReturn(Optional.of(attachment));
        when(projectAccessGuard.requireTaskMember(taskId, otherUserId)).thenReturn(projectId);
        when(projectAccessGuard.isManager(projectId, otherUserId)).thenReturn(true);

        taskAttachmentService.delete(attachmentId, otherUserId);

        verify(taskAttachmentRepository).delete(attachment);
    }

    @Test
    void delete_ShouldThrowAccessDenied_WhenCallerIsNeitherUploaderNorManager() {
        when(taskAttachmentRepository.findById(attachmentId)).thenReturn(Optional.of(attachment));
        when(projectAccessGuard.requireTaskMember(taskId, otherUserId)).thenReturn(projectId);
        when(projectAccessGuard.isManager(projectId, otherUserId)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> taskAttachmentService.delete(attachmentId, otherUserId));

        verify(taskAttachmentRepository, never()).delete(any(TaskAttachment.class));
    }

    @Test
    void delete_ShouldThrowResourceNotFoundException_WhenAttachmentNotFound() {
        when(taskAttachmentRepository.findById(attachmentId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> taskAttachmentService.delete(attachmentId, uploadedBy));

        verify(taskAttachmentRepository, never()).delete(any(TaskAttachment.class));
    }
}
