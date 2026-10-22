package com.sonhoang2.task_service.feign;

import com.sonhoang2.task_service.common.dto.JSendResponse;
import com.sonhoang2.task_service.common.dto.LabelSummary;
import com.sonhoang2.task_service.common.dto.ProjectMembership;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@FeignClient(name = "project-service", url = "${services.project-service.url:http://project-service}", path = "/api/v1")
public interface ProjectServiceClient {

    @GetMapping("/projects/memberships/me")
    JSendResponse<Map<String, List<ProjectMembership>>> listMyMemberships(@RequestHeader("X-User-Id") UUID userId);

    /** project-service rejects this unless the user is a member of the label's project. */
    @GetMapping("/labels/{id}")
    JSendResponse<Map<String, LabelSummary>> findLabel(@PathVariable UUID id, @RequestHeader("X-User-Id") UUID userId);
}
