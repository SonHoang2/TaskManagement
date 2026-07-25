package com.sonhoang2.dashboard_service;

import com.sonhoang2.dashboard_service.dto.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final WebClient webClient;
    private final CircuitBreaker projectServiceCircuitBreaker;
    private final CircuitBreaker taskServiceCircuitBreaker;
    private final CircuitBreaker userServiceCircuitBreaker;
    private final CircuitBreaker sprintServiceCircuitBreaker;
    private final ObjectMapper objectMapper;

    private static final String PROJECT_SERVICE_URL = "http://project-service";
    private static final String TASK_SERVICE_URL = "http://task-service";
    private static final String USER_SERVICE_URL = "http://user-service";
    private static final String SPRINT_SERVICE_URL = "http://sprint-service";

    public Mono<DashboardResponse> getDashboard(UUID userId) {
        log.info("Fetching dashboard data for user: {}", userId);

        Mono<DashboardStats> statsMono = fetchDashboardStats(userId);
        Mono<List<ProjectSummary>> projectsMono = fetchRecentProjects(userId);
        Mono<TaskDistribution> taskDistributionMono = fetchTaskDistribution(userId);

        return Mono.zip(statsMono, projectsMono, taskDistributionMono)
                .map(tuple -> DashboardResponse.builder()
                        .stats(tuple.getT1())
                        .recentProjects(tuple.getT2())
                        .taskDistribution(tuple.getT3())
                        .build())
                .onErrorResume(e -> {
                    log.error("Error fetching dashboard data", e);
                    return Mono.just(DashboardResponse.builder()
                            .stats(DashboardStats.builder()
                                    .totalProjects(0)
                                    .totalTasks(0)
                                    .totalUsers(0)
                                    .activeSprints(0)
                                    .completedTasks(0)
                                    .inProgressTasks(0)
                                    .todoTasks(0)
                                    .build())
                            .recentProjects(new ArrayList<>())
                            .taskDistribution(TaskDistribution.builder()
                                    .todo(0)
                                    .inProgress(0)
                                    .done(0)
                                    .build())
                            .build());
                });
    }

    private Mono<DashboardStats> fetchDashboardStats(UUID userId) {
        Mono<Integer> totalProjects = fetchTotalProjects(userId);
        Mono<Integer> totalTasks = fetchTotalTasks();
        Mono<Integer> totalUsers = fetchTotalUsers();
        Mono<Integer> activeSprints = fetchActiveSprints();
        Mono<TaskDistribution> taskDist = fetchTaskDistribution(userId);

        return Mono.zip(totalProjects, totalTasks, totalUsers, activeSprints, taskDist)
                .map(tuple -> DashboardStats.builder()
                        .totalProjects(tuple.getT1())
                        .totalTasks(tuple.getT2())
                        .totalUsers(tuple.getT3())
                        .activeSprints(tuple.getT4())
                        .completedTasks(tuple.getT5().getDone())
                        .inProgressTasks(tuple.getT5().getInProgress())
                        .todoTasks(tuple.getT5().getTodo())
                        .build());
    }

    private Mono<Integer> fetchTotalProjects(UUID userId) {
        return webClient.get()
                .uri(PROJECT_SERVICE_URL + "/projects")
                .header("X-User-Id", userId.toString())
                .retrieve()
                .bodyToMono(String.class)
                .map(this::extractTotalElements)
                .transform(CircuitBreakerOperator.of(projectServiceCircuitBreaker))
                .onErrorResume(e -> {
                    log.error("Error fetching total projects", e);
                    return Mono.just(0);
                });
    }

    private Mono<Integer> fetchTotalTasks() {
        return webClient.get()
                .uri(TASK_SERVICE_URL + "/tasks")
                .retrieve()
                .bodyToMono(String.class)
                .map(this::extractTotalElements)
                .transform(CircuitBreakerOperator.of(taskServiceCircuitBreaker))
                .onErrorResume(e -> {
                    log.error("Error fetching total tasks", e);
                    return Mono.just(0);
                });
    }

    private Mono<Integer> fetchTotalUsers() {
        return webClient.get()
                .uri(USER_SERVICE_URL + "/users")
                .retrieve()
                .bodyToMono(String.class)
                .map(this::extractTotalElements)
                .transform(CircuitBreakerOperator.of(userServiceCircuitBreaker))
                .onErrorResume(e -> {
                    log.error("Error fetching total users", e);
                    return Mono.just(0);
                });
    }

    private Mono<Integer> fetchActiveSprints() {
        return webClient.get()
                .uri(SPRINT_SERVICE_URL + "/sprints")
                .retrieve()
                .bodyToMono(String.class)
                .map(this::extractTotalElements)
                .transform(CircuitBreakerOperator.of(sprintServiceCircuitBreaker))
                .onErrorResume(e -> {
                    log.error("Error fetching active sprints", e);
                    return Mono.just(0);
                });
    }

    private Mono<List<ProjectSummary>> fetchRecentProjects(UUID userId) {
        return webClient.get()
                .uri(PROJECT_SERVICE_URL + "/projects?size=5")
                .header("X-User-Id", userId.toString())
                .retrieve()
                .bodyToMono(String.class)
                .map(this::extractProjectSummaries)
                .transform(CircuitBreakerOperator.of(projectServiceCircuitBreaker))
                .onErrorResume(e -> {
                    log.error("Error fetching recent projects", e);
                    return Mono.just(new ArrayList<>());
                });
    }

    private Mono<TaskDistribution> fetchTaskDistribution(UUID userId) {
        return webClient.get()
                .uri(TASK_SERVICE_URL + "/tasks")
                .retrieve()
                .bodyToMono(String.class)
                .map(this::extractTaskDistribution)
                .transform(CircuitBreakerOperator.of(taskServiceCircuitBreaker))
                .onErrorResume(e -> {
                    log.error("Error fetching task distribution", e);
                    return Mono.just(TaskDistribution.builder()
                            .todo(0)
                            .inProgress(0)
                            .done(0)
                            .build());
                });
    }

    private int extractTotalElements(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode data = root.path("data");
            if (data.has("totalElements")) {
                return data.get("totalElements").asInt();
            }
            if (data.has("page")) {
                JsonNode page = data.get("page");
                if (page.has("totalElements")) {
                    return page.get("totalElements").asInt();
                }
            }
            return 0;
        } catch (Exception e) {
            log.error("Error extracting total elements from response", e);
            return 0;
        }
    }

    private List<ProjectSummary> extractProjectSummaries(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode data = root.path("data");
            JsonNode content;

            if (data.has("content")) {
                content = data.get("content");
            } else if (data.has("page") && data.get("page").has("content")) {
                content = data.get("page").get("content");
            } else {
                return new ArrayList<>();
            }

            List<ProjectSummary> summaries = new ArrayList<>();
            for (JsonNode node : content) {
                try {
                    ProjectSummary summary = ProjectSummary.builder()
                            .id(UUID.fromString(node.get("id").asText()))
                            .name(node.get("name").asText())
                            .description(node.has("description") ? node.get("description").asText() : "")
                            .memberCount(node.has("memberCount") ? node.get("memberCount").asInt() : 0)
                            .taskCount(node.has("taskStats") && node.get("taskStats").has("total")
                                    ? node.get("taskStats").get("total").asInt() : 0)
                            .createdAt(node.has("createdAt") ? node.get("createdAt").asText() : Instant.now()
                                    .toString())
                            .build();
                    summaries.add(summary);
                } catch (Exception e) {
                    log.error("Error parsing project summary", e);
                }
            }
            return summaries;
        } catch (Exception e) {
            log.error("Error extracting project summaries from response", e);
            return new ArrayList<>();
        }
    }

    private TaskDistribution extractTaskDistribution(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode data = root.path("data");
            JsonNode content;

            if (data.has("content")) {
                content = data.get("content");
            } else if (data.has("page") && data.get("page").has("content")) {
                content = data.get("page").get("content");
            } else {
                return TaskDistribution.builder().todo(0).inProgress(0).done(0).build();
            }

            int todo = 0, inProgress = 0, done = 0;
            for (JsonNode node : content) {
                String status = node.has("status") ? node.get("status").asText().toLowerCase() : "";
                switch (status) {
                    case "todo":
                        todo++;
                        break;
                    case "in_progress":
                    case "inprogress":
                        inProgress++;
                        break;
                    case "done":
                    case "completed":
                        done++;
                        break;
                }
            }
            return TaskDistribution.builder().todo(todo).inProgress(inProgress).done(done).build();
        } catch (Exception e) {
            log.error("Error extracting task distribution from response", e);
            return TaskDistribution.builder().todo(0).inProgress(0).done(0).build();
        }
    }
}
