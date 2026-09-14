package com.example.shortener.web;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import com.example.shortener.domain.ShortLink;
import com.example.shortener.exception.LinkDisabledException;
import com.example.shortener.exception.LinkExpiredException;
import com.example.shortener.exception.LinkNotFoundException;
import com.example.shortener.service.AnalyticsService;
import com.example.shortener.service.ClickMetadata;
import com.example.shortener.service.ClickMetadataExtractor;
import com.example.shortener.service.ShortLinkService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RedirectController.class)
@ContextConfiguration(classes = RedirectControllerTest.WebTestConfiguration.class)
class RedirectControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ShortLinkService shortLinkService;

	@MockitoBean
	private AnalyticsService analyticsService;

	@MockitoBean
	private ClickMetadataExtractor clickMetadataExtractor;

	@Test
	void existingActiveLinkReturnsRedirectWithNoCacheHeaders() throws Exception {
		ShortLink link = activeLink();
		ClickMetadata metadata = new ClickMetadata("127.0.0.1", null, null);
		when(shortLinkService.resolve("abcd1234")).thenReturn(link);
		when(clickMetadataExtractor.extract(org.mockito.ArgumentMatchers.any())).thenReturn(metadata);

		mockMvc.perform(get("/abcd1234"))
				.andExpect(status().isFound())
				.andExpect(header().string("Location", "https://example.com/target"))
				.andExpect(header().string("Cache-Control", "no-store"))
				.andExpect(header().string("Pragma", "no-cache"));

		verify(analyticsService).recordClick(link, metadata);
	}

	@Test
	void missingLinkReturnsNotFound() throws Exception {
		when(shortLinkService.resolve("missing1"))
				.thenThrow(new LinkNotFoundException("Short link was not found: missing1"));

		mockMvc.perform(get("/missing1"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("LINK_NOT_FOUND"));

		verifyNoInteractions(analyticsService, clickMetadataExtractor);
	}

	@Test
	void expiredLinkReturnsGone() throws Exception {
		when(shortLinkService.resolve("expired1"))
				.thenThrow(new LinkExpiredException("Short link has expired: expired1"));

		mockMvc.perform(get("/expired1"))
				.andExpect(status().isGone())
				.andExpect(jsonPath("$.code").value("LINK_EXPIRED"));

		verifyNoInteractions(analyticsService, clickMetadataExtractor);
	}

	@Test
	void disabledLinkReturnsGone() throws Exception {
		when(shortLinkService.resolve("disabled1"))
				.thenThrow(new LinkDisabledException("Short link is disabled: disabled1"));

		mockMvc.perform(get("/disabled1"))
				.andExpect(status().isGone())
				.andExpect(jsonPath("$.code").value("LINK_DISABLED"));

		verifyNoInteractions(analyticsService, clickMetadataExtractor);
	}

	@Test
	void invalidCodeFormatDoesNotInvokeService() throws Exception {
		mockMvc.perform(get("/bad"));

		verifyNoInteractions(shortLinkService, analyticsService, clickMetadataExtractor);
	}

	private ShortLink activeLink() {
		return new ShortLink(
				"abcd1234",
				"https://example.com/target",
				Instant.parse("2026-09-14T12:00:00Z"),
				Instant.parse("2027-09-14T12:00:00Z"),
				true,
				null);
	}

	@SpringBootConfiguration(proxyBeanMethods = false)
	@Import({
			RedirectController.class,
			GlobalExceptionHandler.class,
			RequestIdFilter.class,
			TestClockConfiguration.class
	})
	static class WebTestConfiguration {
	}

	@TestConfiguration
	static class TestClockConfiguration {

		@Bean
		Clock clock() {
			return Clock.fixed(Instant.parse("2026-09-14T12:00:00Z"), ZoneOffset.UTC);
		}
	}
}