package com.vettyou.common;

import java.util.List;

public class InvalidFieldsException extends RuntimeException {

	private final List<ApiError.FieldError> fieldErrors;

	public InvalidFieldsException(String field, String message) {
		super(message);
		this.fieldErrors = List.of(new ApiError.FieldError(field, message));
	}

	public List<ApiError.FieldError> fieldErrors() {
		return fieldErrors;
	}

}
