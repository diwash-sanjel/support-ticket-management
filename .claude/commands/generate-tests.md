---
description: Generate tests for a class or feature from the spec
---

Generate tests for `$ARGUMENTS` following `.claude/rules/testing.md`.

Steps:
1. Read the relevant section of `spec/` (state-machine, api-contract, test-strategy) — derive expected behaviour from the SPEC, not from the implementation.
2. List the test cases (happy path, validation failures, boundaries, error codes) before writing code.
3. Write the tests: behaviour-named, Arrange/Act/Assert, assert response body AND persisted state.
4. Run them (`cd backend && ./mvnw test` or `cd frontend && npm test`). Report pass/fail honestly.
5. If a test fails, determine whether code or spec is wrong; do not weaken assertions just to pass.
6. Note which traceability rows in `spec/test-strategy.md` are now covered.
