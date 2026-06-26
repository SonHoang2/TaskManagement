package com.sonhoang2.task_service.task;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sonhoang2.task_service.common.dto.JSendResponse;
import com.sonhoang2.task_service.common.dto.PageResponse;
import com.sonhoang2.task_service.task.dto.TaskCreateRequest;
import com.sonhoang2.task_service.task.dto.TaskResponse;
import com.sonhoang2.task_service.task.dto.TaskUpdateRequest;
import com.sonhoang2.task_service.task.entity.TaskPriority;
import com.sonhoang2.task_service.task.entity.TaskStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TaskController.class)
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TaskService taskService;

    private UUID taskId = UUID.randomUUID();
    private UUID projectId = UUID.randomUUID();

    @Test
    void create_ShouldReturnCreated_WhenValidRequest() throws Exception {
        TaskCreateRequest request = new TaskCreateRequest();
        request.setProjectId(projectId);
        request.setTitle("Test Task");
        request.setStatus(TaskStatus.TODO);
        request.setPriority(TaskPriority.HIGH);

        TaskResponse response = TaskResponse.builder()
                .id(taskId)
                .projectId(projectId)
                .title("Test Task")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.HIGH)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(taskService.create(any(TaskCreateRequest.class))).thenReturn(response);

        mockMvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.task.id").value(taskId.toString()))
                .andExpect(jsonPath("$.data.task.title").value("Test Task"));
    }

    @Test
    void findAll_ShouldReturnPageResponse_WhenCalled() throws Exception {
        TaskResponse response = TaskResponse.builder()
                .id(taskId)
                .projectId(projectId)
                .title("Test Task")
                .status(TaskStatus.TODO)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        PageResponse<TaskResponse> pageResponse = new PageResponse<>(
                List.of(response),
                0,
                10,
                1,
                1,
                false,
                false,
                1
        );

        when(taskService.findAll(any(), any(), any(Pageable.class))).thenReturn(pageResponse);

        mockMvc.perform(get("/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.page.data[0].id").value(taskId.toString()));
    }

    @Test
    void findAll_WithFilters_ShouldReturnFilteredPageResponse() throws Exception {
        TaskResponse response = TaskResponse.builder()
                .id(taskId)
                .projectId(projectId)
                .title("Test Task")
                .status(TaskStatus.TODO)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        PageResponse<TaskResponse> pageResponse = new PageResponse<>(
                List.of(response),
                0,
                10,
                1,
                1,
                false,
                false,
                1
        );

        when(taskService.findAll(eq("TODO"), eq("Test"), any(Pageable.class))).thenReturn(pageResponse);

        mockMvc.perform(get("/tasks")
                        .param("status", "TODO")
                        .param("keyword", "Test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    void findById_ShouldReturnTaskResponse_WhenTaskExists() throws Exception {
        TaskResponse response = TaskResponse.builder()
                .id(taskId)
                .projectId(projectId)
                .title("Test Task")
                .status(TaskStatus.TODO)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(taskService.findById(taskId)).thenReturn(response);

        mockMvc.perform(get("/tasks/{id}", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.task.id").value(taskId.toString()))
                .andExpect(jsonPath("$.data.task.title").value("Test Task"));
    }

    @Test
    void update_ShouldReturnUpdatedTaskResponse_WhenValidRequest() throws Exception {
        TaskUpdateRequest request = new TaskUpdateRequest();
        request.setTitle("Updated Task");
        request.setStatus(TaskStatus.IN_PROGRESS);

        TaskResponse response = TaskResponse.builder()
                .id(taskId)
                .projectId(projectId)
                .title("Updated Task")
                .status(TaskStatus.IN_PROGRESS)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(taskService.update(eq(taskId), any(TaskUpdateRequest.class))).thenReturn(response);

        mockMvc.perform(patch("/tasks/{id}", taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.task.id").value(taskId.toString()))
                .andExpect(jsonPath("$.data.task.title").value("Updated Task"));
    }

    @Test
    void delete_ShouldReturnSuccess_WhenTaskExists() throws Exception {
        mockMvc.perform(delete("/tasks/{id}", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
    }
}
