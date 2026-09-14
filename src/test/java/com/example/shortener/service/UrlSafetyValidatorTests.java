package com.example.shortener.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class UrlSafetyValidatorTests {

	private final UrlSafetyValidator validator = new UrlSafetyValidator();

	@Test
	void normalizesPublicHttpUrl() {
		String input = " HTTPS://b" + (char) 252 + "cher.example/a/../path?q=value#section ";

		String normalized = validator.validateAndNormalize(input);

		assertEquals("https://xn--bcher-kva.example/path?q=value#section", normalized);
	}

	@Test
	void rejectsMalformedOrPrivateUrls() {
		assertThrows(UrlValidationException.class, () -> validator.validateAndNormalize("not a URL"));
		assertThrows(UrlValidationException.class, () -> validator.validateAndNormalize("ftp://example.com"));
		assertThrows(UrlValidationException.class, () -> validator.validateAndNormalize("http://user:password@example.com"));
		assertThrows(UrlValidationException.class, () -> validator.validateAndNormalize("http://localhost"));
		assertThrows(UrlValidationException.class, () -> validator.validateAndNormalize("http://127.0.0.1"));
		assertThrows(UrlValidationException.class, () -> validator.validateAndNormalize("http://10.0.0.1"));
		assertThrows(UrlValidationException.class, () -> validator.validateAndNormalize("http://172.16.0.1"));
		assertThrows(UrlValidationException.class, () -> validator.validateAndNormalize("http://192.168.0.1"));
	}
}