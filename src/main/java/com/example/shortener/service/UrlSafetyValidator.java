package com.example.shortener.service;

import java.net.IDN;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

import org.springframework.stereotype.Component;

@Component
public class UrlSafetyValidator {

	public String validateAndNormalize(String input) {
		if (input == null || input.trim().isEmpty()) {
			throw new UrlValidationException("URL must not be blank");
		}

		try {
			URI uri = new URI(input.trim());
			String scheme = normalizeScheme(uri);
			HostAndPort hostAndPort = parseAuthority(uri.getRawAuthority());
			String host = normalizeHost(hostAndPort.host());
			rejectPrivateAddress(host);

			URI normalized = uri.normalize();
			StringBuilder result = new StringBuilder(scheme).append("://");
			if (host.indexOf(':') >= 0) {
				result.append('[').append(host).append(']');
			} else {
				result.append(host);
			}
			if (hostAndPort.port() != null) {
				result.append(':').append(hostAndPort.port());
			}
			result.append(normalized.getRawPath());
			if (normalized.getRawQuery() != null) {
				result.append('?').append(normalized.getRawQuery());
			}
			if (normalized.getRawFragment() != null) {
				result.append('#').append(normalized.getRawFragment());
			}

			return new URI(result.toString()).toASCIIString();
		} catch (URISyntaxException | IllegalArgumentException exception) {
			throw new UrlValidationException("Invalid URL", exception);
		}
	}

	private String normalizeScheme(URI uri) {
		if (!uri.isAbsolute() || uri.getScheme() == null) {
			throw new UrlValidationException("URL must be absolute");
		}

		String scheme = uri.getScheme().toLowerCase(Locale.ROOT);
		if (!scheme.equals("http") && !scheme.equals("https")) {
			throw new UrlValidationException("URL scheme must be HTTP or HTTPS");
		}
		return scheme;
	}

	private HostAndPort parseAuthority(String authority) {
		if (authority == null || authority.isBlank() || authority.indexOf('@') >= 0) {
			throw new UrlValidationException("URL must contain a host and no credentials");
		}

		if (authority.startsWith("[")) {
			int closingBracket = authority.indexOf(']');
			if (closingBracket < 2 || !isValidPortSuffix(authority.substring(closingBracket + 1))) {
				throw new UrlValidationException("URL host is invalid");
			}
			return new HostAndPort(
					authority.substring(1, closingBracket),
					portFromSuffix(authority.substring(closingBracket + 1)));
		}

		int colonIndex = authority.lastIndexOf(':');
		if (colonIndex >= 0) {
			if (authority.indexOf(':') != colonIndex || !isValidPortSuffix(authority.substring(colonIndex))) {
				throw new UrlValidationException("URL host is invalid");
			}
			return new HostAndPort(authority.substring(0, colonIndex), authority.substring(colonIndex + 1));
		}

		return new HostAndPort(authority, null);
	}

	private boolean isValidPortSuffix(String suffix) {
		if (suffix.isEmpty()) {
			return true;
		}
		if (!suffix.startsWith(":")) {
			return false;
		}

		String port = suffix.substring(1);
		try {
			return !port.isEmpty() && Integer.parseInt(port) <= 65535;
		} catch (NumberFormatException exception) {
			return false;
		}
	}

	private String portFromSuffix(String suffix) {
		return suffix.isEmpty() ? null : suffix.substring(1);
	}

	private String normalizeHost(String host) {
		if (host.isBlank()) {
			throw new UrlValidationException("URL host is required");
		}
		if (host.indexOf(':') >= 0) {
			return host.toLowerCase(Locale.ROOT);
		}

		String normalizedHost = IDN.toASCII(host).toLowerCase(Locale.ROOT);
		try {
			if (new URI("http://" + normalizedHost).getHost() == null) {
				throw new UrlValidationException("URL host is invalid");
			}
		} catch (URISyntaxException exception) {
			throw new UrlValidationException("URL host is invalid", exception);
		}
		return normalizedHost;
	}

	private void rejectPrivateAddress(String host) {
		if (host.equals("localhost") || isIpv6Loopback(host) || isPrivateIpv4(host)) {
			throw new UrlValidationException("URL host is not permitted");
		}
	}

	private boolean isIpv6Loopback(String host) {
		return host.indexOf(':') >= 0 && host.replace(":", "").matches("0*1");
	}

	private boolean isPrivateIpv4(String host) {
		String[] octets = host.split("\\.", -1);
		if (octets.length != 4) {
			return false;
		}

		int[] address = new int[4];
		try {
			for (int index = 0; index < octets.length; index++) {
				address[index] = Integer.parseInt(octets[index]);
				if (address[index] < 0 || address[index] > 255) {
					return false;
				}
			}
		} catch (NumberFormatException exception) {
			return false;
		}

		return address[0] == 0
				|| address[0] == 10
				|| address[0] == 127
				|| (address[0] == 172 && address[1] >= 16 && address[1] <= 31)
				|| (address[0] == 192 && address[1] == 168);
	}

	private record HostAndPort(String host, String port) {
	}
}