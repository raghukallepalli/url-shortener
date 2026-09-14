package com.example.shortener.config;

import java.net.URI;
import java.time.Duration;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "shortener")
public record ShortenerProperties(
		URI baseUrl,
		@Min(6) @Max(16) int codeLength,
		@Min(1) @Max(10) int collisionRetries,
		@NotNull Duration defaultTtl) {
}