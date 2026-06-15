//package com.sonhoang2.project_service.project;
//
//import com.sonhoang2.project_service.common.dto.JSendResponse;
//import com.sonhoang2.project_service.project.dto.CreateProjectRequest;
//import com.sonhoang2.project_service.project.dto.InvitationDecisionRequest;
//import com.sonhoang2.project_service.project.dto.InviteMemberRequest;
//import com.sonhoang2.project_service.project.dto.ProjectInvitationResponse;
//import com.sonhoang2.project_service.project.dto.ProjectMemberResponse;
//import com.sonhoang2.project_service.project.dto.ProjectResponse;
//import jakarta.validation.Valid;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.PatchMapping;
//import org.springframework.web.bind.annotation.PathVariable;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
//
//import java.net.URI;
//import java.util.List;
//import java.util.Map;
//import java.util.UUID;
//
//@RestController
//@RequestMapping("/projects")
//public class ProjectController {
//
//    private final ProjectService projectService;
//
//    public ProjectController(ProjectService projectService) {
//        this.projectService = projectService;
//    }
//
//    @PostMapping
//    public ResponseEntity<JSendResponse<Map<String, ProjectResponse>>> create(
//            @Valid @RequestBody CreateProjectRequest request
//    ) {
//        ProjectResponse project = projectService.create(request);
//        URI location = ServletUriComponentsBuilder
//                .fromCurrentRequest()
//                .path("/{id}")
//                .buildAndExpand(project.getId())
//                .toUri();
//
//        return ResponseEntity.created(location)
//                .body(JSendResponse.success(Map.of("project", project)));
//    }
//
//    @PostMapping("/{projectId}/invites")
//    public ResponseEntity<JSendResponse<Map<String, ProjectInvitationResponse>>> inviteMember(
//            @PathVariable UUID projectId,
//            @Valid @RequestBody InviteMemberRequest request
//    ) {
//        ProjectInvitationResponse invitation = projectService.inviteMember(projectId, request);
//        URI location = ServletUriComponentsBuilder
//                .fromCurrentContextPath()
//                .path("/projects/invites/{id}")
//                .buildAndExpand(invitation.getId())
//                .toUri();
//
//        return ResponseEntity.created(location)
//                .body(JSendResponse.success(Map.of("invitation", invitation)));
//    }
//
//    @PatchMapping("/invites/{invitationId}")
//    public ResponseEntity<JSendResponse<Map<String, ProjectInvitationResponse>>> decideInvitation(
//            @PathVariable UUID invitationId,
//            @Valid @RequestBody InvitationDecisionRequest request
//    ) {
//        ProjectInvitationResponse invitation = projectService.decideInvitation(invitationId, request);
//        return ResponseEntity.ok(JSendResponse.success(Map.of("invitation", invitation)));
//    }
//
//    @GetMapping("/{projectId}/members")
//    public ResponseEntity<JSendResponse<Map<String, List<ProjectMemberResponse>>>> listMembers(
//            @PathVariable UUID projectId
//    ) {
//        return ResponseEntity.ok(JSendResponse.success(Map.of("members", projectService.listMembers(projectId))));
//    }
//
//    @GetMapping()
//    public ResponseEntity<JSendResponse<Map<String, List<ProjectResponse>>>> listAllProject() {
//        return ResponseEntity.ok(JSendResponse.success(Map.of("projects", projectService.listAllProject())));
//    }
//}
//
