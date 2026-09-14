package com.example.shortener.web;

import java.net.URI;

import com.example.shortener.domain.ShortLink;
import com.example.shortener.service.AnalyticsService;
import com.example.shortener.service.ClickMetadataExtractor;
import com.example.shortener.service.ShortLinkService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RedirectController {

	private final ShortLinkService shortLinkService;
	private final AnalyticsService analyticsService;
	private final ClickMetadataExtractor clickMetadataExtractor;

	public RedirectController(
			ShortLinkService shortLinkService,
			AnalyticsService analyticsService,
			ClickMetadataExtractor clickMetadataExtractor) {
		this.shortLinkService = shortLinkService;
		this.analyticsService = analyticsService;
		this.clickMetadataExtractor = clickMetadataExtractor;
	}

	@GetMapping("/{code:[A-Za-z0-9_-]{4,32}}")
	public ResponseEntity<Void> redirect(@PathVariable String code, HttpServletRequest request) {
		ShortLink link = shortLinkService.resolve(code);
		analyticsService.recordClick(link, clickMetadataExtractor.extract(request));
		return ResponseEntity.status(HttpStatus.FOUND)
				.location(URI.create(link.getOriginalUrl()))
				.header("Cache-Control", "no-store")
				.header("Pragma", "no-cache")
				.build();
	}
}