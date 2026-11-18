---
description: Review code changes against specs and project rules
---

Review the current changes (`git diff` plus untracked files), or `$ARGUMENTS` if a path is given.

Check against `.claude/rules/*.md` and `spec/*.md`:
1. **Spec conformance**: endpoints, status codes, error codes, state machine match the spec exactly.
2. **Correctness**: edge cases, null handling, transaction boundaries, concurrency.
3. **Security**: injection, secrets, leaked internals, CORS, input validation.
4. **Layering**: no entities in responses, no logic in controllers, state rules only in `TicketStatus`.
5. **Tests**: every changed behaviour covered; assertions meaningful.
6. **Quality**: dead code, duplication, naming.

Output a table: `Severity (High/Med/Low) | File:line | Issue | Suggested fix`. Do not edit code unless asked. Report problems found; if none, say so.
