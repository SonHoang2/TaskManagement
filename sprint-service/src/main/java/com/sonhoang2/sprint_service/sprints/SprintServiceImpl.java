package com.sonhoang2.sprint_service.sprints;

import com.sonhoang2.sprint_service.common.dto.PageResponse;
import com.sonhoang2.sprint_service.common.exception.ResourceNotFoundException;
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

import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class SprintServiceImpl implements SprintService {

    private final SprintRepository sprintRepository;

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
    public SprintResponse create(SprintCreateRequest request) {
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
    public PageResponse<SprintResponse> findAll(UUID projectId, Pageable pageable) {
        Page<Sprint> page;
        if (projectId != null) {
            page = sprintRepository.findByProjectId(projectId, pageable);
        } else {
            page = sprintRepository.findAll(pageable);
        }
        return toPageResponse(page);
    }

    @Override
    @Transactional(readOnly = true)
    public SprintResponse findById(UUID id) {
        return toResponse(findSprintByIdOrThrow(id));
    }

    @Override
    public SprintResponse update(UUID id, SprintUpdateRequest request) {
        Sprint existing = findSprintByIdOrThrow(id);
        existing.setName(request.getName());
        existing.setStartDate(request.getStartDate());
        existing.setEndDate(request.getEndDate());
        if (request.getStatus() != null) {
            existing.setStatus(request.getStatus());
        }
        return toResponse(sprintRepository.save(existing));
    }

    @Override
    public void delete(UUID id) {
        sprintRepository.delete(findSprintByIdOrThrow(id));
    }

    private Sprint findSprintByIdOrThrow(UUID id) {
        return sprintRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sprint with id " + id + " not found"));
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
