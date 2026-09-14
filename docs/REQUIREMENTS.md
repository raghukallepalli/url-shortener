# URL Shortener Requirements

## Functional Requirements

- Create a short URL from an absolute `http` or `https` destination URL.
- Allow an optional custom alias; aliases are unique within the service.
- Redirect `GET /{code}` to the active, unexpired destination.
- Allow an optional expiration timestamp. An expired link must no longer redirect.
- Disable a link without deleting its configuration. A disabled link must no longer redirect.
- Record click analytics for redirect attempts that result in a redirect.
- Support idempotent creation through an `Idempotency-Key`: repeating the same request with the same key returns the original result; reusing a key with a materially different request is rejected.

## Non-Functional Requirements

- **Reliability:** link creation and state changes are durable; redirects return a deterministic result for unknown, disabled, and expired codes.
- **Security:** validate destination URLs, prevent open-redirect header injection, use collision-resistant generated codes, and rate-limit public endpoints.
- **Scalability:** redirect lookup is designed for read-heavy traffic; analytics writes must not require synchronous aggregation.
- **Observability:** expose structured logs, metrics for redirect outcomes and creation failures, and request correlation IDs.
- **Testability:** isolate code generation, time, persistence, and analytics behind testable boundaries; cover API contracts and state transitions.
- **Privacy:** collect only analytics fields needed for the agreed reporting purpose; do not store raw IP addresses or full user-agent strings unless explicitly approved.

## Explicit Assumptions

- The service owns a configured public base URL used to construct returned short URLs.
- Codes and custom aliases are case-sensitive, URL-safe strings; generated codes use a documented minimum entropy target.
- `expiresAt` is an ISO-8601 UTC timestamp and must be in the future when supplied.
- Unknown, disabled, and expired codes return `404 Not Found` so link state is not disclosed publicly.
- Redirects use `302 Found` until a permanent-redirect policy is agreed.
- Analytics initially exposes an aggregate click count only. Its consistency and bot policy remain open questions below.
- Management endpoints are unauthenticated only for this initial assignment scope; this must be replaced or explicitly accepted before production use.

## Questions To Clarify

- Does “real-time analytics” mean synchronous visibility after a redirect, a bounded delay, or a dashboard refresh interval?
- Are click counts required to be exact, or is an approximate count acceptable at scale?
- How long must analytics data be retained, and what deletion/export obligations apply?
- Do automated crawlers, link previews, and monitoring probes count as clicks?
- What authentication, authorization, and tenant-isolation model is required for creation, disablement, and analytics?
- If analytics storage is unavailable, should a valid redirect proceed, be retried asynchronously, or fail?

## Initial API Acceptance Criteria

### `POST /api/links`

Request: destination URL, optional `alias`, optional `expiresAt`; `Idempotency-Key` header is required.

- Returns `201 Created` with `code`, `shortUrl`, `destinationUrl`, `expiresAt`, and active status for a new valid request.
- Returns the original successful representation for a replay with the same idempotency key and equivalent request.
- Returns `409 Conflict` for an alias already owned by another link or a key reused with a different request.
- Returns `400 Bad Request` for an invalid destination, alias, expiration, or missing idempotency key.

### `GET /{code}`

- Returns `302 Found` with a `Location` header equal to the stored destination for an active, unexpired code.
- Records one eligible click event for each successful redirect, subject to the unresolved bot policy.
- Returns `404 Not Found` for unknown, disabled, or expired codes and does not redirect.

### `POST /api/links/{code}/disable`

- Returns `200 OK` with the link marked disabled when an active link exists.
- Is idempotent: repeated calls leave the link disabled and return `200 OK`.
- Returns `404 Not Found` when the code does not exist.
- A successfully disabled link subsequently returns `404 Not Found` from `GET /{code}`.

### `GET /api/links/{code}/analytics`

- Returns `200 OK` with the code and its aggregate click count for an existing link.
- Returns `404 Not Found` when the code does not exist.
- States the count timestamp or freshness guarantee once “real-time” has been clarified.
- Does not return raw IP addresses or full user-agent strings.
