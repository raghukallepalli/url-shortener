package com.example.url_shortener;

import java.security.SecureRandom;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;

@SpringBootApplication(scanBasePackages = {"com.example.url_shortener", "com.example.shortener"})
@ConfigurationPropertiesScan(basePackages = "com.example.shortener.config")
public class UrlShortenerApplication {

	public static void main(String[] args) {
		SpringApplication.run(UrlShortenerApplication.class, args);
	}

	@Bean
	SecureRandom secureRandom() {
		return new SecureRandom();
	}
}
