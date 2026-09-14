package com.example.shortener.repository;

import java.time.Instant;
import java.util.List;

import com.example.shortener.domain.ClickEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClickEventRepository extends JpaRepository<ClickEvent, Long> {

	@Query("""
			SELECT COUNT(event)
			FROM ClickEvent event
			WHERE event.shortLinkId = :shortLinkId
			  AND event.occurredAt >= :from
			  AND event.occurredAt < :to
			""")
	long countByShortLinkIdAndOccurredAtRange(
			@Param("shortLinkId") Long shortLinkId,
			@Param("from") Instant from,
			@Param("to") Instant to);

	@Query("""
			SELECT COALESCE(event.referrerHost, 'direct') AS referrer,
				COUNT(event) AS clicks
			FROM ClickEvent event
			WHERE event.shortLinkId = :shortLinkId
			  AND event.occurredAt >= :from
			  AND event.occurredAt < :to
			GROUP BY event.referrerHost
			ORDER BY COUNT(event) DESC
			""")
	List<ReferrerCountView> findTopReferrersByShortLinkIdAndOccurredAtRange(
			@Param("shortLinkId") Long shortLinkId,
			@Param("from") Instant from,
			@Param("to") Instant to,
			Pageable pageable);
}