package com.sonhoang2.sprint_service.sprints;

import com.sonhoang2.sprint_service.common.dto.PageResponse;
import com.sonhoang2.sprint_service.common.security.ProjectAccessGuard;
import com.sonhoang2.sprint_service.sprints.dto.SprintCreateRequest;
import com.sonhoang2.sprint_service.sprints.dto.SprintResponse;
import com.sonhoang2.sprint_service.sprints.entity.Sprint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SprintServiceImplTest {

    @Mock
    private SprintRepository sprintRepository;

    @Mock
    private ProjectAccessGuard projectAccessGuard;

    @InjectMocks
    private SprintServiceImpl sprintService;

    private final UUID userId = UUID.randomUUID();
    private final UUID projectId = UUID.randomUUID();
    private final Pageable pageable = PageRequest.of(0, 10);

    @Test
    void create_ShouldNotSave_WhenCallerIsNotMember() {
        SprintCreateRequest request = SprintCreateRequest.builder().projectId(projectId).name("Sprint 1").build();
        doThrow(new AccessDeniedException("denied")).when(projectAccessGuard).requireMember(projectId, userId);

        assertThrows(AccessDeniedException.class, () -> sprintService.create(request, userId));

        verify(sprintRepository, never()).save(any());
    }

    @Test
    void findAll_WithProjectId_ShouldCheckMembership() {
        when(sprintRepository.findByProjectId(projectId, pageable)).thenReturn(new PageImpl<>(List.of(sprint())));

        PageResponse<SprintResponse> result = sprintService.findAll(projectId, pageable, userId);

        assertEquals(1, result.content().size());
        verify(projectAccessGuard).requireMember(projectId, userId);
    }

    @Test
    void findAll_WithoutProjectId_ShouldOnlyListCallersProjects() {
        Set<UUID> projectIds = Set.of(projectId);
        when(projectAccessGuard.memberProjectIds(userId)).thenReturn(projectIds);
        when(sprintRepository.findByProjectIdIn(projectIds, pageable)).thenReturn(new PageImpl<>(List.of(sprint())));

        PageResponse<SprintResponse> result = sprintService.findAll(null, pageable, userId);

        assertEquals(1, result.content().size());
        verify(sprintRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void findAll_WithoutProjectId_ShouldReturnEmpty_WhenCallerHasNoProjects() {
        when(projectAccessGuard.memberProjectIds(userId)).thenReturn(Set.of());

        PageResponse<SprintResponse> result = sprintService.findAll(null, pageable, userId);

        assertTrue(result.content().isEmpty());
        verifyNoInteractions(sprintRepository);
    }

    @Test
    void delete_ShouldNotDelete_WhenCallerIsNotMember() {
        Sprint sprint = sprint();
        when(sprintRepository.findById(sprint.getId())).thenReturn(Optional.of(sprint));
        doThrow(new AccessDeniedException("denied")).when(projectAccessGuard).requireMember(projectId, userId);

        assertThrows(AccessDeniedException.class, () -> sprintService.delete(sprint.getId(), userId));

        verify(sprintRepository, never()).delete(any());
    }

    private Sprint sprint() {
        return Sprint.builder().id(UUID.randomUUID()).projectId(projectId).name("Sprint 1").build();
    }
}
