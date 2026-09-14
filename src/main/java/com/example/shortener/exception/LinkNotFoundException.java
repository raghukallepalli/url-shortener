package com.example.shortener.exception;

public class LinkNotFoundException extends RuntimeException {

	public LinkNotFoundException(String message) {
		super(message);
	}
}