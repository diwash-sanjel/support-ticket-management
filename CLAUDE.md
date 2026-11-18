# Support Ticket Management System

Spec-driven project. Workflow: Requirement → Specification → Plan/Tasks → Implementation → Testing → Review → Fix.

## Rules
- Specs in `spec/` are the source of truth. Change the spec before changing behaviour.
- Work one task at a time from `spec/tasks.md`; never "build the whole app" in one go.
- Follow `.claude/rules/` (java-springboot, testing, api-standards) and the `documentation` skill.
- Commands: `/review-spec`, `/review-code`, `/generate-tests`.
- Record every phase in `docs/prompt-history.md`; log AI mistakes in `docs/ai-mistakes.md`.
- No secrets in the repo.

## Run and test
- Backend: `cd backend && ./mvnw clean verify` (128 tests, JaCoCo gate); run with `./mvnw spring-boot:run` (H2 file DB, `DATA_DIR` to relocate) or the `postgres` profile with `DB_URL`, `DB_USER`, `DB_PASSWORD` from the git-ignored `backend/.env` (see `.env.example`). NEVER put a password, even as a default, in any yml.
- Frontend: `cd frontend && npm install && npm run dev` (proxies `/api` to :8080); `npm test`, `npm run build`.
- E2E: start both, then `cd e2e && npm install && npx playwright install chromium && npm test`.

## Stack
Java 21, Spring Boot 4.0, Maven wrapper, H2 file DB + Flyway, React + Vite + TypeScript (Vite 5 / Node 18).
