package com.sonhoang2.task_service.feign;

import com.sonhoang2.task_service.common.dto.JSendResponse;
import com.sonhoang2.task_service.common.dto.ProjectResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;
import java.util.UUID;

@FeignClient(name = "project-service")
public interface ProjectServiceClient {

    @GetMapping("/projects/{id}")
    JSendResponse<Map<String, ProjectResponse>> findById(@PathVariable UUID id,
                                                         @RequestHeader("X-User-Id") UUID userId);
}