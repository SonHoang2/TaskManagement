package com.sonhoang2.sprint_service.sprints;

import com.sonhoang2.sprint_service.common.dto.PageResponse;
import com.sonhoang2.sprint_service.sprints.dto.SprintCreateRequest;
import com.sonhoang2.sprint_service.sprints.dto.SprintResponse;
import com.sonhoang2.sprint_service.sprints.dto.SprintUpdateRequest;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface SprintService {

    SprintResponse create(SprintCreateRequest request, UUID userId);

    PageResponse<SprintResponse> findAll(UUID projectId, Pageable pageable, UUID userId);

    SprintResponse findById(UUID id, UUID userId);

    SprintResponse update(UUID id, SprintUpdateRequest request, UUID userId);

    void delete(UUID id, UUID userId);
}
