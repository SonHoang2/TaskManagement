package com.sonhoang2.task_service.attachment.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TaskAttachmentUpdateRequest {

    @Size(max = 500, message = "fileUrl length must be <= 500")
    private String fileUrl;

    @Size(max = 255, message = "fileName length must be <= 255")
    private String fileName;
}



