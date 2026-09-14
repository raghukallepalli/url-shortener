package com.example.shortener.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.example.shortener.api.AnalyticsResponse;
import com.example.shortener.api.ReferrerStat;
import com.example.shortener.config.ShortenerProperties;
import com.example.shortener.domain.ClickEvent;
import com.example.shortener.domain.ShortLink;
import com.example.shortener.exception.InvalidAnalyticsRangeException;
import com.example.shortener.repository.ClickEventRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyticsService {

	private static final int TOP_REFERRER_LIMIT = 10;

	private final ClickEventRepository clickEventRepository;
	private final ShortLinkService shortLinkService;
	private final ShortenerProperties shortenerProperties;
	private final Clock clock;

	public AnalyticsService(
			ClickEventRepository clickEventRepository,
			ShortLinkService shortLinkService,
			ShortenerProperties shortenerProperties,
			Clock clock) {
		this.clickEventRepository = clickEventRepository;
		this.shortLinkService = shortLinkService;
		this.shortenerProperties = shortenerProperties;
		this.clock = clock;
	}

	@Transactional
	public void recordClick(ShortLink link, ClickMetadata metadata) {
		clickEventRepository.saveAndFlush(new ClickEvent(
				link.getId(),
				clock.instant(),
				hmacIpAddress(metadata.ipAddress()),
				referrerHost(metadata.referrer()),
				userAgentFamily(metadata.userAgent())));
	}

	@Transactional(readOnly = true)
	public AnalyticsResponse getAnalytics(String code, Instant from, Instant to) {
		ShortLink link = shortLinkService.requireExisting(code);
		Instant now = clock.instant();
		Instant effectiveTo = to == null ? now : to;
		Instant effectiveFrom = from == null
				? effectiveTo.minus(shortenerProperties.analytics().defaultRange())
				: from;
		validateRange(effectiveFrom, effectiveTo, now);

		long totalClicks = clickEventRepository.countByShortLinkIdAndOccurredAtRange(
				link.getId(), effectiveFrom, effectiveTo);
		List<ReferrerStat> topReferrers = clickEventRepository
				.findTopReferrersByShortLinkIdAndOccurredAtRange(
						link.getId(), effectiveFrom, effectiveTo, PageRequest.of(0, TOP_REFERRER_LIMIT))
				.stream()
				.map(view -> new ReferrerStat(view.getReferrer(), view.getClicks()))
				.toList();

		return new AnalyticsResponse(code, effectiveFrom, effectiveTo, totalClicks, topReferrers);
	}

	private String hmacIpAddress(String ipAddress) {
		if (ipAddress == null || ipAddress.isBlank()) {
			throw new IllegalArgumentException("Client IP address is required");
		}

		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(
					shortenerProperties.analytics().ipHashSecret().getBytes(StandardCharsets.UTF_8),
					"HmacSHA256"));
			return HexFormat.of().formatHex(mac.doFinal(ipAddress.getBytes(StandardCharsets.UTF_8)));
		} catch (GeneralSecurityException exception) {
			throw new IllegalStateException("Unable to hash client IP address", exception);
		}
	}

	private String referrerHost(String referrer) {
		if (referrer == null || referrer.isBlank()) {
			return "direct";
		}

		try {
			String host = new URI(referrer).getHost();
			return host == null || host.isBlank() ? "direct" : host.toLowerCase(Locale.ROOT);
		} catch (URISyntaxException exception) {
			return "direct";
		}
	}

	private String userAgentFamily(String userAgent) {
		if (userAgent == null || userAgent.isBlank()) {
			return "unknown";
		}

		String normalizedUserAgent = userAgent.toLowerCase(Locale.ROOT);
		if (normalizedUserAgent.contains("bot")
				|| normalizedUserAgent.contains("spider")
				|| normalizedUserAgent.contains("crawl")) {
			return "bot";
		}
		if (normalizedUserAgent.contains("edg/") || normalizedUserAgent.contains("edge/")) {
			return "Edge";
		}
		if (normalizedUserAgent.contains("firefox")) {
			return "Firefox";
		}
		if (normalizedUserAgent.contains("chrome") || normalizedUserAgent.contains("crios")) {
			return "Chrome";
		}
		if (normalizedUserAgent.contains("safari")) {
			return "Safari";
		}
		return "other";
	}

	private void validateRange(Instant from, Instant to, Instant now) {
		if (!from.isBefore(to)) {
			throw new InvalidAnalyticsRangeException("Analytics range start must be before its end");
		}
		if (to.isAfter(now)) {
			throw new InvalidAnalyticsRangeException("Analytics range end must not be in the future");
		}
		Duration range = Duration.between(from, to);
		if (range.compareTo(shortenerProperties.analytics().maximumRange()) > 0) {
			throw new InvalidAnalyticsRangeException("Analytics range exceeds the configured maximum");
		}
	}
}