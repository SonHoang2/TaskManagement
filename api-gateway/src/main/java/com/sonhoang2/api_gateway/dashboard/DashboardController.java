package com.sonhoang2.api_gateway.dashboard;

import com.sonhoang2.api_gateway.common.dto.JSendResponse;
import com.sonhoang2.api_gateway.dashboard.dto.DashboardResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public Mono<ResponseEntity<JSendResponse<Map<String, DashboardResponse>>>> getDashboard(
            @RequestHeader("X-User-Id") UUID userId) {
        log.info("Dashboard request for user: {}", userId);

        return dashboardService.getDashboard(userId)
                .map(dashboard -> ResponseEntity.ok(
                        JSendResponse.success(Map.of("dashboard", dashboard))
                ))
                .onErrorResume(e -> {
                    log.error("Error fetching dashboard", e);
                    return Mono.just(ResponseEntity.internalServerError()
                            .body(JSendResponse.<Map<String, DashboardResponse>>error("Failed to fetch dashboard data",
                                    500,
                                    null)));
                });
    }
}
