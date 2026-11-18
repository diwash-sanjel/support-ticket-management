# Plan / Tasks

Each task: small, verifiable, with a done-check. Complete in order; run tests and commit after each. Status: ☐ todo, ☑ done.

## Phase A — Backend foundation
- ☑ **T1** Scaffold Spring Boot 4.0 / Java 21 in `backend/` (web, validation, data-jpa, flyway, h2, postgresql driver, test). Maven wrapper. `.gitignore`. *Done: `./mvnw verify` passes, app starts.*
- ☑ **T2** Domain: `TicketStatus` (with transitions), `TicketPriority`. *Done: unit test over all 25 pairs passes (spec/state-machine.md).*
- ☑ **T3** Flyway `V1__init.sql`, entities `Ticket`, `TicketComment`, repositories. *Done: context loads with `ddl-auto=validate`.*
- ☑ **T4** Error handling: error body, exceptions, `GlobalExceptionHandler`. *Done: tests for 400/404/409/500 shapes.*

## Phase B — Backend features
- ☑ **T5** Create + get ticket (DTOs, validation, service, controller). *Done: FR-1, FR-3, FR-10 tests.*
- ☑ **T6** List with pagination, search, status filter (Specifications). *Done: FR-2, FR-6, FR-7 tests incl. wildcard escaping.*
- ☑ **T7** Update ticket (PATCH rules, terminal-state lock, unknown fields). *Done: FR-4 tests.*
- ☑ **T8** Status transitions endpoint + optimistic locking. *Done: full state-machine integration suite (AC 14).*
- ☑ **T9** Comments endpoint. *Done: FR-5 tests.*
- ☑ **T10** CORS config, file-DB profile, persistence-after-restart check. *Done: FR-12 verified.*

## Phase C — Review checkpoint 1
- ☑ **T11** `/review-code` on backend, `/review-spec` drift check; fix findings; log mistakes in `docs/ai-mistakes.md`.

## Phase D — Frontend
- ☑ **T12** Scaffold Vite 5 + React + TS, router, API client with unified error parsing, proxy to backend.
- ☑ **T13** Ticket list: search (debounced), status filter, pagination, empty/loading/error states.
- ☑ **T14** Create-ticket form with inline field errors.
- ☑ **T15** Detail page: edit form, transition buttons, error banner, 404 page.
- ☑ **T16** Comments list + form.
- ☑ **T17** Frontend tests (forms, error display, filters).

## Phase E — Finish
- ☑ **T18** End-to-end run of all 15 acceptance criteria incl. restart persistence. (Playwright suite in `e2e/` already covers the UI criteria; restart persistence + secrets scan remain.)
- ☑ **T19** Final `/review-code` + secrets scan; fix findings.
- ☑ **T20** README, finalize `docs/prompt-history.md` and `docs/ai-mistakes.md`.

## Dependencies
T1→T2→T3→T4→T5→(T6,T7,T8,T9)→T10→T11→T12→T13–T16→T17→T18→T19→T20. T8 needs T2 and T7.

## Token optimisation
Work task-by-task; read only the spec file relevant to the task; use `/caveman` style brevity in routine exchanges; consider a codebase-memory MCP once the code base grows.
