# API Contract

Base path `/api`. JSON only. Timestamps ISO-8601 UTC.

## Endpoints

| Method | Path | Purpose | Success |
|--------|------|---------|---------|
| POST | /tickets | Create ticket | 201 + Location |
| GET | /tickets?status=&q=&page=&size= | List / filter / search | 200 |
| GET | /tickets/{id} | Details incl. comments | 200 |
| PATCH | /tickets/{id} | Update title, description, priority, assignee | 200 |
| POST | /tickets/{id}/transitions | Change status | 200 |
| POST | /tickets/{id}/comments | Add comment | 201 |

## Schemas

**Text handling**: all string fields are trimmed (Unicode-aware); a blank `assignee` is stored as null; on PATCH a provided `title`/`description` must not be blank. Search covers title and description only (not comments).

**Concurrency**: `version` is informational. Locking is server-side only: simultaneous conflicting writes cannot both apply; the loser gets 409 `CONCURRENT_MODIFICATION` (or `INVALID_STATUS_TRANSITION` if it re-reads the new state). Clients do not send a version.

**CreateTicketRequest**: `title` (req, 1–120, not blank), `description` (req, 1–5000), `priority` (opt, default MEDIUM), `assignee` (opt, ≤100).

**UpdateTicketRequest** (all optional, at least one required): `title`, `description`, `priority`, `assignee` (omitted/null = unchanged; empty or blank string clears the assignee). Unknown fields (e.g. `status`) → 400.

**TransitionRequest**: `targetStatus` (req, enum).

**CreateCommentRequest**: `author` (req, ≤100), `body` (req, 1–2000).

**TicketSummary**: id, title, status, priority, assignee, createdAt, updatedAt.

**TicketDetail**: TicketSummary + description, version, `allowedTransitions[]` (statuses reachable from the current one, lifecycle order; empty for CLOSED/CANCELLED; the UI renders its transition buttons from this and never duplicates the rules), comments[].

**Comment**: id, author, body, createdAt.

**Page**: `{ content: [TicketSummary], page, size, totalElements, totalPages }`. Default size 20, max 100; sort createdAt desc.

## List query rules
- `status` must be a valid enum, else 400.
- `q` is trimmed; blank is ignored; max 100 chars.
- `status` and `q` combine with AND.

## Error format (all errors)
```json
{
  "timestamp": "2026-11-18T10:00:00Z",
  "status": 409,
  "code": "INVALID_STATUS_TRANSITION",
  "message": "Cannot transition from CLOSED to OPEN. Allowed: none",
  "path": "/api/tickets/5/transitions",
  "fieldErrors": [ { "field": "title", "message": "must not be blank" } ]
}
```
`fieldErrors` present only for validation errors.

| HTTP | code | When |
|------|------|------|
| 400 | VALIDATION_ERROR | Bean validation, bad enum, malformed JSON, unknown field, empty PATCH |
| 404 | TICKET_NOT_FOUND | Unknown id |
| 409 | INVALID_STATUS_TRANSITION | Illegal transition |
| 409 | TICKET_NOT_EDITABLE | Field edit on CLOSED/CANCELLED |
| 409 | CONCURRENT_MODIFICATION | Optimistic lock failure |
| 400 | VALIDATION_ERROR | also: unknown JSON field (fieldErrors names it, message "Unknown field"), invalid enum value (fieldErrors names the field and lists allowed values), malformed JSON ("Malformed JSON request body"), invalid query param |
| 404 | NOT_FOUND | Unknown path |
| 405 | METHOD_NOT_ALLOWED | Wrong HTTP method |
| 415 | UNSUPPORTED_MEDIA_TYPE | Body not `application/json` (or no Content-Type) |
| 500 | INTERNAL_ERROR | Unexpected; no stack trace leaked |
