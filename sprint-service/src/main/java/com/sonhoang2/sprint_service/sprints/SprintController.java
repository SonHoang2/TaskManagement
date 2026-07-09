package com.sonhoang2.sprint_service.sprints;

import com.sonhoang2.sprint_service.common.dto.JSendResponse;
import com.sonhoang2.sprint_service.common.dto.PageResponse;
import com.sonhoang2.sprint_service.sprints.dto.SprintCreateRequest;
import com.sonhoang2.sprint_service.sprints.dto.SprintResponse;
import com.sonhoang2.sprint_service.sprints.dto.SprintUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
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
            @Valid @RequestBody SprintCreateRequest request
    ) {
        SprintResponse sprint = sprintService.create(request);
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
            Pageable pageable
    ) {
        PageResponse<SprintResponse> pageResponse = sprintService.findAll(projectId, pageable);
        return ResponseEntity.ok(JSendResponse.success(Map.of("page", pageResponse)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<JSendResponse<Map<String, SprintResponse>>> findById(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(JSendResponse.success(Map.of("sprint", sprintService.findById(id))));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<JSendResponse<Map<String, SprintResponse>>> update(
            @PathVariable UUID id,
            @Valid @RequestBody SprintUpdateRequest request
    ) {
        return ResponseEntity.ok(JSendResponse.success(Map.of("sprint", sprintService.update(id, request))));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<JSendResponse<Void>> delete(
            @PathVariable UUID id
    ) {
        sprintService.delete(id);
        return ResponseEntity.ok(JSendResponse.success(null));
    }
}
