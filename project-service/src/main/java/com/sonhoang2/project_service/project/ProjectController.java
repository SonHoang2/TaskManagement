package com.sonhoang2.project_service.project;

import com.sonhoang2.project_service.common.dto.JSendResponse;
import com.sonhoang2.project_service.common.dto.PageResponse;
import com.sonhoang2.project_service.project.dto.CreateProjectRequest;
import com.sonhoang2.project_service.project.dto.InvitationDecisionRequest;
import com.sonhoang2.project_service.project.dto.InviteMemberRequest;
import com.sonhoang2.project_service.project.dto.ListProjectRequest;
import com.sonhoang2.project_service.project.dto.ProjectDetailResponse;
import com.sonhoang2.project_service.project.dto.ProjectInvitationResponse;
import com.sonhoang2.project_service.project.dto.ProjectMemberResponse;
import com.sonhoang2.project_service.project.dto.ProjectResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping
    public ResponseEntity<JSendResponse<Map<String, ProjectResponse>>> create(
            @Valid @RequestBody CreateProjectRequest request,
            @RequestHeader("X-User-Id") UUID userId) {

        System.out.println("User ID: " + userId);// added header

        ProjectResponse project = projectService.create(request, userId); // pass userId
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(project.getId())
                .toUri();

        return ResponseEntity.created(location).body(JSendResponse.success(Map.of("project", project)));
    }

    @PostMapping("/{projectId}/invites")
    public ResponseEntity<JSendResponse<Map<String, ProjectInvitationResponse>>> inviteMember(
            @PathVariable UUID projectId,
            @Valid @RequestBody InviteMemberRequest request,
            @RequestHeader("X-User-Id") UUID userId) {                     // added header

        ProjectInvitationResponse invitation = projectService.inviteMember(projectId, request, userId);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/projects/invites/{id}")
                .buildAndExpand(invitation.getId())
                .toUri();

        return ResponseEntity.created(location).body(JSendResponse.success(Map.of("invitation", invitation)));
    }

    @PatchMapping("/invites/{invitationId}")
    public ResponseEntity<JSendResponse<Map<String, ProjectInvitationResponse>>> decideInvitation(
            @PathVariable UUID invitationId,
            @Valid @RequestBody InvitationDecisionRequest request,
            @RequestHeader("X-User-Id") UUID userId) {                     // added header
        ProjectInvitationResponse invitation = projectService.decideInvitation(invitationId, request, userId);
        return ResponseEntity.ok(JSendResponse.success(Map.of("invitation", invitation)));
    }

    @GetMapping("/{projectId}/members")
    public ResponseEntity<JSendResponse<Map<String, List<ProjectMemberResponse>>>> listMembers(
            @PathVariable UUID projectId,
            @RequestHeader("X-User-Id") UUID userId) {                     // changed String → UUID

        return ResponseEntity.ok(JSendResponse.success(
                Map.of("members", projectService.listMembers(projectId, userId)) // pass userId
        ));
    }

    @GetMapping("/{projectId}/invitations")
    public ResponseEntity<JSendResponse<Map<String, List<ProjectInvitationResponse>>>> listInvitations(
            @PathVariable UUID projectId,
            @RequestHeader("X-User-Id") UUID userId) {

        return ResponseEntity.ok(JSendResponse.success(
                Map.of("invitations", projectService.listInvitations(projectId, userId))
        ));
    }

    @GetMapping("/invitations/me")
    public ResponseEntity<JSendResponse<Map<String, List<ProjectInvitationResponse>>>> listMyInvitations(
            @RequestHeader("X-User-Id") UUID userId) {

        return ResponseEntity.ok(JSendResponse.success(
                Map.of("invitations", projectService.listInvitationsByInvitee(userId))
        ));
    }

    @GetMapping()
    public ResponseEntity<JSendResponse<PageResponse<ProjectDetailResponse>>> listAllProject(
            Pageable pageable,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection) {

        ListProjectRequest request = new ListProjectRequest();
        request.setSearch(search);
        request.setSortBy(sortBy);
        request.setSortDirection(sortDirection);

        return ResponseEntity.ok(JSendResponse.success(projectService.listAllProject(pageable, userId, request)));
    }

    @GetMapping("/me")
    public ResponseEntity<JSendResponse<PageResponse<ProjectDetailResponse>>> getMyProjects(
            Pageable pageable,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection) {

        ListProjectRequest request = new ListProjectRequest();
        request.setSearch(search);
        request.setSortBy(sortBy);
        request.setSortDirection(sortDirection);

        return ResponseEntity.ok(JSendResponse.success(projectService.getMyProjects(pageable, userId, request)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<JSendResponse<Map<String, ProjectResponse>>> getProjectById(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID userId) {

        return ResponseEntity.ok(JSendResponse.success(
                Map.of("project", projectService.getProjectById(id, userId))
        ));
    }

    @GetMapping("/{id}/tasks")
    public ResponseEntity<JSendResponse<Map<String, PageResponse<Map<String, Object>>>>> getProjectTasks(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID userId,
            Pageable pageable) {

        return ResponseEntity.ok(JSendResponse.success(
                Map.of("page", projectService.getProjectTasks(id, userId, pageable))
        ));
    }
}