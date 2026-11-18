# Acceptance Results

Verified 2026-11-18 on the **actual PostgreSQL 16 server** (database `tickets` for the running app, `tickets_test` for the automated suite), not only H2.

| # | Criterion | Evidence | Result |
|---|---|---|---|
| 1 | Ticket can be created from UI | E2E `create a ticket from the UI` (real Chromium, real PostgreSQL) | PASS |
| 2 | Tickets can be listed | E2E list/search test; backend `TicketListSearchTest` (11) | PASS |
| 3 | Ticket details can be viewed | E2E create test asserts details; `TicketCreateAndGetTest` | PASS |
| 4 | Ticket fields can be updated | E2E `edit fields`; `TicketUpdateTest` (13) | PASS |
| 5 | Assignee can be changed | E2E changes then clears assignee, then reloads to prove it persisted | PASS |
| 6 | Comments can be added | E2E `add comments` incl. reload; `TicketCommentTest` (9) | PASS |
| 7 | Search works | E2E (case-insensitive, literal `%`); backend wildcard-escape tests | PASS |
| 8 | Status filter works | E2E filter test; `TicketListSearchTest.filtersByStatus` | PASS |
| 9 | Valid transitions work | E2E walks OPEN→IN_PROGRESS→RESOLVED→CLOSED; backend all 5 allowed pairs | PASS |
| 10 | Invalid transitions rejected by backend | Backend parameterised over all 20 forbidden pairs (409 + status unchanged); E2E direct API call; E2E stale page gets the server message | PASS |
| 11 | Data survives restart | Backend stopped and restarted against PostgreSQL: marker ticket byte-identical, count unchanged (8), no migration re-run; plus `PersistenceAcrossRestartTest` | PASS |
| 12 | Backend validation works | `RequestFormatErrorsTest`, create/update/comment validation tests, E2E field errors | PASS |
| 13 | UI shows meaningful errors | 37 component tests + E2E: inline field errors, server 409 message, not-found, malformed URL, server unreachable with retry | PASS |
| 14 | State-machine integration tests pass | `TicketTransitionTest` (32 cases) + `TicketStatusTest` (27) + race tests | PASS |
| 15 | No secrets committed | Scan of all 99 committable files: the real DB password appears nowhere; `.env` and build output git-ignored; only `.env.example` (placeholder) is committable. Nothing is committed yet; re-run the scan before the first commit | PASS |

## Totals
| Suite | Result |
|---|---|
| Backend on PostgreSQL (`tickets_test`) | 128 / 128, JaCoCo gate met |
| Backend on H2 | 128 / 128 |
| Frontend components | 37 / 37 |
| End-to-end (Chromium, real stack on PostgreSQL) | 12 / 12 |

## Known limitations (honest list)
- No authentication; assignee and comment author are free text (spec out of scope / assumptions A1, A2).
- Optimistic locking is server-side only; the UI does not send an expected version (spec decision).
- The E2E suite assumes the stack is already running; it does not start the servers itself.
- Single-origin CORS (`FRONTEND_ORIGIN`); a multi-origin production setup needs a list.
