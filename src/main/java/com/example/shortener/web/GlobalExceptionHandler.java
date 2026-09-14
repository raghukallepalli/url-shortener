package com.example.shortener.web;

import java.time.Clock;
import java.util.stream.Collectors;

import com.example.shortener.api.ErrorResponse;
import com.example.shortener.exception.AliasAlreadyExistsException;
import com.example.shortener.exception.CodeGenerationException;
import com.example.shortener.exception.IdempotencyConflictException;
import com.example.shortener.exception.LinkDisabledException;
import com.example.shortener.exception.LinkExpiredException;
import com.example.shortener.exception.LinkNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	private final Clock clock;

	public GlobalExceptionHandler(Clock clock) {
		this.clock = clock;
	}

	@ExceptionHandler(LinkNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleLinkNotFound(
			LinkNotFoundException exception,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(errorResponse("LINK_NOT_FOUND", exception.getMessage(), request));
	}

	@ExceptionHandler(LinkExpiredException.class)
	public ResponseEntity<ErrorResponse> handleLinkExpired(
			LinkExpiredException exception,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.GONE)
				.body(errorResponse("LINK_EXPIRED", exception.getMessage(), request));
	}

	@ExceptionHandler(LinkDisabledException.class)
	public ResponseEntity<ErrorResponse> handleLinkDisabled(
			LinkDisabledException exception,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.GONE)
				.body(errorResponse("LINK_DISABLED", exception.getMessage(), request));
	}

	@ExceptionHandler(AliasAlreadyExistsException.class)
	public ResponseEntity<ErrorResponse> handleAliasAlreadyExists(
			AliasAlreadyExistsException exception,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(errorResponse("ALIAS_TAKEN", exception.getMessage(), request));
	}

	@ExceptionHandler(IdempotencyConflictException.class)
	public ResponseEntity<ErrorResponse> handleIdempotencyConflict(
			IdempotencyConflictException exception,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(errorResponse("IDEMPOTENCY_CONFLICT", exception.getMessage(), request));
	}

	@ExceptionHandler(CodeGenerationException.class)
	public ResponseEntity<ErrorResponse> handleCodeGeneration(
			CodeGenerationException exception,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
				.body(errorResponse("CODE_GENERATION_FAILED", exception.getMessage(), request));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
			MethodArgumentNotValidException exception,
			HttpServletRequest request) {
		return ResponseEntity.badRequest()
				.body(errorResponse("VALIDATION_FAILED", validationMessage(exception), request));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(HttpServletRequest request) {
		return ResponseEntity.badRequest()
				.body(errorResponse("INVALID_REQUEST", "Request body is invalid", request));
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ErrorResponse> handleConstraintViolation(
			ConstraintViolationException exception,
			HttpServletRequest request) {
		return ResponseEntity.badRequest()
				.body(errorResponse("VALIDATION_FAILED", validationMessage(exception), request));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleUnexpectedException(Exception exception, HttpServletRequest request) {
		String traceId = (String) request.getAttribute("traceId");
		LOGGER.error("Unexpected request failure for traceId={}", traceId, exception);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(errorResponse("INTERNAL_ERROR", "An unexpected error occurred", request));
	}

	private ErrorResponse errorResponse(String code, String message, HttpServletRequest request) {
		return new ErrorResponse(code, message, (String) request.getAttribute("traceId"), clock.instant());
	}

	private String validationMessage(MethodArgumentNotValidException exception) {
		return exception.getBindingResult().getFieldErrors().stream()
				.map(error -> error.getField() + " " + error.getDefaultMessage())
				.collect(Collectors.joining(", "));
	}

	private String validationMessage(ConstraintViolationException exception) {
		return exception.getConstraintViolations().stream()
				.map(violation -> violation.getPropertyPath() + " " + violation.getMessage())
				.collect(Collectors.joining(", "));
	}
}