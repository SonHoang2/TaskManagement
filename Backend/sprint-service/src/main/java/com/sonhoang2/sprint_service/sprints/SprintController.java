package com.sonhoang2.sprint_service.sprints;

import com.sonhoang2.sprint_service.common.dto.JSendResponse;
import com.sonhoang2.sprint_service.common.dto.PageResponse;
import com.sonhoang2.sprint_service.sprints.dto.SprintCreateRequest;
import com.sonhoang2.sprint_service.sprints.dto.SprintResponse;
import com.sonhoang2.sprint_service.sprints.dto.SprintUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/sprints")
public class SprintController {

    private final SprintService sprintService;

    public SprintController(SprintService sprintService) {
        this.sprintService = sprintService;
    }

    @PostMapping
    public ResponseEntity<JSendResponse<Map<String, SprintResponse>>> create(
            @Valid @RequestBody SprintCreateRequest request,
            @RequestHeader("X-User-Id") UUID userId
    ) {
        SprintResponse sprint = sprintService.create(request, userId);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(sprint.getId())
                .toUri();

        return ResponseEntity.created(location)
                .body(JSendResponse.success(Map.of("sprint", sprint)));
    }

    @GetMapping
    public ResponseEntity<JSendResponse<Map<String, PageResponse<SprintResponse>>>> findAll(
            @RequestParam(required = false) UUID projectId,
            @RequestHeader("X-User-Id") UUID userId,
            Pageable pageable
    ) {
        PageResponse<SprintResponse> pageResponse = sprintService.findAll(projectId, pageable, userId);
        return ResponseEntity.ok(JSendResponse.success(Map.of("page", pageResponse)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<JSendResponse<Map<String, SprintResponse>>> findById(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID userId
    ) {
        return ResponseEntity.ok(JSendResponse.success(Map.of("sprint", sprintService.findById(id, userId))));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<JSendResponse<Map<String, SprintResponse>>> update(
            @PathVariable UUID id,
            @Valid @RequestBody SprintUpdateRequest request,
            @RequestHeader("X-User-Id") UUID userId
    ) {
        return ResponseEntity.ok(JSendResponse.success(Map.of("sprint", sprintService.update(id, request, userId))));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<JSendResponse<Void>> delete(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID userId
    ) {
        sprintService.delete(id, userId);
        return ResponseEntity.ok(JSendResponse.success(null));
    }
}
