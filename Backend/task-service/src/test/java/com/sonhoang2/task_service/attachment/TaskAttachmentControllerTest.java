package com.sonhoang2.task_service.attachment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sonhoang2.task_service.attachment.dto.TaskAttachmentCreateRequest;
import com.sonhoang2.task_service.attachment.dto.TaskAttachmentResponse;
import com.sonhoang2.task_service.attachment.dto.TaskAttachmentUpdateRequest;
import com.sonhoang2.task_service.common.config.SecurityConfig;
import com.sonhoang2.task_service.common.dto.PageResponse;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TaskAttachmentController.class)
@Import(SecurityConfig.class)
class TaskAttachmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TaskAttachmentService taskAttachmentService;

    private final UUID attachmentId = UUID.randomUUID();
    private final UUID taskId = UUID.randomUUID();
    private final UUID uploadedBy = UUID.randomUUID();

    @Test
    void create_ShouldReturnCreated_WhenValidRequest() throws Exception {
        TaskAttachmentCreateRequest request = new TaskAttachmentCreateRequest();
        request.setTaskId(taskId);
        request.setFileUrl("https://example.com/file.pdf");
        request.setFileName("file.pdf");

        TaskAttachmentResponse response = TaskAttachmentResponse.builder()
                .id(attachmentId)
                .taskId(taskId)
                .fileUrl("https://example.com/file.pdf")
                .fileName("file.pdf")
                .uploadedBy(uploadedBy)
                .createdAt(Instant.now())
                .build();

        when(taskAttachmentService.create(any(TaskAttachmentCreateRequest.class), eq(uploadedBy))).thenReturn(response);

        mockMvc.perform(post("/api/v1/task-attachments")
                        .header("X-User-Id", uploadedBy.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.attachment.id").value(attachmentId.toString()))
                .andExpect(jsonPath("$.data.attachment.fileName").value("file.pdf"));
    }

    @Test
    void findAll_ShouldReturnPageResponse_WhenCalled() throws Exception {
        TaskAttachmentResponse response = TaskAttachmentResponse.builder()
                .id(attachmentId)
                .taskId(taskId)
                .fileUrl("https://example.com/file.pdf")
                .fileName("file.pdf")
                .uploadedBy(uploadedBy)
                .createdAt(Instant.now())
                .build();

        PageResponse<TaskAttachmentResponse> pageResponse = new PageResponse<>(
                List.of(response),
                0,
                10,
                1,
                1,
                false,
                false,
                1
        );

        when(taskAttachmentService.findAll(eq(taskId), any(), any(), eq(uploadedBy))).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/task-attachments").param("taskId", taskId.toString()).header("X-User-Id", uploadedBy.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.page.content[0].id").value(attachmentId.toString()));
    }

    @Test
    void findAll_WithFilters_ShouldReturnFilteredPageResponse() throws Exception {
        TaskAttachmentResponse response = TaskAttachmentResponse.builder()
                .id(attachmentId)
                .taskId(taskId)
                .fileUrl("https://example.com/file.pdf")
                .fileName("file.pdf")
                .uploadedBy(uploadedBy)
                .createdAt(Instant.now())
                .build();

        PageResponse<TaskAttachmentResponse> pageResponse = new PageResponse<>(
                List.of(response),
                0,
                10,
                1,
                1,
                false,
                false,
                1
        );

        when(taskAttachmentService.findAll(eq(taskId), eq(uploadedBy), any(), eq(uploadedBy))).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/task-attachments")
                        .header("X-User-Id", uploadedBy.toString())
                        .param("taskId", taskId.toString())
                        .param("uploadedBy", uploadedBy.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    void findById_ShouldReturnAttachmentResponse_WhenAttachmentExists() throws Exception {
        TaskAttachmentResponse response = TaskAttachmentResponse.builder()
                .id(attachmentId)
                .taskId(taskId)
                .fileUrl("https://example.com/file.pdf")
                .fileName("file.pdf")
                .uploadedBy(uploadedBy)
                .createdAt(Instant.now())
                .build();

        when(taskAttachmentService.findById(attachmentId, uploadedBy)).thenReturn(response);

        mockMvc.perform(get("/api/v1/task-attachments/{id}", attachmentId).header("X-User-Id", uploadedBy.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.attachment.id").value(attachmentId.toString()))
                .andExpect(jsonPath("$.data.attachment.fileName").value("file.pdf"));
    }

    @Test
    void update_ShouldReturnUpdatedAttachmentResponse_WhenValidRequest() throws Exception {
        TaskAttachmentUpdateRequest request = new TaskAttachmentUpdateRequest();
        request.setFileName("updated-file.pdf");

        TaskAttachmentResponse response = TaskAttachmentResponse.builder()
                .id(attachmentId)
                .taskId(taskId)
                .fileUrl("https://example.com/file.pdf")
                .fileName("updated-file.pdf")
                .uploadedBy(uploadedBy)
                .createdAt(Instant.now())
                .build();

        when(taskAttachmentService.update(eq(attachmentId),
                any(TaskAttachmentUpdateRequest.class), eq(uploadedBy))).thenReturn(response);

        mockMvc.perform(patch("/api/v1/task-attachments/{id}", attachmentId)
                        .header("X-User-Id", uploadedBy.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.attachment.id").value(attachmentId.toString()))
                .andExpect(jsonPath("$.data.attachment.fileName").value("updated-file.pdf"));
    }

    @Test
    void delete_ShouldReturnSuccess_WhenAttachmentExists() throws Exception {
        mockMvc.perform(delete("/api/v1/task-attachments/{id}", attachmentId).header("X-User-Id", uploadedBy.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        verify(taskAttachmentService).delete(attachmentId, uploadedBy);
    }
}
