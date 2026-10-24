package com.sonhoang2.project_service.label;

import com.sonhoang2.project_service.common.dto.JSendResponse;
import com.sonhoang2.project_service.common.dto.PageResponse;
import com.sonhoang2.project_service.label.dto.LabelCreateRequest;
import com.sonhoang2.project_service.label.dto.LabelResponse;
import com.sonhoang2.project_service.label.dto.LabelUpdateRequest;
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
@RequestMapping("/labels")
public class LabelController {

    private final LabelService labelService;

    public LabelController(LabelService labelService) {
        this.labelService = labelService;
    }

    @PostMapping
    public ResponseEntity<JSendResponse<Map<String, LabelResponse>>> create(
            @Valid @RequestBody LabelCreateRequest request,
            @RequestHeader("X-User-Id") UUID userId
    ) {
        LabelResponse label = labelService.create(request, userId);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(label.getId())
                .toUri();

        return ResponseEntity.created(location)
                .body(JSendResponse.success(Map.of("label", label)));
    }

    @GetMapping
    public ResponseEntity<JSendResponse<Map<String, PageResponse<LabelResponse>>>> findAll(
            @RequestParam UUID projectId,
            @RequestParam(required = false) String name,
            @RequestHeader("X-User-Id") UUID userId,
            Pageable pageable
    ) {
        PageResponse<LabelResponse> pageResponse = labelService.findAll(projectId, name, pageable, userId);
        return ResponseEntity.ok(JSendResponse.success(Map.of("page", pageResponse)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<JSendResponse<Map<String, LabelResponse>>> findById(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID userId
    ) {
        return ResponseEntity.ok(JSendResponse.success(Map.of("label", labelService.findById(id, userId))));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<JSendResponse<Map<String, LabelResponse>>> update(
            @PathVariable UUID id,
            @Valid @RequestBody LabelUpdateRequest request,
            @RequestHeader("X-User-Id") UUID userId
    ) {
        return ResponseEntity.ok(JSendResponse.success(Map.of("label", labelService.update(id, request, userId))));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<JSendResponse<Void>> delete(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID userId
    ) {
        labelService.delete(id, userId);
        return ResponseEntity.ok(JSendResponse.success(null));
    }
}
