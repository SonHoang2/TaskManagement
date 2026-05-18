package com.sonhoang2.TaskManagementAPI.attachment;

import com.sonhoang2.TaskManagementAPI.common.dto.PageResponse;
import com.sonhoang2.TaskManagementAPI.common.exception.ResourceNotFoundException;
import com.sonhoang2.TaskManagementAPI.attachment.dto.TaskAttachmentCreateRequest;
import com.sonhoang2.TaskManagementAPI.attachment.dto.TaskAttachmentResponse;
import com.sonhoang2.TaskManagementAPI.attachment.dto.TaskAttachmentUpdateRequest;
import com.sonhoang2.TaskManagementAPI.attachment.entity.TaskAttachment;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class TaskAttachmentServiceImpl implements TaskAttachmentService {

    private final TaskAttachmentRepository taskAttachmentRepository;

    private PageResponse<TaskAttachmentResponse> toPageResponse(Page<TaskAttachment> page) {
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
    public TaskAttachmentResponse create(TaskAttachmentCreateRequest request) {
        TaskAttachment attachment = TaskAttachment.builder()
                .taskId(request.getTaskId())
                .fileUrl(request.getFileUrl())
                .fileName(request.getFileName())
                .uploadedBy(request.getUploadedBy())
                .build();

        return toResponse(taskAttachmentRepository.save(attachment));
    }

    @Override
    public PageResponse<TaskAttachmentResponse> findAll(UUID taskId, UUID uploadedBy, Pageable pageable) {
        Page<TaskAttachment> page;

        if (taskId != null && uploadedBy != null) {
            page = taskAttachmentRepository.findByTaskIdAndUploadedBy(taskId, uploadedBy, pageable);
        } else if (taskId != null) {
            page = taskAttachmentRepository.findByTaskId(taskId, pageable);
        } else if (uploadedBy != null) {
            page = taskAttachmentRepository.findByUploadedBy(uploadedBy, pageable);
        } else {
            page = taskAttachmentRepository.findAll(pageable);
        }

        return toPageResponse(page);
    }

    @Override
    @Transactional(readOnly = true)
    public TaskAttachmentResponse findById(UUID id) {
        return toResponse(findAttachmentByIdOrThrow(id));
    }

    @Override
    public TaskAttachmentResponse update(UUID id, TaskAttachmentUpdateRequest request) {
        TaskAttachment attachment = findAttachmentByIdOrThrow(id);
        attachment.setTaskId(request.getTaskId());
        attachment.setFileUrl(request.getFileUrl());
        attachment.setFileName(request.getFileName());
        attachment.setUploadedBy(request.getUploadedBy());

        return toResponse(attachment);
    }

    @Override
    public void delete(UUID id) {
        taskAttachmentRepository.delete(findAttachmentByIdOrThrow(id));
    }

    private TaskAttachment findAttachmentByIdOrThrow(UUID id) {
        return taskAttachmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TaskAttachment with id " + id + " not found"));
    }

    private TaskAttachmentResponse toResponse(TaskAttachment attachment) {
        return TaskAttachmentResponse.builder()
                .id(attachment.getId())
                .taskId(attachment.getTaskId())
                .fileUrl(attachment.getFileUrl())
                .fileName(attachment.getFileName())
                .uploadedBy(attachment.getUploadedBy())
                .createdAt(attachment.getCreatedAt())
                .build();
    }
}



