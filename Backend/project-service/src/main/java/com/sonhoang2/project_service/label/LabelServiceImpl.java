package com.sonhoang2.project_service.label;

import com.sonhoang2.project_service.common.dto.PageResponse;
import com.sonhoang2.project_service.common.exception.ResourceNotFoundException;
import com.sonhoang2.project_service.label.dto.LabelCreateRequest;
import com.sonhoang2.project_service.label.dto.LabelRepository;
import com.sonhoang2.project_service.label.dto.LabelResponse;
import com.sonhoang2.project_service.label.dto.LabelUpdateRequest;
import com.sonhoang2.project_service.label.entity.Label;
import com.sonhoang2.project_service.project.ProjectMemberRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class LabelServiceImpl implements LabelService {

    private final LabelRepository labelRepository;
    private final ModelMapper modelMapper;
    private final ProjectMemberRepository projectMemberRepository;

    private PageResponse<LabelResponse> toPageResponse(Page<Label> page) {
        return new PageResponse<>(
                page.getContent().stream().map(this::toResponse).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext(),
                page.hasPrevious(),
                page.getNumberOfElements()
        );
    }

    @Override
    public LabelResponse create(LabelCreateRequest request, UUID userId) {
        requireMember(request.getProjectId(), userId);

        Label label = Label.builder()
                .projectId(request.getProjectId())
                .name(request.getName())
                .color(request.getColor())
                .build();
        return toResponse(labelRepository.save(label));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<LabelResponse> findAll(UUID projectId, String name, Pageable pageable, UUID userId) {
        requireMember(projectId, userId);

        Page<Label> page = name != null
                ? labelRepository.findByProjectIdAndNameContainingIgnoreCase(projectId, name, pageable)
                : labelRepository.findByProjectId(projectId, pageable);

        return toPageResponse(page);
    }

    @Override
    @Transactional(readOnly = true)
    public LabelResponse findById(UUID id, UUID userId) {
        return toResponse(findAccessibleLabelOrThrow(id, userId));
    }

    @Override
    public LabelResponse update(UUID id, LabelUpdateRequest request, UUID userId) {
        Label existing = findAccessibleLabelOrThrow(id, userId);

        modelMapper.map(request, existing);
        return toResponse(labelRepository.save(existing));
    }

    @Override
    public void delete(UUID id, UUID userId) {
        labelRepository.delete(findAccessibleLabelOrThrow(id, userId));
    }

    private Label findAccessibleLabelOrThrow(UUID id, UUID userId) {
        Label label = labelRepository.findById(id)
                .orElseThrow(() -> {
                    return new ResourceNotFoundException("Label with id " + id + " not found");
                });
        requireMember(label.getProjectId(), userId);
        return label;
    }

    private void requireMember(UUID projectId, UUID userId) {
        if (!projectMemberRepository.existsByProjectIdAndUserId(projectId, userId)) {
            throw new AccessDeniedException("You are not a member of this project");
        }
    }

    private LabelResponse toResponse(Label label) {
        return LabelResponse.builder()
                .id(label.getId())
                .projectId(label.getProjectId())
                .name(label.getName())
                .color(label.getColor())
                .createdAt(label.getCreatedAt())
                .build();
    }
}
