package com.example.shortener.domain;

import java.time.Instant;
import java.sql.Types;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;

@Entity
@Table(name = "click_events")
public class ClickEvent {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "short_link_id", nullable = false)
	private Long shortLinkId;

	@Column(name = "occurred_at", nullable = false)
	private Instant occurredAt;

	@JdbcTypeCode(Types.CHAR)
	@Column(name = "ip_hash", nullable = false, length = 64, columnDefinition = "CHAR(64)")
	private String ipHash;

	@Column(name = "referrer_host", length = 255)
	private String referrerHost;

	@Column(name = "user_agent_family", length = 80)
	private String userAgentFamily;

	protected ClickEvent() {
	}

	public ClickEvent(
			Long shortLinkId,
			Instant occurredAt,
			String ipHash,
			String referrerHost,
			String userAgentFamily) {
		this.shortLinkId = shortLinkId;
		this.occurredAt = occurredAt;
		this.ipHash = ipHash;
		this.referrerHost = referrerHost;
		this.userAgentFamily = userAgentFamily;
	}

	public Long getId() {
		return id;
	}

	public Long getShortLinkId() {
		return shortLinkId;
	}

	public Instant getOccurredAt() {
		return occurredAt;
	}

	public String getIpHash() {
		return ipHash;
	}

	public String getReferrerHost() {
		return referrerHost;
	}

	public String getUserAgentFamily() {
		return userAgentFamily;
	}
}