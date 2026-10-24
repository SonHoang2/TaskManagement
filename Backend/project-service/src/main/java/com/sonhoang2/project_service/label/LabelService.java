package com.sonhoang2.project_service.label;

import com.sonhoang2.project_service.common.dto.PageResponse;
import com.sonhoang2.project_service.label.dto.LabelCreateRequest;
import com.sonhoang2.project_service.label.dto.LabelResponse;
import com.sonhoang2.project_service.label.dto.LabelUpdateRequest;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface LabelService {

    LabelResponse create(LabelCreateRequest request, UUID userId);

    PageResponse<LabelResponse> findAll(UUID projectId, String name, Pageable pageable, UUID userId);

    LabelResponse findById(UUID id, UUID userId);

    LabelResponse update(UUID id, LabelUpdateRequest request, UUID userId);

    void delete(UUID id, UUID userId);
}
