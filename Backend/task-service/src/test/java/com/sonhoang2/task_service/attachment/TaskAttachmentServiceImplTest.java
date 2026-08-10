package com.sonhoang2.task_service.attachment;

import com.sonhoang2.task_service.common.dto.PageResponse;
import com.sonhoang2.task_service.common.exception.ResourceNotFoundException;
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

    @InjectMocks
    private TaskAttachmentServiceImpl taskAttachmentService;

    private TaskAttachment attachment;
    private TaskAttachmentCreateRequest createRequest;
    private TaskAttachmentUpdateRequest updateRequest;
    private UUID attachmentId;
    private UUID taskId;
    private UUID uploadedBy;

    @BeforeEach
    void setUp() {
        attachmentId = UUID.randomUUID();
        taskId = UUID.randomUUID();
        uploadedBy = UUID.randomUUID();

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
        createRequest.setUploadedBy(uploadedBy);

        updateRequest = new TaskAttachmentUpdateRequest();
        updateRequest.setFileUrl("https://example.com/updated-file.pdf");
        updateRequest.setFileName("updated-file.pdf");
    }

    @Test
    void create_ShouldReturnAttachmentResponse() {
        when(taskAttachmentRepository.save(any(TaskAttachment.class))).thenReturn(attachment);

        TaskAttachmentResponse result = taskAttachmentService.create(createRequest);

        assertNotNull(result);
        assertEquals(attachmentId, result.getId());
        assertEquals(taskId, result.getTaskId());
        assertEquals("https://example.com/file.pdf", result.getFileUrl());
        assertEquals("file.pdf", result.getFileName());
        assertEquals(uploadedBy, result.getUploadedBy());
        verify(taskAttachmentRepository).save(any(TaskAttachment.class));
    }

    @Test
    void findAll_WithoutFilters_ShouldReturnAllAttachments() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<TaskAttachment> attachmentPage = new PageImpl<>(java.util.List.of(attachment));
        when(taskAttachmentRepository.findAll(pageable)).thenReturn(attachmentPage);

        PageResponse<TaskAttachmentResponse> result = taskAttachmentService.findAll(null, null, pageable);

        assertNotNull(result);
        assertEquals(1, result.content().size());
        verify(taskAttachmentRepository).findAll(pageable);
    }

    @Test
    void findAll_WithTaskIdFilter_ShouldReturnFilteredAttachments() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<TaskAttachment> attachmentPage = new PageImpl<>(java.util.List.of(attachment));
        when(taskAttachmentRepository.findByTaskId(taskId, pageable)).thenReturn(attachmentPage);

        PageResponse<TaskAttachmentResponse> result = taskAttachmentService.findAll(taskId, null, pageable);

        assertNotNull(result);
        assertEquals(1, result.content().size());
        verify(taskAttachmentRepository).findByTaskId(taskId, pageable);
    }

    @Test
    void findAll_WithUploadedByFilter_ShouldReturnFilteredAttachments() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<TaskAttachment> attachmentPage = new PageImpl<>(java.util.List.of(attachment));
        when(taskAttachmentRepository.findByUploadedBy(uploadedBy, pageable)).thenReturn(attachmentPage);

        PageResponse<TaskAttachmentResponse> result = taskAttachmentService.findAll(null, uploadedBy, pageable);

        assertNotNull(result);
        assertEquals(1, result.content().size());
        verify(taskAttachmentRepository).findByUploadedBy(uploadedBy, pageable);
    }

    @Test
    void findAll_WithTaskIdAndUploadedByFilters_ShouldReturnFilteredAttachments() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<TaskAttachment> attachmentPage = new PageImpl<>(java.util.List.of(attachment));
        when(taskAttachmentRepository.findByTaskIdAndUploadedBy(taskId, uploadedBy, pageable)).thenReturn(attachmentPage);

        PageResponse<TaskAttachmentResponse> result = taskAttachmentService.findAll(taskId, uploadedBy, pageable);

        assertNotNull(result);
        assertEquals(1, result.content().size());
        verify(taskAttachmentRepository).findByTaskIdAndUploadedBy(taskId, uploadedBy, pageable);
    }

    @Test
    void findById_ShouldReturnAttachmentResponse_WhenAttachmentExists() {
        when(taskAttachmentRepository.findById(attachmentId)).thenReturn(Optional.of(attachment));

        TaskAttachmentResponse result = taskAttachmentService.findById(attachmentId);

        assertNotNull(result);
        assertEquals(attachmentId, result.getId());
        verify(taskAttachmentRepository).findById(attachmentId);
    }

    @Test
    void findById_ShouldThrowResourceNotFoundException_WhenAttachmentNotFound() {
        when(taskAttachmentRepository.findById(attachmentId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> taskAttachmentService.findById(attachmentId));

        verify(taskAttachmentRepository).findById(attachmentId);
    }

    @Test
    void update_ShouldReturnUpdatedAttachmentResponse_WhenAttachmentExists() {
        when(taskAttachmentRepository.findById(attachmentId)).thenReturn(Optional.of(attachment));
        doNothing().when(modelMapper).map(updateRequest, attachment);

        TaskAttachmentResponse result = taskAttachmentService.update(attachmentId, updateRequest);

        assertNotNull(result);
        verify(taskAttachmentRepository).findById(attachmentId);
        verify(modelMapper).map(updateRequest, attachment);
    }

    @Test
    void update_ShouldThrowResourceNotFoundException_WhenAttachmentNotFound() {
        when(taskAttachmentRepository.findById(attachmentId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> taskAttachmentService.update(attachmentId, updateRequest));

        verify(taskAttachmentRepository).findById(attachmentId);
        verify(modelMapper, never()).map(any(), any());
    }

    @Test
    void delete_ShouldDeleteAttachment_WhenAttachmentExists() {
        when(taskAttachmentRepository.findById(attachmentId)).thenReturn(Optional.of(attachment));
        doNothing().when(taskAttachmentRepository).delete(attachment);

        taskAttachmentService.delete(attachmentId);

        verify(taskAttachmentRepository).findById(attachmentId);
        verify(taskAttachmentRepository).delete(attachment);
    }

    @Test
    void delete_ShouldThrowResourceNotFoundException_WhenAttachmentNotFound() {
        when(taskAttachmentRepository.findById(attachmentId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> taskAttachmentService.delete(attachmentId));

        verify(taskAttachmentRepository).findById(attachmentId);
        verify(taskAttachmentRepository, never()).delete(any(TaskAttachment.class));
    }
}
