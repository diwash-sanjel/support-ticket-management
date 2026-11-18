# Test Strategy

## Levels
| Level | Tooling | Scope |
|-------|---------|-------|
| Unit | JUnit 5, AssertJ | `TicketStatus` transitions, service rules |
| Integration | `@SpringBootTest` + MockMvc + H2 | Full API through DB |
| Frontend | Vitest + React Testing Library | Forms, error display, list filters |
| Manual E2E | Browser | Acceptance checklist, restart persistence |

## State-machine tests (mandatory, AC 14)
- Parameterised unit test over all 25 from/to pairs against the table in state-machine.md (5 pass, 20 fail).
- Integration test per allowed transition: `POST /transitions` → 200, status updated.
- Integration test per disallowed transition from each state → 409 `INVALID_STATUS_TRANSITION`, status unchanged in DB.
- Named cases: CLOSED→OPEN, RESOLVED→OPEN, CANCELLED→OPEN, OPEN→RESOLVED, OPEN→CLOSED.
- Unknown target status → 400. Unknown ticket → 404.

## Traceability
| Requirement | Tests |
|-------------|-------|
| FR-1, FR-10 | create valid/invalid (blank title, oversize, bad priority) |
| FR-2, FR-3 | list pagination, detail with comments, 404 |
| FR-4 | partial update, empty PATCH 400, `status` in PATCH 400, terminal-state 409 |
| FR-5 | add comment valid/invalid, comment on missing ticket |
| FR-6, FR-7 | keyword case-insensitive, wildcard chars escaped, status filter, combined |
| FR-8, FR-9 | state-machine suite above |
| FR-12 | restart check: manual script + file-DB profile test |
| NFR-1 | every error asserts the common error body |

## Rules
- Tests use H2 in-memory (isolated); no dependency on dev data.
- Each test independent; clean DB per test.
- AI-generated tests must be reviewed to ensure they assert behaviour, not just call code (see docs/ai-mistakes.md).
- Target ≥80% line coverage on service and domain packages (JaCoCo).

## PostgreSQL verification (done once at T11, repeatable)
Run against a throwaway PostgreSQL 16 instance, never a shared server. Credentials come from environment variables only; nothing is committed.
1. Disposable cluster: `initdb -D <tmp> -U <user> --pwfile=<file> --auth=scram-sha-256`, start with `pg_ctl -o "-p 55432 -c listen_addresses=127.0.0.1"`, then `createdb tickets_it`.
2. App: `DB_URL=jdbc:postgresql://127.0.0.1:55432/tickets_it DB_USER=... DB_PASSWORD=... java -jar backend/target/tickets-*.jar --spring.profiles.active=postgres`.
3. Full automated suite on Postgres: set `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, run `./mvnw clean verify` (env overrides the in-memory H2 test URL; tests wipe the DB).

Results (2026-11-18): Flyway V1 applied and Hibernate `validate` passed. 21 manual HTTP checks passed: create, list order, paging, case-insensitive search, literal `%` and `_` with `LIKE ... ESCAPE`, PATCH, assignee clear, full lifecycle, invalid transitions 409, terminal-edit 409, comment on CLOSED, status filter, validation, 415, 404. Data and comments survived an app restart with no re-migration. `created_at`/`updated_at` are `timestamp with time zone`. All 128 automated tests, including the race tests, passed under `PostgreSQLDialect`.

## Frontend and end-to-end tests (T12-T17)
- **Unit/component** (`frontend/`, Vitest + React Testing Library, API mocked): 34 tests covering the API client error parsing, list (search debounce, status filter, paging, stale-response guard, retry), create form (field errors, banner, double-submit), detail (transition buttons from `allowedTransitions`, rejected transition, terminal ticket, edit diff, clear assignee, 404, comments). Run: `cd frontend && npm test`.
- **End-to-end** (`e2e/`, Playwright + Chromium against the real backend and Vite dev server, no mocks): 10 tests covering acceptance criteria 1-10, 12 and 13, including a genuinely stale page attempting an invalid transition, a not-found page, and a simulated unreachable server. Run with backend on :8080 and `npm run dev` on :5173, then `cd e2e && npm test`.
- Mocked tests cannot find integration problems; the E2E run found a real one (see docs/ai-mistakes.md M12).
