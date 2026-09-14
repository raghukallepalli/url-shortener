package com.example.shortener.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import com.example.shortener.domain.ShortLink;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Import(ShortLinkRepositoryTest.JpaTestConfiguration.class)
class ShortLinkRepositoryTest {

	private static final Instant CREATED_AT = Instant.parse("2026-09-14T12:00:00Z");

	@Autowired
	private ShortLinkRepository repository;

	@Test
	void savesAndFindsByCode() {
		ShortLink saved = repository.saveAndFlush(link("code1234", "key-1234"));

		assertThat(repository.findByCode("code1234"))
				.hasValueSatisfying(found -> assertThat(found.getId()).isEqualTo(saved.getId()));
	}

	@Test
	void savesAndFindsByIdempotencyKey() {
		ShortLink saved = repository.saveAndFlush(link("code1234", "key-1234"));

		assertThat(repository.findByIdempotencyKey("key-1234"))
				.hasValueSatisfying(found -> assertThat(found.getId()).isEqualTo(saved.getId()));
	}

	@Test
	void duplicateCodeViolatesDatabaseConstraint() {
		repository.saveAndFlush(link("code1234", "key-1234"));

		assertThatThrownBy(() -> repository.saveAndFlush(link("code1234", "key-5678")))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void duplicateIdempotencyKeyViolatesDatabaseConstraint() {
		repository.saveAndFlush(link("code1234", "key-1234"));

		assertThatThrownBy(() -> repository.saveAndFlush(link("code5678", "key-1234")))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void populatesVersionForOptimisticLocking() {
		ShortLink saved = repository.saveAndFlush(link("code1234", "key-1234"));

		assertThat(saved.getVersion()).isZero();
	}

	private ShortLink link(String code, String idempotencyKey) {
		return new ShortLink(
				code,
				"https://example.com/target",
				CREATED_AT,
				null,
				true,
				idempotencyKey);
	}

	@SpringBootConfiguration
	@EntityScan(basePackageClasses = ShortLink.class)
	@EnableJpaRepositories(basePackageClasses = ShortLinkRepository.class)
	static class JpaTestConfiguration {
	}
}