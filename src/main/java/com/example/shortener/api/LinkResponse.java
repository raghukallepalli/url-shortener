package com.example.shortener.api;

import java.time.Instant;

public record LinkResponse(
		String code,
		String shortUrl,
		String originalUrl,
		Instant createdAt,
		Instant expiresAt) {
}