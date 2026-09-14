package com.example.shortener.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class ClickMetadataExtractor {

	public ClickMetadata extract(HttpServletRequest request) {
		return new ClickMetadata(
				request.getRemoteAddr(),
				request.getHeader("Referer"),
				request.getHeader("User-Agent"));
	}
}