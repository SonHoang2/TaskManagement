package com.sonhoang2.sprint_service.sprints;

import com.sonhoang2.sprint_service.common.dto.PageResponse;
import com.sonhoang2.sprint_service.common.exception.ResourceNotFoundException;
import com.sonhoang2.sprint_service.common.security.ProjectAccessGuard;
import com.sonhoang2.sprint_service.sprints.dto.SprintCreateRequest;
import com.sonhoang2.sprint_service.sprints.dto.SprintResponse;
import com.sonhoang2.sprint_service.sprints.dto.SprintUpdateRequest;
import com.sonhoang2.sprint_service.sprints.entity.Sprint;
import com.sonhoang2.sprint_service.sprints.entity.SprintStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class SprintServiceImpl implements SprintService {

    private final SprintRepository sprintRepository;
    private final ProjectAccessGuard projectAccessGuard;

    private PageResponse<SprintResponse> toPageResponse(Page<Sprint> page) {
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
    public SprintResponse create(SprintCreateRequest request, UUID userId) {
        projectAccessGuard.requireMember(request.getProjectId(), userId);

        Sprint sprint = Sprint.builder()
                .projectId(request.getProjectId())
                .name(request.getName())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(request.getStatus() != null ? request.getStatus() : SprintStatus.PLANNED.name())
                .build();
        return toResponse(sprintRepository.save(sprint));
    }

    @Override
    public PageResponse<SprintResponse> findAll(UUID projectId, Pageable pageable, UUID userId) {
        if (projectId != null) {
            projectAccessGuard.requireMember(projectId, userId);
            return toPageResponse(sprintRepository.findByProjectId(projectId, pageable));
        }

        Set<UUID> projectIds = projectAccessGuard.memberProjectIds(userId);
        Page<Sprint> page = projectIds.isEmpty()
                ? Page.empty(pageable)
                : sprintRepository.findByProjectIdIn(projectIds, pageable);
        return toPageResponse(page);
    }

    @Override
    @Transactional(readOnly = true)
    public SprintResponse findById(UUID id, UUID userId) {
        return toResponse(findAccessibleSprintOrThrow(id, userId));
    }

    @Override
    public SprintResponse update(UUID id, SprintUpdateRequest request, UUID userId) {
        Sprint existing = findAccessibleSprintOrThrow(id, userId);
        existing.setName(request.getName());
        existing.setStartDate(request.getStartDate());
        existing.setEndDate(request.getEndDate());
        if (request.getStatus() != null) {
            existing.setStatus(request.getStatus());
        }
        return toResponse(sprintRepository.save(existing));
    }

    @Override
    public void delete(UUID id, UUID userId) {
        sprintRepository.delete(findAccessibleSprintOrThrow(id, userId));
    }

    private Sprint findAccessibleSprintOrThrow(UUID id, UUID userId) {
        Sprint sprint = sprintRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sprint with id " + id + " not found"));
        projectAccessGuard.requireMember(sprint.getProjectId(), userId);
        return sprint;
    }

    private SprintResponse toResponse(Sprint sprint) {
        return SprintResponse.builder()
                .id(sprint.getId())
                .projectId(sprint.getProjectId())
                .name(sprint.getName())
                .startDate(sprint.getStartDate())
                .endDate(sprint.getEndDate())
                .status(sprint.getStatus())
                .createdAt(sprint.getCreatedAt())
                .build();
    }
}
