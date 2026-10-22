package com.sonhoang2.sprint_service.task_sprints;

import com.sonhoang2.sprint_service.common.dto.JSendResponse;
import com.sonhoang2.sprint_service.common.dto.TaskSummary;
import com.sonhoang2.sprint_service.common.exception.ResourceConflictException;
import com.sonhoang2.sprint_service.common.security.ProjectAccessGuard;
import com.sonhoang2.sprint_service.feign.TaskServiceClient;
import com.sonhoang2.sprint_service.sprints.SprintRepository;
import com.sonhoang2.sprint_service.sprints.entity.Sprint;
import com.sonhoang2.sprint_service.task_sprints.dto.TaskSprintCreateRequest;
import com.sonhoang2.sprint_service.task_sprints.dto.TaskSprintResponse;
import com.sonhoang2.sprint_service.task_sprints.entity.TaskSprint;
import com.sonhoang2.sprint_service.task_sprints.entity.TaskSprintId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskSprintServiceImplTest {

    @Mock
    private TaskSprintRepository taskSprintRepository;

    @Mock
    private SprintRepository sprintRepository;

    @Mock
    private ProjectAccessGuard projectAccessGuard;

    @Mock
    private TaskServiceClient taskServiceClient;

    @InjectMocks
    private TaskSprintServiceImpl taskSprintService;

    private final UUID userId = UUID.randomUUID();
    private final UUID projectId = UUID.randomUUID();
    private final UUID taskId = UUID.randomUUID();
    private final Pageable pageable = PageRequest.of(0, 10);
    private Sprint sprint;
    private TaskSprintCreateRequest createRequest;

    @BeforeEach
    void setUp() {
        sprint = Sprint.builder().id(UUID.randomUUID()).projectId(projectId).name("Sprint 1").build();
        createRequest = new TaskSprintCreateRequest();
        createRequest.setTaskId(taskId);
        createRequest.setSprintId(sprint.getId());
    }

    @Test
    void create_ShouldLink_WhenTaskAndSprintShareTheCallersProject() {
        when(sprintRepository.findById(sprint.getId())).thenReturn(Optional.of(sprint));
        when(taskServiceClient.findById(taskId, userId)).thenReturn(task(projectId));
        when(taskSprintRepository.save(any(TaskSprint.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskSprintResponse result = taskSprintService.create(createRequest, userId);

        assertEquals(taskId, result.getTaskId());
        verify(projectAccessGuard).requireMember(projectId, userId);
    }

    @Test
    void create_ShouldReject_WhenTaskBelongsToAnotherProject() {
        when(sprintRepository.findById(sprint.getId())).thenReturn(Optional.of(sprint));
        when(taskServiceClient.findById(taskId, userId)).thenReturn(task(UUID.randomUUID()));

        assertThrows(ResourceConflictException.class, () -> taskSprintService.create(createRequest, userId));

        verify(taskSprintRepository, never()).save(any());
    }

    @Test
    void create_ShouldReject_WhenCallerIsNotMemberOfSprintsProject() {
        when(sprintRepository.findById(sprint.getId())).thenReturn(Optional.of(sprint));
        doThrow(new AccessDeniedException("denied")).when(projectAccessGuard).requireMember(projectId, userId);

        assertThrows(AccessDeniedException.class, () -> taskSprintService.create(createRequest, userId));

        verifyNoInteractions(taskServiceClient);
        verify(taskSprintRepository, never()).save(any());
    }

    @Test
    void findAll_ByTaskId_ShouldCheckMembershipOfEachSprintsProject() {
        TaskSprint link = TaskSprint.builder().id(new TaskSprintId(taskId, sprint.getId())).sprint(sprint).build();
        when(taskSprintRepository.findByIdTaskId(taskId, pageable)).thenReturn(new PageImpl<>(List.of(link)));
        doThrow(new AccessDeniedException("denied")).when(projectAccessGuard).requireMember(projectId, userId);

        assertThrows(AccessDeniedException.class, () -> taskSprintService.findAll(taskId, null, pageable, userId));
    }

    @Test
    void findAll_ShouldRequireTaskIdOrSprintId() {
        assertThrows(ResponseStatusException.class, () -> taskSprintService.findAll(null, null, pageable, userId));

        verifyNoInteractions(taskSprintRepository);
    }

    private static JSendResponse<Map<String, TaskSummary>> task(UUID projectId) {
        return JSendResponse.success(Map.of("task", new TaskSummary(UUID.randomUUID(), projectId)));
    }
}
