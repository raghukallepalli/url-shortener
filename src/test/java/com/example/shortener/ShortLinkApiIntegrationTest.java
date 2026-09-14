package com.example.shortener;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.shortener.repository.ShortLinkRepository;
import com.example.shortener.repository.ClickEventRepository;
import com.example.shortener.domain.ShortLink;
import java.time.Instant;
import com.example.url_shortener.UrlShortenerApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = UrlShortenerApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ShortLinkApiIntegrationTest {

	private static final String ORIGINAL_URL = "https://example.com/products?id=7";
	private static final String REQUEST_BODY = "{\"originalUrl\":\"https://example.com/products?id=7\",\"customAlias\":\"demo7\"}";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ShortLinkRepository shortLinkRepository;

	@Autowired
	private ClickEventRepository clickEventRepository;

	@BeforeEach
	void clearRepository() {
		clickEventRepository.deleteAll();
		clickEventRepository.flush();
		shortLinkRepository.deleteAll();
		shortLinkRepository.flush();
	}

	@Test
	void completesShortLinkLifecycle() throws Exception {
		mockMvc.perform(post("/api/v1/links")
				.contentType(MediaType.APPLICATION_JSON)
				.header("Idempotency-Key", "request-7")
				.content(REQUEST_BODY))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.code").value("demo7"))
				.andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/demo7")));

		mockMvc.perform(post("/api/v1/links")
				.contentType(MediaType.APPLICATION_JSON)
				.header("Idempotency-Key", "request-7")
				.content(REQUEST_BODY))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.code").value("demo7"));
		assertEquals(1, shortLinkRepository.count());

		mockMvc.perform(get("/api/v1/links/demo7"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value("demo7"))
				.andExpect(jsonPath("$.originalUrl").value(ORIGINAL_URL))
				.andExpect(jsonPath("$.createdAt").exists())
				.andExpect(jsonPath("$.expiresAt").exists());

		assertRedirect("/demo7");
		assertRedirect("/demo7");

		mockMvc.perform(get("/api/v1/links/demo7/analytics"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalClicks").value(2));

		assertEquals(2, clickEventRepository.count());
		mockMvc.perform(get("/missing1"))
				.andExpect(status().isNotFound());
		assertEquals(2, clickEventRepository.count());

		mockMvc.perform(delete("/api/v1/links/demo7"))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/demo7"))
				.andExpect(status().isGone())
				.andExpect(jsonPath("$.code").value("LINK_DISABLED"));
		assertEquals(2, clickEventRepository.count());

		mockMvc.perform(get("/api/v1/links/demo7/analytics"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalClicks").value(2));
	}

	@Test
	void rejectedExpiredRedirectDoesNotCreateClickEvent() throws Exception {
		shortLinkRepository.saveAndFlush(new ShortLink(
				"expired1", ORIGINAL_URL, Instant.parse("2020-01-01T00:00:00Z"),
				Instant.parse("2020-01-02T00:00:00Z"), true, null));

		mockMvc.perform(get("/expired1"))
				.andExpect(status().isGone())
				.andExpect(jsonPath("$.code").value("LINK_EXPIRED"));

		assertEquals(0, clickEventRepository.count());
	}

	private void assertRedirect(String path) throws Exception {
		mockMvc.perform(get(path))
				.andExpect(status().isFound())
				.andExpect(header().string("Location", ORIGINAL_URL))
				.andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
				.andExpect(header().string("Pragma", "no-cache"));
	}
}