package com.example.shortener.service;

public class UrlValidationException extends RuntimeException {

	public UrlValidationException(String message) {
		super(message);
	}

	public UrlValidationException(String message, Throwable cause) {
		super(message, cause);
	}
}