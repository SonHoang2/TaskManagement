package com.sonhoang2.notification_service.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record JSendResponse<T>(
        String status,
        T data,
        String message,
        Integer code
) {

    public static <T> JSendResponse<T> success(T data) {
        return new JSendResponse<>("success", data, null, null);
    }

    public static <T> JSendResponse<T> fail(T data, String message) {
        return new JSendResponse<>("fail", data, message, null);
    }

    public static <T> JSendResponse<T> error(String message, Integer code, T data) {
        return new JSendResponse<>("error", data, message, code);
    }
}

