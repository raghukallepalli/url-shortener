package com.example.shortener.service;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

import com.example.shortener.api.CreateLinkRequest;
import com.example.shortener.api.LinkResponse;
import com.example.shortener.config.ShortenerProperties;
import com.example.shortener.domain.ShortLink;
import com.example.shortener.exception.AliasAlreadyExistsException;
import com.example.shortener.exception.CodeGenerationException;
import com.example.shortener.exception.IdempotencyConflictException;
import com.example.shortener.exception.LinkDisabledException;
import com.example.shortener.exception.LinkExpiredException;
import com.example.shortener.exception.LinkNotFoundException;
import com.example.shortener.repository.ShortLinkRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShortLinkService {

	private final ShortLinkRepository shortLinkRepository;
	private final ShortLinkWriter shortLinkWriter;
	private final CodeGenerator codeGenerator;
	private final UrlSafetyValidator urlSafetyValidator;
	private final ShortenerProperties shortenerProperties;
	private final Clock clock;

	public ShortLinkService(
			ShortLinkRepository shortLinkRepository,
			ShortLinkWriter shortLinkWriter,
			CodeGenerator codeGenerator,
			UrlSafetyValidator urlSafetyValidator,
			ShortenerProperties shortenerProperties,
			Clock clock) {
		this.shortLinkRepository = shortLinkRepository;
		this.shortLinkWriter = shortLinkWriter;
		this.codeGenerator = codeGenerator;
		this.urlSafetyValidator = urlSafetyValidator;
		this.shortenerProperties = shortenerProperties;
		this.clock = clock;
	}

	public LinkResponse create(CreateLinkRequest request, String idempotencyKey) {
		Objects.requireNonNull(request, "request must not be null");
		String originalUrl = urlSafetyValidator.validateAndNormalize(request.originalUrl());
		String normalizedIdempotencyKey = normalizeIdempotencyKey(idempotencyKey);

		if (normalizedIdempotencyKey != null) {
			var existingLink = shortLinkRepository.findByIdempotencyKey(normalizedIdempotencyKey);
			if (existingLink.isPresent()) {
				return responseForIdempotencyKey(existingLink.get(), originalUrl);
			}
		}

		Instant now = clock.instant();
		Instant expiresAt = request.expiresAt() == null
				? now.plus(shortenerProperties.defaultTtl())
				: request.expiresAt();
		boolean hasCustomAlias = request.customAlias() != null && !request.customAlias().isBlank();

		for (int attempt = 0; attempt < shortenerProperties.collisionRetries(); attempt++) {
			String code = hasCustomAlias ? request.customAlias() : codeGenerator.generate(shortenerProperties.codeLength());
			ShortLink link = new ShortLink(code, originalUrl, now, expiresAt, true, normalizedIdempotencyKey);

			try {
				return toResponse(shortLinkWriter.insert(link));
			} catch (DataIntegrityViolationException exception) {
				if (normalizedIdempotencyKey != null) {
					var existingLink = shortLinkRepository.findByIdempotencyKey(normalizedIdempotencyKey);
					if (existingLink.isPresent()) {
						return responseForIdempotencyKey(existingLink.get(), originalUrl);
					}
				}
				if (shortLinkRepository.existsByCode(code)) {
					if (hasCustomAlias) {
						throw new AliasAlreadyExistsException("Custom alias is already in use: " + code);
					}
					continue;
				}
				throw exception;
			}
		}

		throw new CodeGenerationException("Could not generate a unique short-link code");
	}

	public ShortLink resolve(String code) {
		ShortLink link = requireExisting(code);
		if (!link.isActive()) {
			throw new LinkDisabledException("Short link is disabled: " + code);
		}
		if (link.isExpiredAt(clock.instant())) {
			throw new LinkExpiredException("Short link has expired: " + code);
		}
		return link;
	}

	@Transactional(readOnly = true)
	public ShortLink requireExisting(String code) {
		return shortLinkRepository.findByCode(code)
				.orElseThrow(() -> new LinkNotFoundException(code));
	}

	public LinkResponse get(String code) {
		return toResponse(resolve(code));
	}

	@Transactional
	public void disable(String code) {
		ShortLink link = shortLinkRepository.findByCode(code)
				.orElseThrow(() -> new LinkNotFoundException("Short link was not found: " + code));
		link.disable();
	}

	private String normalizeIdempotencyKey(String idempotencyKey) {
		if (idempotencyKey == null || idempotencyKey.isBlank()) {
			return null;
		}
		if (idempotencyKey.length() > 128) {
			throw new IllegalArgumentException("Idempotency key must not exceed 128 characters");
		}
		return idempotencyKey;
	}

	private LinkResponse responseForIdempotencyKey(ShortLink link, String originalUrl) {
		if (!link.getOriginalUrl().equals(originalUrl)) {
			throw new IdempotencyConflictException("Idempotency key was used for a different URL");
		}
		return toResponse(link);
	}

	private LinkResponse toResponse(ShortLink link) {
		String baseUrl = shortenerProperties.baseUrl().toString();
		String shortUrl = (baseUrl.endsWith("/") ? baseUrl : baseUrl + "/") + link.getCode();
		return new LinkResponse(
				link.getCode(),
				shortUrl,
				link.getOriginalUrl(),
				link.getCreatedAt(),
				link.getExpiresAt());
	}
}