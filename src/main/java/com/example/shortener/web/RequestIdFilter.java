package com.example.shortener.web;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RequestIdFilter extends OncePerRequestFilter {

	private static final String REQUEST_ID_HEADER = "X-Request-Id";
	private static final String TRACE_ID_ATTRIBUTE = "traceId";
	private static final Pattern REQUEST_ID_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{8,64}$");

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {
		String traceId = requestId(request.getHeader(REQUEST_ID_HEADER));
		request.setAttribute(TRACE_ID_ATTRIBUTE, traceId);
		response.setHeader(REQUEST_ID_HEADER, traceId);
		MDC.put(TRACE_ID_ATTRIBUTE, traceId);

		try {
			filterChain.doFilter(request, response);
		} finally {
			MDC.remove(TRACE_ID_ATTRIBUTE);
		}
	}

	private String requestId(String requestId) {
		if (requestId != null && REQUEST_ID_PATTERN.matcher(requestId).matches()) {
			return requestId;
		}
		return UUID.randomUUID().toString();
	}
}