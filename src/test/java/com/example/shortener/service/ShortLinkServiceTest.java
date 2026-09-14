package com.example.shortener.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

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
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.dao.DataIntegrityViolationException;

class ShortLinkServiceTest {

	private static final Instant NOW = Instant.parse("2026-09-14T12:00:00Z");
	private static final Duration DEFAULT_TTL = Duration.ofDays(365);
	private static final ShortenerProperties PROPERTIES = new ShortenerProperties(
			URI.create("http://localhost:8080"),
			8,
			2,
			DEFAULT_TTL,
			new ShortenerProperties.Analytics("test-secret", Duration.ofDays(30), Duration.ofDays(90)));

	@Test
	void createsGeneratedCode() {
		ServiceContext context = context();
		when(context.generator().generate(8)).thenReturn("generated");
		persistInput(context);

		LinkResponse response = context.service().create(request(null, null), null);

		assertEquals("generated", response.code());
		verify(context.generator(), times(1)).generate(8);
		verify(context.writer(), times(1)).insert(any(ShortLink.class));
	}

	@Test
	void createsCustomAlias() {
		ServiceContext context = context();
		persistInput(context);

		LinkResponse response = context.service().create(request("custom-code", null), null);

		assertEquals("custom-code", response.code());
		verify(context.generator(), never()).generate(8);
		verify(context.writer(), times(1)).insert(any(ShortLink.class));
	}

	@Test
	void usesDefaultExpirationFromFixedClock() {
		ServiceContext context = context();
		when(context.generator().generate(8)).thenReturn("generated");
		persistInput(context);

		context.service().create(request(null, null), null);

		assertEquals(NOW.plus(DEFAULT_TTL), capturedLink(context).getExpiresAt());
		verify(context.generator(), times(1)).generate(8);
	}

	@Test
	void preservesExplicitExpiration() {
		ServiceContext context = context();
		Instant expiresAt = NOW.plus(Duration.ofHours(2));
		when(context.generator().generate(8)).thenReturn("generated");
		persistInput(context);

		context.service().create(request(null, expiresAt), null);

		assertEquals(expiresAt, capturedLink(context).getExpiresAt());
		verify(context.generator(), times(1)).generate(8);
	}

	@Test
	void returnsExistingLinkForSameNormalizedIdempotencyKey() {
		ServiceContext context = context();
		when(context.repository().findByIdempotencyKey("key"))
				.thenReturn(Optional.of(link("existing", "https://example.com", NOW.plus(DEFAULT_TTL), true)));

		LinkResponse response = context.service().create(request(null, null), "key");

		assertEquals("existing", response.code());
		verifyNoInteractions(context.writer(), context.generator());
	}

	@Test
	void throwsForIdempotencyKeyUsedWithDifferentUrl() {
		ServiceContext context = context();
		when(context.repository().findByIdempotencyKey("key"))
				.thenReturn(Optional.of(link("existing", "https://example.com", NOW.plus(DEFAULT_TTL), true)));

		assertThrows(IdempotencyConflictException.class, () -> context.service()
				.create(new CreateLinkRequest("https://different.example", null, null), "key"));

		verifyNoInteractions(context.writer(), context.generator());
	}

	@Test
	void retriesGeneratedCodeCollisionWithDifferentCode() {
		ServiceContext context = context();
		when(context.generator().generate(8)).thenReturn("COLLIDE1", "SUCCESS2");
		when(context.writer().insert(any(ShortLink.class)))
				.thenThrow(new DataIntegrityViolationException("collision"))
				.thenAnswer(invocation -> invocation.getArgument(0));
		when(context.repository().existsByCode("COLLIDE1")).thenReturn(true);

		LinkResponse response = context.service().create(request(null, null), null);

		assertEquals("SUCCESS2", response.code());
		verify(context.generator(), times(2)).generate(8);
		verify(context.writer(), times(2)).insert(any(ShortLink.class));
	}

	@Test
	void throwsAfterCollisionRetryExhaustion() {
		ServiceContext context = context();
		when(context.generator().generate(8)).thenReturn("code-one", "code-two");
		when(context.writer().insert(any(ShortLink.class))).thenThrow(new DataIntegrityViolationException("duplicate"));
		when(context.repository().existsByCode("code-one")).thenReturn(true);
		when(context.repository().existsByCode("code-two")).thenReturn(true);

		assertThrows(CodeGenerationException.class, () -> context.service().create(request(null, null), null));

		verify(context.generator(), times(2)).generate(8);
		verify(context.writer(), times(2)).insert(any(ShortLink.class));
	}

