package com.example.shortener.exception;

public class LinkExpiredException extends RuntimeException {

	public LinkExpiredException(String message) {
		super(message);
	}
}