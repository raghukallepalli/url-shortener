package com.example.shortener.web;

import java.net.URI;

import com.example.shortener.domain.ShortLink;
import com.example.shortener.service.ShortLinkService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RedirectController {

	private final ShortLinkService shortLinkService;

	public RedirectController(ShortLinkService shortLinkService) {
		this.shortLinkService = shortLinkService;
	}

	@GetMapping("/{code:[A-Za-z0-9_-]{4,32}}")
	public ResponseEntity<Void> redirect(@PathVariable String code) {
		ShortLink link = shortLinkService.resolve(code);
		return ResponseEntity.status(HttpStatus.FOUND)
				.location(URI.create(link.getOriginalUrl()))
				.header("Cache-Control", "no-store")
				.header("Pragma", "no-cache")
				.build();
	}
}