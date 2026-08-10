package com.sonhoang2.task_service.attachment;

import com.sonhoang2.task_service.common.dto.PageResponse;
import com.sonhoang2.task_service.common.exception.ResourceNotFoundException;
import com.sonhoang2.task_service.attachment.dto.TaskAttachmentCreateRequest;
import com.sonhoang2.task_service.attachment.dto.TaskAttachmentResponse;
import com.sonhoang2.task_service.attachment.dto.TaskAttachmentUpdateRequest;
import com.sonhoang2.task_service.attachment.entity.TaskAttachment;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
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
    private final ModelMapper modelMapper;

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
        modelMapper.map(request, attachment);

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



