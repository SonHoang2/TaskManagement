package com.sonhoang2.project_service.label;

import com.sonhoang2.TaskManagementAPI.common.dto.JSendResponse;
import com.sonhoang2.TaskManagementAPI.common.dto.PageResponse;
import com.sonhoang2.TaskManagementAPI.label.dto.LabelCreateRequest;
import com.sonhoang2.TaskManagementAPI.label.dto.LabelResponse;
import com.sonhoang2.TaskManagementAPI.label.dto.LabelUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
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
            @Valid @RequestBody LabelCreateRequest request
    ) {
        LabelResponse label = labelService.create(request);
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
            @RequestParam(required = false) UUID projectId,
            @RequestParam(required = false) String name,
            Pageable pageable
    ) {
        PageResponse<LabelResponse> pageResponse = labelService.findAll(projectId, name, pageable);
        return ResponseEntity.ok(JSendResponse.success(Map.of("page", pageResponse)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<JSendResponse<Map<String, LabelResponse>>> findById(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(JSendResponse.success(Map.of("label", labelService.findById(id))));
    }

    @PutMapping("/{id}")
    public ResponseEntity<JSendResponse<Map<String, LabelResponse>>> update(
            @PathVariable UUID id,
            @Valid @RequestBody LabelUpdateRequest request
    ) {
        return ResponseEntity.ok(JSendResponse.success(Map.of("label", labelService.update(id, request))));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<JSendResponse<Void>> delete(
            @PathVariable UUID id
    ) {
        labelService.delete(id);
        return ResponseEntity.ok(JSendResponse.success(null));
    }
}
