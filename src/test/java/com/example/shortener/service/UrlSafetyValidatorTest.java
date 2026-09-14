package com.example.shortener.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UrlSafetyValidatorTest {

	private final UrlSafetyValidator validator = new UrlSafetyValidator();

	@ParameterizedTest
	@CsvSource({
			"https://example.com,https://example.com",
			"HTTP://Example.COM/path,http://example.com/path",
			"https://example.com/a/../b?q=1#section,https://example.com/b?q=1#section"
	})
	void validatesAndNormalizesPublicUrls(String input, String expected) {
		assertEquals(expected, validator.validateAndNormalize(input));
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"javascript:alert(1)",
			"file:///etc/passwd",
			"http://localhost/admin",
			"http://127.0.0.1/admin",
			"http://10.1.2.3/admin",
			"http://192.168.1.10/admin",
			"https://user:password@example.com",
			"a relative URL"
	})
	void rejectsUnsafeOrMalformedUrls(String input) {
		assertThrows(UrlValidationException.class, () -> validator.validateAndNormalize(input));
	}
}