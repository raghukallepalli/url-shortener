package com.example.shortener.domain;

import java.time.Instant;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "short_links")
public class ShortLink {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 32)
	private String code;

	@Column(name = "original_url", nullable = false, length = 2048)
	private String originalUrl;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "expires_at")
	private Instant expiresAt;

	@Column(nullable = false)
	private boolean active;

	@Column(name = "idempotency_key", unique = true, length = 128)
	private String idempotencyKey;

	@Version
	@Column(nullable = false)
	private Long version;

	protected ShortLink() {
	}

	public ShortLink(
			String code,
			String originalUrl,
			Instant createdAt,
			Instant expiresAt,
			boolean active,
			String idempotencyKey) {
		this.code = code;
		this.originalUrl = originalUrl;
		this.createdAt = createdAt;
		this.expiresAt = expiresAt;
		this.active = active;
		this.idempotencyKey = idempotencyKey;
	}

	public Long getId() {
		return id;
	}

	public String getCode() {
		return code;
	}

	public String getOriginalUrl() {
		return originalUrl;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getExpiresAt() {
		return expiresAt;
	}

	public boolean isActive() {
		return active;
	}

	public String getIdempotencyKey() {
		return idempotencyKey;
	}

	public Long getVersion() {
		return version;
	}

	public boolean isExpiredAt(Instant now) {
		return expiresAt != null && !expiresAt.isAfter(Objects.requireNonNull(now, "now must not be null"));
	}

	public void disable() {
		active = false;
	}
}
