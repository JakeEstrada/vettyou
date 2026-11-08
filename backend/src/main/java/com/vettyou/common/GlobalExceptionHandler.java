package com.vettyou.common;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import com.vettyou.auth.DuplicateEmailException;
import com.vettyou.mail.MailNotReadyException;

import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ApiError> validation(MethodArgumentNotValidException ex) {
		List<ApiError.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
			.map(error -> new ApiError.FieldError(error.getField(), error.getDefaultMessage()))
			.toList();
		return unprocessable(fieldErrors);
	}

	@ExceptionHandler(HandlerMethodValidationException.class)
	ResponseEntity<ApiError> methodValidation(HandlerMethodValidationException ex) {
		List<ApiError.FieldError> fieldErrors = ex.getParameterValidationResults().stream()
			.flatMap(result -> result.getResolvableErrors().stream()
				.map(error -> new ApiError.FieldError(result.getMethodParameter().getParameterName(),
						error.getDefaultMessage())))
			.toList();
		return unprocessable(fieldErrors);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	ResponseEntity<ApiError> constraintViolation(ConstraintViolationException ex) {
		List<ApiError.FieldError> fieldErrors = ex.getConstraintViolations().stream()
			.map(violation -> new ApiError.FieldError(violation.getPropertyPath().toString(), violation.getMessage()))
			.toList();
		return unprocessable(fieldErrors);
	}

	@ExceptionHandler(DuplicateEmailException.class)
	ResponseEntity<ApiError> duplicateEmail(DuplicateEmailException ex) {
		return ResponseEntity.status(409).body(ApiErrors.of(409, "EMAIL_TAKEN", ex.getMessage(), List.of()));
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	ResponseEntity<ApiError> conflict(DataIntegrityViolationException ex) {
		return ResponseEntity.status(409)
			.body(ApiErrors.of(409, "EMAIL_TAKEN", "An account with this email already exists", List.of()));
	}

	@ExceptionHandler(AuthenticationException.class)
	ResponseEntity<ApiError> authentication(AuthenticationException ex) {
		return ResponseEntity.status(401)
			.body(ApiErrors.of(401, "UNAUTHENTICATED", "Email or password is incorrect", List.of()));
	}

	@ExceptionHandler(InvalidFieldsException.class)
	ResponseEntity<ApiError> invalidFields(InvalidFieldsException ex) {
		return unprocessable(ex.fieldErrors());
	}

	@ExceptionHandler(MailNotReadyException.class)
	ResponseEntity<ApiError> mailNotReady(MailNotReadyException ex) {
		return ResponseEntity.status(503).body(ApiErrors.of(503, "MAIL_NOT_READY", ex.getMessage(), List.of()));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	ResponseEntity<ApiError> malformed(HttpMessageNotReadableException ex) {
		return ResponseEntity.badRequest()
			.body(ApiErrors.of(400, "MALFORMED_REQUEST", "Request body is malformed", List.of()));
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<ApiError> unexpected(Exception ex) {
		if (ex instanceof org.springframework.web.ErrorResponse errorResponse) {
			int status = errorResponse.getStatusCode().value();
			if (status < 500) {
				String detail = errorResponse.getBody().getDetail();
				String message = detail == null || detail.isBlank() ? ApiErrors.messageFor(status) : detail;
				return ResponseEntity.status(status).body(ApiErrors.of(status, ApiErrors.codeFor(status), message, List.of()));
			}
		}
		log.error("Unhandled error correlationId={}", CorrelationIds.current(), ex);
		return ResponseEntity.internalServerError()
			.body(ApiErrors.of(500, "INTERNAL_ERROR", "Unexpected error", List.of()));
	}

	private static ResponseEntity<ApiError> unprocessable(List<ApiError.FieldError> fieldErrors) {
		return ResponseEntity.unprocessableContent()
			.body(ApiErrors.of(422, "VALIDATION_FAILED", "Request has invalid fields", fieldErrors));
	}

}
