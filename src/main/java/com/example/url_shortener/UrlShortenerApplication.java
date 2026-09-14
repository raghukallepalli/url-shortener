package com.example.url_shortener;

import java.security.SecureRandom;
import java.time.Clock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {"com.example.url_shortener", "com.example.shortener"})
@ConfigurationPropertiesScan(basePackages = "com.example.shortener.config")
@EntityScan(basePackages = "com.example.shortener.domain")
@EnableJpaRepositories(basePackages = "com.example.shortener.repository")
public class UrlShortenerApplication {

	public static void main(String[] args) {
		SpringApplication.run(UrlShortenerApplication.class, args);
	}

	@Bean
	SecureRandom secureRandom() {
		return new SecureRandom();
	}

	@Bean
	Clock clock() {
		return Clock.systemUTC();
	}
}
