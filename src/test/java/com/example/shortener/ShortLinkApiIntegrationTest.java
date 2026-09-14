package com.example.shortener;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.shortener.repository.ShortLinkRepository;
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

	@BeforeEach
	void clearRepository() {
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

		mockMvc.perform(get("/demo7"))
				.andExpect(status().isFound())
				.andExpect(header().string("Location", ORIGINAL_URL))
				.andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")));

		mockMvc.perform(delete("/api/v1/links/demo7"))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/demo7"))
				.andExpect(status().isGone())
				.andExpect(jsonPath("$.code").value("LINK_DISABLED"));
	}
}