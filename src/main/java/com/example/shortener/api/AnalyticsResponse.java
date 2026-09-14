package com.example.shortener.api;

import java.time.Instant;
import java.util.List;

public record AnalyticsResponse(
		String code,
		Instant from,
		Instant to,
		long totalClicks,
		List<ReferrerStat> topReferrers) {
}