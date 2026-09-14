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
