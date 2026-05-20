package com.sonhoang2.TaskManagementAPI.label;

import com.sonhoang2.TaskManagementAPI.common.dto.PageResponse;
import com.sonhoang2.TaskManagementAPI.label.dto.LabelCreateRequest;
import com.sonhoang2.TaskManagementAPI.label.dto.LabelResponse;
import com.sonhoang2.TaskManagementAPI.label.dto.LabelUpdateRequest;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface LabelService {

    LabelResponse create(LabelCreateRequest request);

    PageResponse<LabelResponse> findAll(UUID projectId, String name, Pageable pageable);

    LabelResponse findById(UUID id);

    LabelResponse update(UUID id, LabelUpdateRequest request);

    void delete(UUID id);
}
