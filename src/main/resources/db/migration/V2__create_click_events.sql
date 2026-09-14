CREATE TABLE click_events (
	id BIGSERIAL PRIMARY KEY,
	short_link_id BIGINT NOT NULL REFERENCES short_links(id),
	occurred_at TIMESTAMPTZ NOT NULL,
	ip_hash CHAR(64) NOT NULL,
	referrer_host VARCHAR(255),
	user_agent_family VARCHAR(80)
);

CREATE INDEX click_events_short_link_id_occurred_at_idx
	ON click_events (short_link_id, occurred_at DESC);

CREATE INDEX click_events_occurred_at_idx
	ON click_events (occurred_at);