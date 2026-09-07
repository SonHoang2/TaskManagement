package com.sonhoang2.task_service.common.exception;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sonhoang2.common.dto.JSendResponse;
import feign.FeignException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class FeignExceptionHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<JSendResponse<Map<String, Object>>> handleFeignException(
            FeignException ex,
            HttpServletRequest request) {

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("timestamp", Instant.now());
        data.put("path", request.getRequestURI());

        HttpStatus status = HttpStatus.valueOf(ex.status());
        String message = ex.getMessage();

        String content = ex.contentUTF8();
        if (content != null && !content.isEmpty()) {
            try {
                JsonNode jsonNode = objectMapper.readTree(content);
                JsonNode messageNode = jsonNode.path("message");
                if (!messageNode.isMissingNode() && !messageNode.isNull()) {
                    message = messageNode.asText();
                }
            } catch (Exception e) {
                // If parsing fails, try to extract message using regex
                try {
                    java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\"message\"\\s*:\\s*\"([^\"]+)\"");
                    java.util.regex.Matcher matcher = pattern.matcher(content);
                    if (matcher.find()) {
                        message = matcher.group(1);
                    }
                } catch (Exception ignored) {
                    // If regex fails, use the original content
                }
            }
        }

        return ResponseEntity.status(status).body(JSendResponse.fail(data, message));
    }
}
