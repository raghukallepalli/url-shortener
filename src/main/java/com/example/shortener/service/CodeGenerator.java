package com.example.shortener.service;

import java.security.SecureRandom;
import java.util.Objects;

import org.springframework.stereotype.Component;

@Component
public class CodeGenerator {

	private static final String BASE62_ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

	private final SecureRandom secureRandom;

	public CodeGenerator(SecureRandom secureRandom) {
		this.secureRandom = Objects.requireNonNull(secureRandom, "secureRandom must not be null");
	}

	public String generate(int length) {
		if (length < 1) {
			throw new IllegalArgumentException("length must be at least 1");
		}

		StringBuilder code = new StringBuilder(length);
		for (int index = 0; index < length; index++) {
			code.append(BASE62_ALPHABET.charAt(secureRandom.nextInt(BASE62_ALPHABET.length())));
		}
		return code.toString();
	}
}