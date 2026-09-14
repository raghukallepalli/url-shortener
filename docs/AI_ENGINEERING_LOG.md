# AI Engineering Log

AI assists within defined engineering tasks. The engineer owns correctness, security, maintainability, and production readiness, including review, validation, and final sign-off.

| Date | Task | Intent | Prompt summary | AI suggestion | Engineer action: accepted, edited, or rejected | Engineering rationale | Validation performed | Human sign-off |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 2026-09-14 | Requirement normalization | Convert the assignment brief into an implementation-ready, reviewable contract. | Document URL-shortener functional and non-functional requirements, assumptions, open questions, and API acceptance criteria without implementing Java. | Proposed a concise requirements specification with four initial APIs, explicit assumptions, unresolved policy questions, and measurable acceptance criteria. | Edited | Preserved the requested scope while labeling policy decisions as assumptions rather than business facts; retained unresolved analytics, tenant, and failure-mode decisions for stakeholder clarification. | Reviewed `docs/REQUIREMENTS.md` for coverage of requested requirements, assumptions, questions, and acceptance criteria. | Pending |


| Task                    | Engineer action       | Rationale                                                                                           |
| ----------------------- | --------------------- | --------------------------------------------------------------------------------------------------- |
| API and schema design   | Edited Copilot output | Kept analytics outside V1 so it can be introduced and regression-tested as a brownfield enhancement |
| Database schema         | Accepted with review  | Database constraints remain the final protection against concurrent alias and idempotency conflicts |
| Hibernate configuration | Edited                | Used `ddl-auto: validate` because Flyway owns production schema changes                             |


| Task           | Copilot result                  | Engineer action      | Rationale                                                  |
| -------------- | ------------------------------- | -------------------- | ---------------------------------------------------------- |
| JPA entity     | Generated entity with setters   | Edited               | Protected immutable identity and creation fields           |
| Code generator | Generated Base62 candidate      | Accepted with review | SecureRandom provides unpredictable codes                  |
| URL validator  | Added scheme and host checks    | Expanded             | Added private-address and credential rejection             |
| Generator test | Proposed random uniqueness test | Rejected             | Statistical tests can be flaky and cannot prove uniqueness |

## Persistence Test Scope

Repository tests use H2 with Hibernate-managed `create-drop` schema. They validate application persistence behavior, including repository queries, unique constraints, and optimistic-lock version initialization, but do not establish exact PostgreSQL or Flyway migration compatibility. Add PostgreSQL integration tests with Testcontainers later to verify the production dialect, migrations, indexes, and constraints.



| Task              | Copilot suggestion                 | Engineer action | Rationale                                                               |
| ----------------- | ---------------------------------- | --------------- | ----------------------------------------------------------------------- |
| Collision retries | Retry inside one transaction       | Rejected        | PostgreSQL marks the transaction rollback-only after constraint failure |
| Insert writer     | Separate `REQUIRES_NEW` component  | Accepted        | Every collision attempt receives a usable transaction                   |
| Uniqueness        | Check repository before insert     | Edited          | Pre-check is only an optimization; database constraint is authoritative |
| Time handling     | Direct `Instant.now()`             | Edited          | Injected `Clock` makes expiration deterministic and testable            |
| Integrity errors  | Treat every violation as collision | Rejected        | Integrity failures must be classified or propagated                     |



| Copilot suggestion                  | Engineer action | Reason                                       |
| ----------------------------------- | --------------- | -------------------------------------------- |
| Modify V1 to add clicks             | Rejected        | Applied migrations are immutable; created V2 |
| Store complete referrer             | Rejected        | Only hostname is needed                      |
| Store raw IP                        | Rejected        | Used keyed HMAC                              |
| Fire-and-forget executor            | Rejected        | It can silently lose accepted clicks         |
| Add analytics before resolving link | Rejected        | Invalid links must not produce events        |
| Replace redirect implementation     | Edited          | Preserved existing status and headers        |
