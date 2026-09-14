package com.example.shortener.api;

import java.time.Instant;

public record ErrorResponse(
		String code,
		String message,
		String traceId,
		Instant timestamp) {
}