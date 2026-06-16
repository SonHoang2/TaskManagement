package com.sonhoang2.project_service.project.feign;

import com.sonhoang2.project_service.common.dto.JSendResponse;
import com.sonhoang2.project_service.common.dto.PageResponse;
import com.sonhoang2.project_service.common.dto.UserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;
import java.util.UUID;

@FeignClient(name = "user-service")
public interface UserServiceClient {

    @GetMapping("/users/{id}")
    JSendResponse<Map<String, UserResponse>> findById(@PathVariable UUID id);

    @GetMapping("/users")
    JSendResponse<Map<String, PageResponse<UserResponse>>> findUsers(@RequestParam(required = false) String keyword,
                                                                     Pageable pageable);
}