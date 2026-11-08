package com.vettyou.common;

import java.util.List;

public final class ApiErrors {

	private ApiErrors() {
	}

	public static ApiError of(int status, String code, String message, List<ApiError.FieldError> fieldErrors) {
		List<ApiError.FieldError> errors = fieldErrors == null ? List.of() : List.copyOf(fieldErrors);
		return new ApiError(status, code, message, errors, CorrelationIds.current());
	}

	public static String codeFor(int status) {
		return switch (status) {
			case 400 -> "MALFORMED_REQUEST";
			case 401 -> "UNAUTHENTICATED";
			case 403 -> "FORBIDDEN";
			case 404 -> "NOT_FOUND";
			case 405 -> "METHOD_NOT_ALLOWED";
			case 409 -> "CONFLICT";
			case 422 -> "VALIDATION_FAILED";
			case 429 -> "RATE_LIMITED";
			default -> status >= 500 ? "INTERNAL_ERROR" : "REQUEST_FAILED";
		};
	}

	public static String messageFor(int status) {
		return switch (status) {
			case 400 -> "Request is malformed";
			case 401 -> "Authentication is required";
			case 403 -> "You are not allowed to perform this action";
			case 404 -> "Resource was not found";
			case 405 -> "HTTP method is not supported";
			case 409 -> "Request conflicts with the current state";
			case 422 -> "Request has invalid fields";
			case 429 -> "Too many requests";
			default -> status >= 500 ? "Unexpected error" : "Request failed";
		};
	}

}
