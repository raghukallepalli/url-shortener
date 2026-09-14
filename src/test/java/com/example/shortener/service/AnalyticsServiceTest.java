package com.example.shortener.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import com.example.shortener.api.AnalyticsResponse;
import com.example.shortener.config.ShortenerProperties;
import com.example.shortener.domain.ClickEvent;
import com.example.shortener.domain.ShortLink;
import com.example.shortener.exception.InvalidAnalyticsRangeException;
import com.example.shortener.repository.ClickEventRepository;
import com.example.shortener.repository.ShortLinkRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.domain.Pageable;

class AnalyticsServiceTest {

	private static final Instant NOW = Instant.parse("2026-09-14T12:00:00Z");
	private static final ShortenerProperties PROPERTIES = new ShortenerProperties(
			URI.create("http://localhost:8080"),
			8,
			2,
			Duration.ofDays(365),
			new ShortenerProperties.Analytics("test-secret", Duration.ofDays(30), Duration.ofDays(90)));

	@Test
	void recordsHashedIpAndHostnameOnlyReferrer() {
		ServiceContext context = context();

		context.service().recordClick(link(true), new ClickMetadata(
				"203.0.113.10", "https://News.Example.com/article?source=mail", "Mozilla/5.0 Chrome/120"));

		ClickEvent event = capturedEvent(context);
		assertThat(event.getIpHash()).isNotEqualTo("203.0.113.10").matches("[0-9a-f]{64}");
		assertEquals("news.example.com", event.getReferrerHost());
	}

	@Test
	void sameIpAndSecretProduceSameHashWhileDifferentIpsDoNot() {
		ServiceContext context = context();

		context.service().recordClick(link(true), new ClickMetadata("203.0.113.10", null, null));
		context.service().recordClick(link(true), new ClickMetadata("203.0.113.10", null, null));
		context.service().recordClick(link(true), new ClickMetadata("203.0.113.11", null, null));

		ArgumentCaptor<ClickEvent> captor = ArgumentCaptor.forClass(ClickEvent.class);
		verify(context.clickEventRepository(), Mockito.times(3)).saveAndFlush(captor.capture());
		List<ClickEvent> events = captor.getAllValues();
		assertEquals(events.get(0).getIpHash(), events.get(1).getIpHash());
		assertThat(events.get(2).getIpHash()).isNotEqualTo(events.get(0).getIpHash());
		assertThat(events).allSatisfy(event -> assertEquals("direct", event.getReferrerHost()));
	}

	@Test
	void usesDefaultThirtyDayRangeAndRequestsTenReferrers() {
		ServiceContext context = context();
		ShortLink link = link(false);
		when(context.shortLinkService().requireExisting("disabled")).thenReturn(link);
		when(context.clickEventRepository().countByShortLinkIdAndOccurredAtRange(any(), any(), any())).thenReturn(2L);
		when(context.clickEventRepository().findTopReferrersByShortLinkIdAndOccurredAtRange(
				any(), any(), any(), any())).thenReturn(List.of());

		AnalyticsResponse response = context.service().getAnalytics("disabled", null, null);

		assertEquals(NOW.minus(Duration.ofDays(30)), response.from());
		assertEquals(NOW, response.to());
		assertEquals(2, response.totalClicks());
		verify(context.clickEventRepository()).countByShortLinkIdAndOccurredAtRange(
				eq(null), eq(NOW.minus(Duration.ofDays(30))), eq(NOW));
		ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
		verify(context.clickEventRepository()).findTopReferrersByShortLinkIdAndOccurredAtRange(
				eq(null), eq(NOW.minus(Duration.ofDays(30))), eq(NOW), pageable.capture());
		assertEquals(10, pageable.getValue().getPageSize());
	}

	@Test
	void rejectsInvalidAndOversizedRanges() {
		ServiceContext context = context();
		when(context.shortLinkService().requireExisting("link")).thenReturn(link(true));

		assertThrows(InvalidAnalyticsRangeException.class,
				() -> context.service().getAnalytics("link", NOW, NOW));
		assertThrows(InvalidAnalyticsRangeException.class,
				() -> context.service().getAnalytics("link", NOW.minus(Duration.ofDays(91)), NOW));
	}

	private ClickEvent capturedEvent(ServiceContext context) {
		ArgumentCaptor<ClickEvent> captor = ArgumentCaptor.forClass(ClickEvent.class);
		verify(context.clickEventRepository()).saveAndFlush(captor.capture());
		return captor.getValue();
	}

	private ServiceContext context() {
		ClickEventRepository clickEventRepository = Mockito.mock(ClickEventRepository.class);
		ShortLinkService shortLinkService = Mockito.mock(ShortLinkService.class);
		return new ServiceContext(
				new AnalyticsService(clickEventRepository, shortLinkService, PROPERTIES,
						Clock.fixed(NOW, ZoneOffset.UTC)),
				clickEventRepository,
				shortLinkService);
	}

	private ShortLink link(boolean active) {
		return new ShortLink("disabled", "https://example.com", NOW, NOW.plus(Duration.ofDays(365)), active, null);
	}

	private record ServiceContext(
			AnalyticsService service,
			ClickEventRepository clickEventRepository,
			ShortLinkService shortLinkService) {
	}
}