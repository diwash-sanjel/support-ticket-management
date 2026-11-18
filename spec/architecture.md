# Architecture

## Overview
```
React (Vite, TS)  --HTTP/JSON-->  Spring Boot REST API  --JPA-->  H2 (file) / PostgreSQL
```

## Repo layout
```
backend/   Spring Boot (Maven wrapper, Java 21)
frontend/  React + Vite + TypeScript
spec/      specifications
docs/      prompt-history.md, ai-mistakes.md
```

## Backend packages (`com.tothenew.tickets`)
- `web` — controllers, request/response DTOs, `GlobalExceptionHandler`
- `service` — `TicketService` (business rules, state machine enforcement), `CommentService`
- `domain` — `Ticket`, `TicketComment`, `TicketStatus`, `TicketPriority`
- `repository` — Spring Data JPA, `TicketSpecifications` for search/filter
- `exception` — domain exceptions

## Key decisions
| # | Decision | Rationale |
|---|----------|-----------|
| 1 | Layered architecture, DTOs separate from entities | No entity leakage, stable contract |
| 2 | Transition rules inside `TicketStatus` enum | Single source of truth, trivially unit-testable |
| 3 | Dedicated transition endpoint, not PATCH | Explicit intent; keeps field edits separate from lifecycle |
| 4 | H2 file DB + Flyway, Postgres via profile | Zero-setup persistence, production-compatible schema |
| 5 | Optimistic locking (`@Version`) | Prevents lost updates / racing transitions |
| 6 | JPA Specifications for search+filter | Composable, parameterised (no SQL injection) |
| 7 | Constructor injection, records for DTOs, `@Transactional` at service | Standard Spring practice |
| 8 | Frontend dev proxy via Vite to `/api` | Avoids CORS in dev; CORS still restricted in backend |

## Config & secrets
- `application.yml` holds non-secret defaults only.
- `postgres` profile reads `DB_URL`, `DB_USER`, `DB_PASSWORD` from env.
- `.env`, `data/`, `*.db` git-ignored.

## Testing layers
Unit (state machine, service) → `@WebMvcTest`/`@SpringBootTest` + MockMvc integration → frontend component tests. See test-strategy.md.
