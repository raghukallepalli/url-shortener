# Test Results

## Execution Record

- Environment captured: 2026-09-14T09:58:10.0159581Z. See [environment evidence](../evidence/environment.txt).
- Maven verification completed: 2026-09-14T05:00:49-05:00; elapsed time: 42.576 s. See [Maven log](../evidence/mvn-clean-verify.log).
- Git commit tested: `0cda0f8e728b31f97297e4b97dc700b2370933e4`.

## Environment Versions

| Component | Captured version |
| --- | --- |
| Java | OpenJDK 17.0.20.1 |
| Maven | Apache Maven 3.9.16 |
| Docker | 29.7.2 |
| Docker Compose | v5.5.1 |
| Database used by Maven verification | PostgreSQL 16.15 |

Source: [environment evidence](../evidence/environment.txt) and [Maven log](../evidence/mvn-clean-verify.log).

## Commands Executed

- Automated verification: `mvn clean verify`. The captured [Maven log](../evidence/mvn-clean-verify.log) contains `BUILD SUCCESS`.
- Manual HTTP validation: the retained [API evidence](../evidence/api) records health, link creation, idempotent replay, two redirects, analytics retrieval, disablement, disabled redirect, and disabled-link analytics results. The exact curl command text is not retained in the evidence directory, so this report does not reconstruct or claim it.
- Database verification: the captured [link query result](../evidence/database-link-result.txt) and [analytics query result](../evidence/database-analytics-result.txt) record the PostgreSQL query output. The exact query command text is not retained in the evidence directory.

## Automated Test Summary

Surefire recorded 63 tests run, 0 failures, 0 errors, and 0 skipped. See [test summary](../evidence/test-summary.txt) and the individual [Surefire reports](../target/surefire-reports).

### Unit and Component Suites

The non-integration suites total 61 tests, all with 0 failures, 0 errors, and 0 skipped:

| Suite | Tests |
| --- | ---: |
| `ShortLinkTest` | 5 |
| `ShortLinkRepositoryTest` | 5 |
| `AnalyticsServiceTest` | 4 |
| `CodeGeneratorTest` | 4 |
| `ShortLinkServiceTest` | 16 |
| `UrlSafetyValidatorTest` | 11 |
| `UrlSafetyValidatorTests` | 2 |
| `RedirectControllerTest` | 5 |
| `ShortLinkControllerTest` | 8 |
| `UrlShortenerApplicationTests` | 1 |

Source: [test summary](../evidence/test-summary.txt).

### Integration Suite

`ShortLinkApiIntegrationTest` ran 2 tests with 0 failures, 0 errors, and 0 skipped. Source: [integration Surefire report](../target/surefire-reports/com.example.shortener.ShortLinkApiIntegrationTest.txt).

## Manual API Validation

These are manual HTTP results, distinct from the automated tests above.

| Validation | Expected | Actual | Result | Evidence |
| --- | --- | --- | --- | --- |
| Maven verify | Build succeeds | `BUILD SUCCESS` | Pass | [Maven log](../evidence/mvn-clean-verify.log) |
| Create link | HTTP 201 | HTTP 201 | Pass | [headers](../evidence/api/create.headers) |
| Redirect | HTTP 302 | HTTP 302 | Pass | [headers](../evidence/api/redirect-01.headers) |
| Click count | 2 | 2 | Pass | [response body](../evidence/api/analytics.json) |
| Disable | HTTP 204 | HTTP 204 | Pass | [headers](../evidence/api/disable.headers) |
| Disabled redirect | HTTP 410 | HTTP 410 | Pass | [headers](../evidence/api/disabled-redirect.headers) |

| Scenario | Observed result | Evidence |
| --- | --- | --- |
| Health | `200`; status `UP`; liveness and readiness groups returned | [headers](../evidence/api/health.headers), [body](../evidence/api/health.json) |
| Create `evidence-demo` | `201`; location points to the created short URL | [headers](../evidence/api/create.headers), [body](../evidence/api/create.json) |
| Idempotent replay | `201`; same code, short URL, original URL, and represented timestamps as creation | [headers](../evidence/api/idempotent-replay.headers), [body](../evidence/api/idempotent-replay.json) |
| Redirect 1 | `302`; destination location, `Cache-Control: no-store`, `Pragma: no-cache`, and zero-length body | [headers](../evidence/api/redirect-01.headers) |
| Redirect 2 | `302`; same redirect and cache headers | [headers](../evidence/api/redirect-02.headers) |
| Analytics after redirects | `200`; `totalClicks` is 2 and each sanitized referrer host has one click | [headers](../evidence/api/analytics.headers), [body](../evidence/api/analytics.json) |
| Disable link | `204` | [headers](../evidence/api/disable.headers) |
| Redirect after disablement | `410`; `LINK_DISABLED` | [headers](../evidence/api/disabled-redirect.headers), [body](../evidence/api/disabled-redirect.json) |
| Analytics after disablement | `200`; `totalClicks` remains 2 | [headers](../evidence/api/disabled-link-analytics.headers), [body](../evidence/api/disabled-link-analytics.json) |

## Flyway Migration Results

The captured Maven log records Flyway running against PostgreSQL 16.15 during `mvn clean verify`; the Maven build completed with `BUILD SUCCESS`. The retained evidence does not provide a separate Flyway migration report with individual migration version or execution-count assertions. See [Maven log](../evidence/mvn-clean-verify.log).

## Database Verification

- The persisted `evidence-demo` row exists, is inactive, and has recorded creation and expiration timestamps. See [link query result](../evidence/database-link-result.txt).
- The persisted click-event count for `evidence-demo` is 2. See [analytics query result](../evidence/database-analytics-result.txt).

## Failures Encountered and Corrections

No automated test failure, Maven build failure, or manual API failure is retained in the evidence files reviewed for this report. The Maven log explicitly records `BUILD SUCCESS`; Surefire records 0 failures and 0 errors. No correction is claimed because no failure-and-correction record is present in the retained evidence.

## Tests Not Performed or Not Evidenced

- **Performance:** no load, latency, throughput, database query-plan, or capacity-test artifact is retained.
- **Security:** no penetration, authorization, rate-limit, dependency, proxy-trust, or secret-management test artifact is retained.
- **Resilience:** no database-outage, analytics-write-failure, retry, concurrency/race, restart, backup, or recovery-test artifact is retained.
- **Migration depth:** no separate Flyway migration report or PostgreSQL Testcontainers artifact is retained beyond the Flyway activity recorded in the Maven log.

Absence of an artifact is not evidence that a test was not run; these areas are marked pending because their execution is not demonstrated by the supplied evidence.

## Final Engineering Sign-Off Status

**Conditional: automated verification and the captured manual lifecycle checks passed; performance, security, resilience, and deeper migration evidence remain pending.**

The captured evidence supports the tested commit for the automated suite and the documented manual lifecycle. It does not support an unconditional production sign-off until the pending categories above have been executed, reviewed, and accepted.