package com.sonhoang2.project_service.label;

import com.sonhoang2.project_service.common.dto.PageResponse;
import com.sonhoang2.project_service.common.exception.ResourceNotFoundException;
import com.sonhoang2.project_service.label.dto.LabelCreateRequest;
import com.sonhoang2.project_service.label.dto.LabelRepository;
import com.sonhoang2.project_service.label.dto.LabelResponse;
import com.sonhoang2.project_service.label.dto.LabelUpdateRequest;
import com.sonhoang2.project_service.label.entity.Label;
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
public class LabelServiceImpl implements LabelService {

    private final LabelRepository labelRepository;
    private final ModelMapper modelMapper;

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
    public LabelResponse create(LabelCreateRequest request) {
        Label label = Label.builder()
                .projectId(request.getProjectId())
                .name(request.getName())
                .color(request.getColor())
                .build();
        return toResponse(labelRepository.save(label));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<LabelResponse> findAll(UUID projectId, String name, Pageable pageable) {
        Page<Label> page;

        if (projectId != null && name != null) {
            page = labelRepository.findByProjectIdAndNameContainingIgnoreCase(projectId, name, pageable);
        } else if (projectId != null) {
            page = labelRepository.findByProjectId(projectId, pageable);
        } else if (name != null) {
            page = labelRepository.findByNameContainingIgnoreCase(name, pageable);
        } else {
            page = labelRepository.findAll(pageable);
        }

        return toPageResponse(page);
    }

    @Override
    @Transactional(readOnly = true)
    public LabelResponse findById(UUID id) {
        return toResponse(findLabelByIdOrThrow(id));
    }

    @Override
    public LabelResponse update(UUID id, LabelUpdateRequest request) {
        Label existing = findLabelByIdOrThrow(id);

        modelMapper.map(request, existing);
        return toResponse(labelRepository.save(existing));
    }

    @Override
    public void delete(UUID id) {
        labelRepository.delete(findLabelByIdOrThrow(id));
    }

    private Label findLabelByIdOrThrow(UUID id) {
        return labelRepository.findById(id)
                .orElseThrow(() -> {
                    return new ResourceNotFoundException("Label with id " + id + " not found");
                });
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
