package com.example.shortener.api;

import java.time.Instant;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateLinkRequest(
		@NotBlank @Size(max = 2048) String originalUrl,
		@Pattern(regexp = "^[A-Za-z0-9_-]{4,32}$") String customAlias,
		@Future Instant expiresAt) {
}