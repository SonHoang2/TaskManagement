package com.sonhoang2.project_service.label;

import com.sonhoang2.project_service.label.dto.LabelCreateRequest;
import com.sonhoang2.project_service.label.dto.LabelResponse;
import com.sonhoang2.project_service.label.dto.LabelUpdateRequest;
import com.sonhoang2.project_service.common.dto.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface LabelService {

    LabelResponse create(LabelCreateRequest request);

    PageResponse<LabelResponse> findAll(UUID projectId, String name, Pageable pageable);

    LabelResponse findById(UUID id);

    LabelResponse update(UUID id, LabelUpdateRequest request);

    void delete(UUID id);
}
