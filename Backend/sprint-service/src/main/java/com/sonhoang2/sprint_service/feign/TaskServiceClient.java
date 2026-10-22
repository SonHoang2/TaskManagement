package com.sonhoang2.sprint_service.feign;

import com.sonhoang2.sprint_service.common.dto.JSendResponse;
import com.sonhoang2.sprint_service.common.dto.TaskSummary;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;
import java.util.UUID;

@FeignClient(name = "task-service", url = "${services.task-service.url:http://task-service}", path = "/api/v1")
public interface TaskServiceClient {

    /** task-service rejects this unless the user is a member of the task's project. */
    @GetMapping("/tasks/{id}")
    JSendResponse<Map<String, TaskSummary>> findById(@PathVariable UUID id, @RequestHeader("X-User-Id") UUID userId);
}
