# Requirements — Support Ticket Management System

Status: v2 (reviewed at T11; assumptions A1-A5 accepted as written)

## 1. Purpose
Internal tool for creating, tracking and resolving support tickets, with a backend-enforced lifecycle.

## 2. Tech constraints
- Java 21, Spring Boot 4.0.x, Maven
- H2 file-based DB (persists across restarts); PostgreSQL-compatible schema, switchable via profile
- REST/JSON API
- React (Vite + TypeScript) frontend
- No secrets committed; configuration via environment variables / profiles

## 3. Functional requirements

| ID | Requirement |
|----|-------------|
| FR-1 | Create a ticket with title, description, priority, optional assignee. New tickets start as `OPEN`. |
| FR-2 | List tickets (paginated, newest first). |
| FR-3 | View ticket details, including comments. |
| FR-4 | Update title, description, priority, assignee (partial update). Status is NOT changed through this endpoint. |
| FR-5 | Add comments to a ticket (author + body). Comments are append-only. |
| FR-6 | Search tickets by keyword (case-insensitive match on title and description; comments are not searched). |
| FR-7 | Filter tickets by status. Search and filter can be combined. |
| FR-8 | Change status via a dedicated transition endpoint; backend enforces the state machine. |
| FR-9 | Invalid transitions are rejected with HTTP 409 and a clear message. |
| FR-10 | Backend validates all input; errors return a consistent structured body. |
| FR-11 | UI displays meaningful errors (field-level validation, transition rejection, not-found, network). |
| FR-12 | Data persists across application restart. |

## 4. State machine (summary; detail in state-machine.md)
`OPEN → IN_PROGRESS → RESOLVED → CLOSED`; `OPEN → CANCELLED`; `IN_PROGRESS → CANCELLED`.
All other transitions, including same-state, are rejected. `CLOSED` and `CANCELLED` are terminal.

## 5. Non-functional requirements
- NFR-1 Consistent error format across all endpoints.
- NFR-2 Layered architecture (controller / service / repository), DTOs separate from entities.
- NFR-3 Integration tests cover every allowed and disallowed transition.
- NFR-4 No secrets or credentials in the repo; `.gitignore` covers DB files and env files.
- NFR-5 CORS limited to the frontend dev origin.

## 6. Out of scope
Authentication/authorization, file attachments, email notifications, user management (assignee is free-text), comment edit/delete, ticket delete.

## 7. Assumptions (to confirm)
- A1 Assignee is a free-text name/email string, not a user entity.
- A2 Comment author is a free-text name supplied by the client (no auth).
- A3 Terminal-state tickets (`CLOSED`, `CANCELLED`) still allow comments but not field edits. (Accepted, implemented, tested.)
- A4 Priorities: `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`; default `MEDIUM`.
- A5 Same-state transition (e.g. OPEN→OPEN) is rejected.

## 8. Acceptance criteria
1. Ticket can be created from UI.
2. Tickets can be listed.
3. Ticket details can be viewed.
4. Ticket fields can be updated.
5. Assignee can be changed.
6. Comments can be added.
7. Search works.
8. Status filter works.
9. Valid transitions work.
10. Invalid transitions are rejected by backend.
11. Data survives restart.
12. Backend validation works.
13. UI shows meaningful errors.
14. State-machine integration tests pass.
15. No secrets committed.
