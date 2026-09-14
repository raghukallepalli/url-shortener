package com.example.shortener.web;

import java.net.URI;

import com.example.shortener.api.CreateLinkRequest;
import com.example.shortener.api.LinkResponse;
import com.example.shortener.service.ShortLinkService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/links")
public class ShortLinkController {

	private final ShortLinkService shortLinkService;

	public ShortLinkController(ShortLinkService shortLinkService) {
		this.shortLinkService = shortLinkService;
	}

	@PostMapping
	public ResponseEntity<LinkResponse> create(
			@Valid @RequestBody CreateLinkRequest request,
			@RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey) {
		LinkResponse response = shortLinkService.create(request, idempotencyKey);
		return ResponseEntity.created(URI.create(response.shortUrl())).body(response);
	}

	@GetMapping("/{code}")
	public ResponseEntity<LinkResponse> get(@PathVariable String code) {
		return ResponseEntity.ok(shortLinkService.get(code));
	}

	@DeleteMapping("/{code}")
	public ResponseEntity<Void> disable(@PathVariable String code) {
		shortLinkService.disable(code);
		return ResponseEntity.noContent().build();
	}
}