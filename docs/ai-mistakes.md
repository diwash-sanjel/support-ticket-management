# AI Mistakes Log

Real mistakes or unsafe suggestions from AI during this project, how they were caught and fixed.

## M1 — Outdated framework assumption in the spec
- **Context**: Writing `spec/requirements.md` and `architecture.md`.
- **AI output**: Specified "Spring Boot 3.x" from training-time knowledge.
- **How detected**: Spring Initializr metadata (queried at T1) only offers 4.0.x / 4.1.x.
- **Fix**: Moved to Spring Boot 4.0.9; updated specs, rules and CLAUDE.md. Package moves in Boot 4 (e.g. `org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc`, Jackson 3) handled at compile time.
- **Lesson**: Verify versions against the live source before writing them into a spec.

## M2 — Generated build file referenced a non-existent parent version
- **Context**: T1 scaffold from Initializr with `bootVersion=4.0.9.RELEASE`.
- **AI output**: `pom.xml` parent `4.0.9.RELEASE`; Maven failed: artifact not found.
- **How detected**: First `./mvnw verify` failed with "Non-resolvable parent POM".
- **Fix**: Checked `maven-metadata.xml` on Maven Central; real version is `4.0.9`. Corrected.
- **Lesson**: Run the build immediately after scaffolding; don't trust generated coordinates.

## M3 — Unneeded dependency with security surface
- **Context**: Initializr "h2" dependency also pulled in `spring-boot-h2console`.
- **AI output**: Included the H2 web console, which is not in the requirements.
- **How detected**: Review of the generated `pom.xml` against requirements.
- **Fix**: Removed it.
- **Lesson**: Review generated dependency lists; remove anything not required.

## M4 — Test controller nested in test class was never registered
- **Context**: T4 error-handling tests.
- **AI output**: A `@RestController` declared as a static nested class inside the `@SpringBootTest` class.
- **How detected**: 5 of 6 tests failed (404 instead of expected codes); the one passing test was the unknown-path case, which would have passed regardless — a false-positive risk.
- **Fix**: Moved the controller to a top-level test class so component scan finds it.
- **Lesson**: A green test proves nothing unless it can fail; check why failures happen before changing assertions (the handler was correct, the test setup was wrong).

## M5 — Test config silently shadowed the main configuration
- **Context**: T4 added `src/test/resources/application.yml` to point tests at in-memory H2.
- **AI output**: A test-side `application.yml` with the same name as the main one. Classpath resolution picks the test file first, so the main file (`ddl-auto: validate`, `fail-on-unknown-properties`, CORS origin) was never loaded in any test.
- **How detected**: In T10, `WebConfig` needed `app.cors.allowed-origin` and every test failed with "Could not resolve placeholder". Until then the tests passed while testing the wrong configuration.
- **Fix**: Replaced it with `src/test/resources/application.properties`, which merges with the main YAML. Also needed `mvn clean`, because stale copies of deleted resources stayed in `target/`.
- **Lesson**: Override config with a differently named file or profile, never a same-named file. A passing suite can hide missing config, so smoke-test the real app too (done at T10: create, rejected transition, unknown field).

## M6 — Spec/behaviour mismatch on a detail the AI invented
- **Context**: T8 transition tests.
- **AI output**: The test asserted the "Allowed:" list in alphabetical order, but the implementation lists targets in lifecycle (enum) order. The spec only said the message "includes allowed targets".
- **How detected**: One failing test out of 117.
- **Fix**: Spec was silent, so lifecycle order was kept (more natural for users), the spec now states it, and the test was corrected. Not a case of weakening an assertion: the spec was updated first.
- **Lesson**: Pin down output formats in the spec before writing assertions.

## M7 — Spec contradicted itself on PATCH null semantics
- **Context**: T7 design. `api-contract.md` said "null clears assignee" while also saying PATCH fields are optional, i.e. absent/null means unchanged. A Java record cannot distinguish the two.
- **How detected**: While writing `UpdateTicketRequest`, before any test ran.
- **Fix**: Spec amended: omitted/null = unchanged, empty or blank string clears.
- **Lesson**: `/review-spec` should be run before implementation to catch such conflicts.

## M8 — Review found a real defect the AI had shipped: wrong Content-Type returned 500
- **Context**: T11 `/review-code`. The catch-all `@ExceptionHandler(Exception.class)` was added in T4 as a safety net.
- **AI output**: Framework exceptions with no dedicated handler (e.g. `HttpMediaTypeNotSupportedException`) fell into the catch-all, so a POST with `Content-Type: text/plain` or none returned **500 INTERNAL_ERROR**; confirmed by running the built jar with curl. The spec reserves 500 for genuinely unexpected errors.
- **How detected**: Reading the handler during review, then proving it with the real app, then a failing test (red before the fix).
- **Fix**: Added 415 and missing-parameter handlers. While there, made the vague "Malformed request body" message specific (field name plus allowed enum values for invalid enums, "Unknown field" for unknown properties), and truncated echoed values.
- **Lesson**: A catch-all handler hides missing handlers; test odd HTTP inputs, not just valid and invalid JSON.

