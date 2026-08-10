package com.sonhoang2.project_service.project.feign;

import com.sonhoang2.project_service.common.dto.JSendResponse;
import com.sonhoang2.project_service.common.dto.PageResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;
import java.util.UUID;

@FeignClient(name = "task-service")
public interface TaskServiceClient {

    @GetMapping("/tasks/project/{projectId}")
    JSendResponse<Map<String, PageResponse<Map<String, Object>>>> findByProjectId(
            @PathVariable UUID projectId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam("page") int page,
            @RequestParam("size") int size,
            @RequestParam(value = "sort", required = false) String sort);

    @GetMapping("/tasks/project/{projectId}/stats")
    JSendResponse<Map<String, Map<String, Object>>> getTaskStatsByProjectId(@PathVariable UUID projectId);
}
