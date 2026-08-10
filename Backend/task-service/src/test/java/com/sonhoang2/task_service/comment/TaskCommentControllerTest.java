package com.sonhoang2.task_service.comment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sonhoang2.task_service.common.config.SecurityConfig;
import com.sonhoang2.task_service.common.dto.PageResponse;
import com.sonhoang2.task_service.comment.dto.TaskCommentCreateRequest;
import com.sonhoang2.task_service.comment.dto.TaskCommentResponse;
import com.sonhoang2.task_service.comment.dto.TaskCommentUpdateRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TaskCommentController.class)
@Import(SecurityConfig.class)
class TaskCommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TaskCommentService taskCommentService;

    private UUID commentId = UUID.randomUUID();
    private UUID taskId = UUID.randomUUID();
    private UUID userId = UUID.randomUUID();

    @Test
    void create_ShouldReturnCreated_WhenValidRequest() throws Exception {
        TaskCommentCreateRequest request = new TaskCommentCreateRequest();
        request.setTaskId(taskId);
        request.setUserId(userId);
        request.setContent("Test comment");

        TaskCommentResponse response = TaskCommentResponse.builder()
                .id(commentId)
                .taskId(taskId)
                .userId(userId)
                .content("Test comment")
                .createdAt(Instant.now())
                .build();

        when(taskCommentService.create(any(TaskCommentCreateRequest.class))).thenReturn(response);

        mockMvc.perform(post("/task-comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.comment.id").value(commentId.toString()))
                .andExpect(jsonPath("$.data.comment.content").value("Test comment"));
    }

    @Test
    void findAll_ShouldReturnPageResponse_WhenCalled() throws Exception {
        TaskCommentResponse response = TaskCommentResponse.builder()
                .id(commentId)
                .taskId(taskId)
                .userId(userId)
                .content("Test comment")
                .createdAt(Instant.now())
                .build();

        PageResponse<TaskCommentResponse> pageResponse = new PageResponse<>(
                List.of(response),
                0,
                10,
                1,
                1,
                false,
                false,
                1
        );

        when(taskCommentService.findAll(any(), any(), any())).thenReturn(pageResponse);

        mockMvc.perform(get("/task-comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.page.content[0].id").value(commentId.toString()));
    }

    @Test
    void findAll_WithFilters_ShouldReturnFilteredPageResponse() throws Exception {
        TaskCommentResponse response = TaskCommentResponse.builder()
                .id(commentId)
                .taskId(taskId)
                .userId(userId)
                .content("Test comment")
                .createdAt(Instant.now())
                .build();

        PageResponse<TaskCommentResponse> pageResponse = new PageResponse<>(
                List.of(response),
                0,
                10,
                1,
                1,
                false,
                false,
                1
        );

        when(taskCommentService.findAll(eq(taskId), eq(userId), any())).thenReturn(pageResponse);

        mockMvc.perform(get("/task-comments")
                        .param("taskId", taskId.toString())
                        .param("userId", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    void findById_ShouldReturnCommentResponse_WhenCommentExists() throws Exception {
        TaskCommentResponse response = TaskCommentResponse.builder()
                .id(commentId)
                .taskId(taskId)
                .userId(userId)
                .content("Test comment")
                .createdAt(Instant.now())
                .build();

        when(taskCommentService.findById(commentId)).thenReturn(response);

        mockMvc.perform(get("/task-comments/{id}", commentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.comment.id").value(commentId.toString()))
                .andExpect(jsonPath("$.data.comment.content").value("Test comment"));
    }

    @Test
    void update_ShouldReturnUpdatedCommentResponse_WhenValidRequest() throws Exception {
        TaskCommentUpdateRequest request = new TaskCommentUpdateRequest();
        request.setContent("Updated comment");

        TaskCommentResponse response = TaskCommentResponse.builder()
                .id(commentId)
                .taskId(taskId)
                .userId(userId)
                .content("Updated comment")
                .createdAt(Instant.now())
                .build();

        when(taskCommentService.update(eq(commentId), any(TaskCommentUpdateRequest.class))).thenReturn(response);

        mockMvc.perform(patch("/task-comments/{id}", commentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.comment.id").value(commentId.toString()))
                .andExpect(jsonPath("$.data.comment.content").value("Updated comment"));
    }

    @Test
    void delete_ShouldReturnSuccess_WhenCommentExists() throws Exception {
        mockMvc.perform(delete("/task-comments/{id}", commentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
    }
}
