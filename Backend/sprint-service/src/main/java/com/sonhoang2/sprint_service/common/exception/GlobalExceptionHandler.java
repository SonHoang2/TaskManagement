package com.sonhoang2.sprint_service.common.exception;

import com.sonhoang2.sprint_service.common.dto.JSendResponse;
import feign.FeignException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<JSendResponse<Map<String, Object>>> handleAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest request) {
        return fail(HttpStatus.FORBIDDEN, ex.getMessage(), request);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<JSendResponse<Map<String, Object>>> handleNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request) {
        return fail(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(ResourceConflictException.class)
    public ResponseEntity<JSendResponse<Map<String, Object>>> handleConflict(
            ResourceConflictException ex,
            HttpServletRequest request) {
        return fail(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    /** Relays 4xx answers from other services (e.g. task-service refusing access); anything else is a 503. */
    @ExceptionHandler(FeignException.class)
    public ResponseEntity<JSendResponse<Map<String, Object>>> handleFeign(
            FeignException ex,
            HttpServletRequest request) {
        HttpStatus status = HttpStatus.resolve(ex.status());
        if (status == null || !status.is4xxClientError()) {
            return fail(HttpStatus.SERVICE_UNAVAILABLE, "Upstream service unavailable", request);
        }
        return fail(status, status.getReasonPhrase(), request);
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
