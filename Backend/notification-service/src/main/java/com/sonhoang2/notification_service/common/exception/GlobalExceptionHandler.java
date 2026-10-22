package com.sonhoang2.notification_service.common.exception;

import com.sonhoang2.notification_service.common.dto.JSendResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<JSendResponse<Map<String, Object>>> handleNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request) {
        return fail(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<JSendResponse<Map<String, Object>>> handleMissingHeader(
            MissingRequestHeaderException ex,
            HttpServletRequest request) {
        return fail(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    private ResponseEntity<JSendResponse<Map<String, Object>>> fail(HttpStatus status,
                                                                   String message,
                                                                   HttpServletRequest request) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("timestamp", Instant.now());
        data.put("path", request.getRequestURI());
        return ResponseEntity.status(status).body(JSendResponse.fail(data, message));
    }
}
