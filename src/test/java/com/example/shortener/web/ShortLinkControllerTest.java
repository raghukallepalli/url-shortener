package com.example.shortener.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import com.example.shortener.api.LinkResponse;
import com.example.shortener.exception.AliasAlreadyExistsException;
import com.example.shortener.service.ShortLinkService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ShortLinkController.class)
@ContextConfiguration(classes = ShortLinkControllerTest.WebTestConfiguration.class)
class ShortLinkControllerTest {

	private static final LinkResponse RESPONSE = new LinkResponse(
			"abcd1234",
			"http://localhost:8080/abcd1234",
			"https://example.com/target",
			Instant.parse("2026-09-14T12:00:00Z"),
			Instant.parse("2027-09-14T12:00:00Z"));

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ShortLinkService shortLinkService;

	@Test
	void postValidRequestReturnsCreatedResponseAndLocation() throws Exception {
		when(shortLinkService.create(any(), any())).thenReturn(RESPONSE);

		mockMvc.perform(post("/api/v1/links")
				.contentType(MediaType.APPLICATION_JSON)
				.header("Idempotency-Key", "request-key")
				.content("{\"originalUrl\":\"https://example.com/target\",\"customAlias\":\"abcd1234\"}"))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", RESPONSE.shortUrl()))
				.andExpect(header().exists("X-Request-Id"))
				.andExpect(jsonPath("$.code").value(RESPONSE.code()))
				.andExpect(jsonPath("$.shortUrl").value(RESPONSE.shortUrl()))
				.andExpect(jsonPath("$.originalUrl").value(RESPONSE.originalUrl()))
				.andExpect(jsonPath("$.createdAt").value(RESPONSE.createdAt().toString()))
				.andExpect(jsonPath("$.expiresAt").value(RESPONSE.expiresAt().toString()));
	}

	@Test
	void postBlankOriginalUrlReturnsBadRequest() throws Exception {
		mockMvc.perform(post("/api/v1/links")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"originalUrl\":\"\",\"customAlias\":\"abcd1234\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(header().exists("X-Request-Id"))
				.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
	}

	@Test
	void postInvalidCustomAliasReturnsBadRequest() throws Exception {
		mockMvc.perform(post("/api/v1/links")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"originalUrl\":\"https://example.com/target\",\"customAlias\":\"invalid alias\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(header().exists("X-Request-Id"))
				.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
	}

	@Test
	void getExistingLinkReturnsOk() throws Exception {
		when(shortLinkService.get("abcd1234")).thenReturn(RESPONSE);

		mockMvc.perform(get("/api/v1/links/abcd1234"))
				.andExpect(status().isOk())
				.andExpect(header().exists("X-Request-Id"))
				.andExpect(jsonPath("$.code").value(RESPONSE.code()));
	}

	@Test
	void deleteExistingLinkReturnsNoContent() throws Exception {
		doNothing().when(shortLinkService).disable("abcd1234");

		mockMvc.perform(delete("/api/v1/links/abcd1234"))
				.andExpect(status().isNoContent())
				.andExpect(header().exists("X-Request-Id"));

		verify(shortLinkService).disable("abcd1234");
	}

	@Test
	void aliasAlreadyExistsReturnsConflict() throws Exception {
		when(shortLinkService.create(any(), any()))
				.thenThrow(new AliasAlreadyExistsException("Custom alias is already in use: abcd1234"));

		mockMvc.perform(post("/api/v1/links")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"originalUrl\":\"https://example.com/target\",\"customAlias\":\"abcd1234\"}"))
				.andExpect(status().isConflict())
				.andExpect(header().exists("X-Request-Id"))
				.andExpect(jsonPath("$.code").value("ALIAS_TAKEN"));
	}

	@SpringBootConfiguration(proxyBeanMethods = false)
	@Import({
			ShortLinkController.class,
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