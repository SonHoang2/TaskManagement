package com.sonhoang2.TaskManagementAPI.dto.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiSuccessResponse<T> {

	private final String status;
	private final String message;
	private final T data;
	private final Object error;
	private final Instant timestamp;
	private final String path;

	public static <T> ApiSuccessResponse<T> success(String message, T data, String path) {
		return ApiSuccessResponse.<T>builder()
				.status("SUCCESS")
				.message(message)
				.data(data)
				.timestamp(Instant.now())
				.path(path)
				.build();
	}

	public static ApiSuccessResponse<Void> fail(String message, Object error, String path) {
		return ApiSuccessResponse.<Void>builder()
				.status("FAIL")
				.message(message)
				.error(error)
				.timestamp(Instant.now())
				.path(path)
				.build();
	}

	public static ApiSuccessResponse<Void> error(String message, Object error, String path) {
		return ApiSuccessResponse.<Void>builder()
				.status("ERROR")
				.message(message)
				.error(error)
				.timestamp(Instant.now())
				.path(path)
				.build();
	}
}