## M9 — My concurrency test asserted an invariant that is not true
- **Context**: T11, new race test for OPEN->IN_PROGRESS vs OPEN->CANCELLED, asserting "exactly one success".
- **AI output**: Failed in round 4. The code was right: if CANCELLED runs after IN_PROGRESS commits, IN_PROGRESS->CANCELLED is a legal chain, so both succeed.
- **How detected**: Intermittent failure; reasoned from the state machine rather than loosening the assertion blindly.
- **Fix**: Split into (a) identical racing transitions: exactly one winner and one 409, and (b) different transitions: consistency only (no 500s, final status matches responses). Repeated 5x for flakiness.
- **Lesson**: Derive concurrency invariants from the spec's rules, including legal sequential chains.

## M10 — Weak assertions and design gaps found in review
- `updatedAt` checked with `isAfterOrEqualTo`, which passes even if never updated. Fixed by backdating the row via JDBC and asserting strictly after.
- Two tests were tautological or redundant (`specCountsAre...` tested the test helper; `namedExamples...` duplicated the parameterised suite). Removed.
- A public `setStatus` could bypass the state machine. Replaced by `Ticket.transitionTo`; tests seed states via JDBC.
- UI had no way to know allowed transitions without copying the rules; `allowedTransitions` added to the API (ordered via `EnumSet`; the earlier `Set.of` had non-deterministic order).
- `trim()` vs Unicode whitespace; now `strip()`, with a test.
- `TIMESTAMP` vs `Instant` risk on PostgreSQL; changed to `TIMESTAMP WITH TIME ZONE`. Verified on real PostgreSQL 16 afterwards (see `spec/test-strategy.md`, "PostgreSQL verification").

## M11 — `beforeEach` returned the mock, so vitest called it as a cleanup hook
- **Context**: T13-T16 component tests, `beforeEach(() => createTicket.mockReset())`.
- **AI output**: A concise arrow whose value is the mock function. Vitest treats a function returned from `beforeEach` as a cleanup callback and invokes it after each test, which called the mock (returning a rejected promise nobody awaited). Symptoms were misleading: tests failed with the *mocked* `ApiError` as the failure message, and one test hung.
- **How detected**: Cost about an hour of bisecting. `mockReset` vs `mockClear` made no difference, and a copy of the file with the `beforeEach` line removed passed, which pointed at the hook itself.
- **Fix**: Block body, `beforeEach(() => { mock.mockReset(); })`, in all three test files; rule added to `.claude/rules/testing.md`.
- **Lesson**: When a failure message is an error you yourself injected, suspect the test harness, not the component.

## M12 — Strict CORS broke the app on 127.0.0.1; only the real-browser E2E could find it
- **Context**: T11 set backend CORS to a single origin `http://localhost:5173` (spec NFR-5) and 34 mocked component tests passed.
- **AI output**: With the dev app opened at `http://127.0.0.1:5173`, every POST/PATCH returned `403 Invalid CORS request` (plain text, not our JSON error), while GETs worked because browsers send `Origin` only on non-GET same-origin requests. The UI showed "Unexpected response from server (HTTP 403)".
- **How detected**: 6 of 10 Playwright E2E tests failed at "create ticket"; reproduced with `curl -H "Origin: http://127.0.0.1:5173"`.
- **Fix**: Kept backend CORS strict (a foreign origin is still rejected, verified). The Vite dev proxy now strips the `Origin` header, since in dev the browser only talks to Vite.
- **Lesson**: Mocked tests can pass while the integrated system is broken; run the real stack before closing a UI task.

## M13 — Test bugs found while stabilising the UI tests
- Two assertions used ambiguous queries (`getByText('IN PROGRESS')` also matched the filter `<option>`; the description text matched both the paragraph and the textarea). The components were right; queries were scoped (`within(table)`, heading role).
- The E2E "stale page" test was flaky (passed, failed, then passed 4/4 alone). Root cause (inferred, the failing trace was not captured): the test changed server state before the page had finished loading, so there was nothing stale. Fixed by waiting for the OPEN buttons first; 3 consecutive full runs green afterwards. The 38 s duration of the failing run matches a 30 s click timeout.

## M14 — Two UX bugs the AI shipped, found by re-reading the finished frontend
- **Context**: T19 final `/review-code` of the frontend (all 34 component tests and 10 E2E tests were green at the time).
- **AI output**: (1) The edit form was keyed on the ticket `version`, so clicking a status button (which bumps the version) remounted the form and **silently discarded unsaved typing**. (2) `/tickets/abc` made the backend answer 400, and the page showed a retry banner reading "Validation failed" instead of a not-found page.
- **How detected**: Review, not tests: green suites did not cover either path. Both reproduced as failing tests first (2 red, then green), and the E2E suite gained two real-UI tests.
- **Fix**: Form is remounted only when the saved field values change; a 400 on the detail endpoint renders the not-found page.
- **Lesson**: Passing tests prove only what they cover; review for user-visible data loss and for error messages that make no sense to a person.

## Security note (not an AI mistake, recorded for the audit trail)
When the database was switched to a real PostgreSQL, the credentials were first placed as a default value in the tracked `application-postgres.yml`. That would have broken acceptance criterion 15 on the first commit, and the line was also invalid YAML (`${DB_USER: postgres}`: colon-space inside an unquoted value stops the app from starting). Resolved by moving the values to a git-ignored `backend/.env`, committing only `.env.example`, removing any password default from the yml, and scanning all committable files for the real value (none found).
