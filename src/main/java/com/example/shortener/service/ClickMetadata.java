package com.example.shortener.service;

public record ClickMetadata(
		String ipAddress,
		String referrer,
		String userAgent) {
}