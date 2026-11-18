# Support Ticket Management System

Create, track and resolve support tickets, with a backend-enforced lifecycle.
Java 21 / Spring Boot 4 / PostgreSQL or H2 / React + TypeScript.

Built with a spec-driven, AI-assisted workflow: Requirement → Specification → Plan → Implementation → Testing → Review → Fix.
Specs live in [`spec/`](spec), reusable AI instructions in [`.claude/`](.claude), the AI audit trail in [`docs/`](docs).

## Lifecycle (enforced by the backend)
`OPEN → IN_PROGRESS → RESOLVED → CLOSED`, plus `OPEN → CANCELLED` and `IN_PROGRESS → CANCELLED`.
Everything else is rejected with HTTP 409 (e.g. `CLOSED → OPEN`, `RESOLVED → OPEN`, `CANCELLED → OPEN`). See [`spec/state-machine.md`](spec/state-machine.md).

## Prerequisites
Java 21, Node 18+ (Vite 5), and optionally PostgreSQL (verified on 16). Maven is not needed (wrapper included).

## Run it
**Backend** (default: file-based H2, data survives restarts, stored in `backend/data/`, override with `DATA_DIR`):
```bash
cd backend && ./mvnw spring-boot:run          # http://localhost:8080
```
**Backend on PostgreSQL** — credentials come only from environment variables:
```bash
cd backend
cp .env.example .env                          # then edit; .env is git-ignored
set -a; source .env; set +a
./mvnw spring-boot:run -Dspring-boot.run.profiles=postgres
```
Flyway creates the schema on first start.

**Frontend**:
```bash
cd frontend && npm install && npm run dev     # http://localhost:5173 (proxies /api to :8080)
```

## Test it
| What | Command | Count |
|---|---|---|
| Backend unit + integration (JaCoCo gate ≥80% on `service`/`domain`) | `cd backend && ./mvnw clean verify` | 128 |
| Same suite on PostgreSQL | set `SPRING_DATASOURCE_URL/USERNAME/PASSWORD` to a **scratch** database, then `./mvnw clean verify` (tests wipe the DB) | 128 |
| Frontend components (API mocked) | `cd frontend && npm test` | 37 |
| End-to-end in Chromium, real backend, no mocks | start both servers, then `cd e2e && npm install && npx playwright install chromium && npm test` | 12 |

Results and evidence per acceptance criterion: [`docs/acceptance-results.md`](docs/acceptance-results.md).

## API (details in [`spec/api-contract.md`](spec/api-contract.md))
| Method | Path | Purpose |
|---|---|---|
| POST | `/api/tickets` | Create |
| GET | `/api/tickets?q=&status=&page=&size=` | List, keyword search, status filter |
| GET | `/api/tickets/{id}` | Details with comments and `allowedTransitions` |
| PATCH | `/api/tickets/{id}` | Update title, description, priority, assignee |
| POST | `/api/tickets/{id}/transitions` | Change status (`{"targetStatus": "..."}`) |
| POST | `/api/tickets/{id}/comments` | Add comment |

All errors share one body: `timestamp, status, code, message, path, fieldErrors?`.

## Security and configuration
- No secrets are committed. The `postgres` profile has **no default password**; use env vars (`DB_URL`, `DB_USER`, `DB_PASSWORD`). `.env`, `data/` and DB files are git-ignored.
- CORS allows only `FRONTEND_ORIGIN` (default `http://localhost:5173`). The Vite dev proxy drops the `Origin` header so the app also works via `127.0.0.1`.
- No authentication (out of scope, see [`spec/requirements.md`](spec/requirements.md)).

## Repository map
```
spec/        requirements, architecture, data model, API contract, state machine, UI flow, test strategy, tasks
.claude/     rules/ (Spring Boot, testing, API standards), skills/documentation, commands/ (review-code, review-spec, generate-tests)
backend/     Spring Boot service (Flyway migrations, JPA, tests)
frontend/    React + Vite + TypeScript UI (+ component tests)
e2e/         Playwright end-to-end tests
docs/        prompt-history.md, ai-mistakes.md, acceptance-results.md
.specstory/  automatic prompt transcripts
```
