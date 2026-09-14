package com.example.shortener.repository;

import java.util.Optional;

import com.example.shortener.domain.ShortLink;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShortLinkRepository extends JpaRepository<ShortLink, Long> {

	Optional<ShortLink> findByCode(String code);

	Optional<ShortLink> findByIdempotencyKey(String idempotencyKey);

	boolean existsByCode(String code);
}