	@Test
	void throwsForCustomAliasCollision() {
		ServiceContext context = context();
		when(context.writer().insert(any(ShortLink.class))).thenThrow(new DataIntegrityViolationException("duplicate"));
		when(context.repository().existsByCode("custom-code")).thenReturn(true);

		assertThrows(AliasAlreadyExistsException.class, () -> context.service().create(request("custom-code", null), null));

		verify(context.generator(), never()).generate(8);
		verify(context.writer(), times(1)).insert(any(ShortLink.class));
	}

	@Test
	void throwsWhenCodeIsMissing() {
		ServiceContext context = context();
		when(context.repository().findByCode("missing")).thenReturn(Optional.empty());

		assertThrows(LinkNotFoundException.class, () -> context.service().resolve("missing"));
	}

	@Test
	void throwsWhenLinkIsExpired() {
		ServiceContext context = context();
		when(context.repository().findByCode("expired"))
				.thenReturn(Optional.of(link("expired", "https://example.com", NOW, true)));

		assertThrows(LinkExpiredException.class, () -> context.service().resolve("expired"));
	}

	@Test
	void throwsWhenLinkIsDisabled() {
		ServiceContext context = context();
		when(context.repository().findByCode("disabled"))
				.thenReturn(Optional.of(link("disabled", "https://example.com", NOW.plus(DEFAULT_TTL), false)));

		assertThrows(LinkDisabledException.class, () -> context.service().resolve("disabled"));
	}

	@Test
	void resolvesActiveUnexpiredLink() {
		ServiceContext context = context();
		ShortLink link = link("active", "https://example.com", NOW.plus(DEFAULT_TTL), true);
		when(context.repository().findByCode("active")).thenReturn(Optional.of(link));

		assertSame(link, context.service().resolve("active"));
	}

	@Test
	void disableChangesLinkToInactive() {
		ServiceContext context = context();
		ShortLink link = link("active", "https://example.com", NOW.plus(DEFAULT_TTL), true);
		when(context.repository().findByCode("active")).thenReturn(Optional.of(link));

		context.service().disable("active");

		assertFalse(link.isActive());
	}

	@Test
	void repeatedDisableIsSafe() {
		ServiceContext context = context();
		ShortLink link = link("active", "https://example.com", NOW.plus(DEFAULT_TTL), true);
		when(context.repository().findByCode("active")).thenReturn(Optional.of(link));

		context.service().disable("active");
		context.service().disable("active");

		assertFalse(link.isActive());
		verify(context.repository(), times(2)).findByCode("active");
	}

	@Test
	void rethrowsUnclassifiedIntegrityViolation() {
		ServiceContext context = context();
		DataIntegrityViolationException exception = new DataIntegrityViolationException("unexpected failure");
		when(context.generator().generate(8)).thenReturn("generated");
		when(context.writer().insert(any(ShortLink.class))).thenThrow(exception);
		when(context.repository().existsByCode("generated")).thenReturn(false);

		assertSame(exception, assertThrows(
				DataIntegrityViolationException.class,
				() -> context.service().create(request(null, null), null)));
		verify(context.generator(), times(1)).generate(8);
		verify(context.writer(), times(1)).insert(any(ShortLink.class));
	}

	private void persistInput(ServiceContext context) {
		when(context.writer().insert(any(ShortLink.class))).thenAnswer(invocation -> invocation.getArgument(0));
	}

	private ShortLink capturedLink(ServiceContext context) {
		ArgumentCaptor<ShortLink> captor = ArgumentCaptor.forClass(ShortLink.class);
		verify(context.writer(), times(1)).insert(captor.capture());
		return captor.getValue();
	}

	private ServiceContext context() {
		ShortLinkRepository repository = Mockito.mock(ShortLinkRepository.class);
		ShortLinkWriter writer = Mockito.mock(ShortLinkWriter.class);
		CodeGenerator generator = Mockito.mock(CodeGenerator.class);
		return new ServiceContext(
				new ShortLinkService(repository, writer, generator, new UrlSafetyValidator(), PROPERTIES,
						Clock.fixed(NOW, ZoneOffset.UTC)),
				repository,
				writer,
				generator);
	}

	private CreateLinkRequest request(String customAlias, Instant expiresAt) {
		return new CreateLinkRequest("https://example.com", customAlias, expiresAt);
	}

	private ShortLink link(String code, String originalUrl, Instant expiresAt, boolean active) {
		return new ShortLink(code, originalUrl, NOW, expiresAt, active, null);
	}

	private record ServiceContext(
			ShortLinkService service,
			ShortLinkRepository repository,
			ShortLinkWriter writer,
			CodeGenerator generator) {
	}
}