# Prompt History

Full automatic transcripts live in `.specstory/history/`. This file is a curated index of prompts and outcomes.

| # | Phase | Prompt (summary) | Outcome |
|---|-------|------------------|---------|
| 1 | Requirement | Paste of SE/SSE assignment; "understand the requirement and give your understanding" | Summary of goals, deliverables and open questions |
| 2 | Specification | "Follow the workflow in order. Start from Requirement then specification." | Created `spec/` (requirements, state-machine, data-model, api-contract, architecture, ui-flow, test-strategy) |
| 3 | Plan / Tasks | "Proceed to next phase" | Created steering files in `.claude/` (3 rules, documentation skill, 3 commands), `CLAUDE.md`, and `spec/tasks.md` (T1–T20) |
| 4 | Implementation (Phase A) | "Proceed to next phase" | T1–T4 done: Spring Boot 4.0.9 scaffold, `TicketStatus` state machine (27 tests), Flyway V1 + entities, global error handling (6 tests). 4 AI mistakes logged in `docs/ai-mistakes.md` |
| 5 | Implementation (Phase B) | "proceed to next phase" | T5–T10 done: create/get, list+search+filter, PATCH, transitions, comments, CORS, restart persistence. 117 backend tests green; real-app smoke test. Mistakes M5–M7 logged |
| 6 | Review (T11) | "proceed but in each phase test fully before moving to next one" | Ran `/review-spec` and `/review-code`: 9 spec findings, 11 code findings. Fixed (tests first): 415 bug, specific field errors, `transitionTo` invariant, `allowedTransitions`, strip(), TIMESTAMPTZ, JaCoCo gate (80% service/domain), real race tests. 128 tests green. Mistakes M8–M10 logged |
| 7 | Verification | "create a database and test against it and once the test is successful then proceed to next phase" | Existing local Postgres needs a password and has no role for the user, so no guessing. Used a disposable PG 16 cluster instead: Flyway + validate OK, 21 HTTP checks, restart persistence, all 128 tests green on Postgres |
| 8 | Frontend (Phase D) | "proceed to next phase" | T12–T17 done: Vite 5 + React + TS app (list/search/filter/paging, create, detail with transitions/edit/comments, unified error handling). 34 component tests, 10 Playwright E2E tests against the real stack, build + typecheck clean. Mistakes M11–M13 logged (incl. a CORS bug found only by E2E) |
| 9 | Verification on real DB + finish (Phase E) | "I have updated the database credentials in yml file. proceed to next phase and test with actual database connection" | Connected to the real local PostgreSQL 16 (created databases `tickets`, `tickets_test`). Found the yml invalid and the password hard-coded: moved secrets to git-ignored `.env`. On the real DB: 12/12 E2E, restart persistence verified, 128/128 backend tests. T19: secrets scan clean; final review found 2 UX bugs, fixed test-first (37 component tests). T20: README, `docs/acceptance-results.md`. M14 logged |
