package com.vettyou.common;

import java.util.List;

public record ApiError(
		int status,
		String code,
		String message,
		List<FieldError> fieldErrors,
		String correlationId) {

	public record FieldError(String field, String message) {
	}

}
