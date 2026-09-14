CREATE TABLE short_links (
	id BIGSERIAL PRIMARY KEY,
	code VARCHAR(32) NOT NULL UNIQUE,
	original_url VARCHAR(2048) NOT NULL,
	created_at TIMESTAMPTZ NOT NULL,
	expires_at TIMESTAMPTZ,
	active BOOLEAN NOT NULL DEFAULT TRUE,
	idempotency_key VARCHAR(128) UNIQUE,
	version BIGINT NOT NULL DEFAULT 0,
	CONSTRAINT short_links_code_format_check
		CHECK (code ~ '^[A-Za-z0-9_-]{4,32}$')
);

CREATE INDEX short_links_active_expires_at_idx
	ON short_links (expires_at)
	WHERE active;
