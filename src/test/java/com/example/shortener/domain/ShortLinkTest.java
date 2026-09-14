package com.example.shortener.domain;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class ShortLinkTest {

	private static final Instant NOW = Instant.parse("2026-09-14T12:00:00Z");

	@Test
	void linkWithoutExpirationIsNotExpired() {
		assertFalse(linkWithExpiration(null).isExpiredAt(NOW));
	}

	@Test
	void linkWithFutureExpirationIsNotExpired() {
		assertFalse(linkWithExpiration(NOW.plusSeconds(1)).isExpiredAt(NOW));
	}

	@Test
	void linkWithExpirationEqualToNowIsExpired() {
		assertTrue(linkWithExpiration(NOW).isExpiredAt(NOW));
	}

	@Test
	void linkWithPastExpirationIsExpired() {
		assertTrue(linkWithExpiration(NOW.minusSeconds(1)).isExpiredAt(NOW));
	}

	@Test
	void disableChangesActiveLinkToInactive() {
		ShortLink link = linkWithExpiration(null);

		assertTrue(link.isActive());
		link.disable();
		assertFalse(link.isActive());
	}

	private ShortLink linkWithExpiration(Instant expiresAt) {
		return new ShortLink(
				"abcd1234",
				"https://example.com",
				NOW,
				expiresAt,
				true,
				null);
	}
}