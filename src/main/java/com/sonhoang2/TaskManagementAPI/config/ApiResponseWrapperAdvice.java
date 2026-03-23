package com.sonhoang2.TaskManagementAPI.config;

import com.sonhoang2.TaskManagementAPI.dto.common.ApiSuccessResponse;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@RestControllerAdvice
public class ApiResponseWrapperAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(
            Object body,
            MethodParameter returnType,
            MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType,
            ServerHttpRequest request,
            ServerHttpResponse response) {

        if (body instanceof ApiSuccessResponse<?>) {
            return body;
        }

        if (!(request instanceof ServletServerHttpRequest servletRequest)
                || !(response instanceof ServletServerHttpResponse servletResponse)) {
            return body;
        }

        int httpStatus = servletResponse.getServletResponse().getStatus();
        if (httpStatus >= 400) {
            return body;
        }

        String message = resolveMessage(httpStatus, servletRequest.getServletRequest().getMethod());
        String path = servletRequest.getServletRequest().getRequestURI();

        return ApiSuccessResponse.success(message, body, path);
    }

    private String resolveMessage(int status, String method) {
        if (status == 201) {
            return "Created successfully";
        }

        if ("DELETE".equalsIgnoreCase(method)) {
            return "Deleted successfully";
        }

        if ("PUT".equalsIgnoreCase(method) || "PATCH".equalsIgnoreCase(method)) {
            return "Updated successfully";
        }

        return "Success";
    }
}

