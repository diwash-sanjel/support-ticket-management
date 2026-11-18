# Data Model

## ticket
| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGINT | PK, generated identity |
| title | VARCHAR(120) | NOT NULL |
| description | VARCHAR(5000) | NOT NULL |
| status | VARCHAR(20) | NOT NULL, default `OPEN` (stored as string enum) |
| priority | VARCHAR(20) | NOT NULL, default `MEDIUM` |
| assignee | VARCHAR(100) | NULL |
| version | BIGINT | NOT NULL (optimistic lock) |
| created_at | TIMESTAMPTZ | NOT NULL |
| updated_at | TIMESTAMPTZ | NOT NULL |

Indexes: `status`, `created_at DESC`.

## ticket_comment
| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGINT | PK, generated identity |
| ticket_id | BIGINT | NOT NULL, FK → ticket(id) |
| author | VARCHAR(100) | NOT NULL |
| body | VARCHAR(2000) | NOT NULL |
| created_at | TIMESTAMPTZ | NOT NULL |

Index: `ticket_id`.

## Enums
- `TicketStatus`: OPEN, IN_PROGRESS, RESOLVED, CLOSED, CANCELLED
- `TicketPriority`: LOW, MEDIUM, HIGH, CRITICAL

## Notes
- Schema managed by Flyway migrations (`V1__init.sql`); Hibernate `ddl-auto=validate`.
- Enums stored with `@Enumerated(EnumType.STRING)` (never ordinal).
- H2 file DB at `${DATA_DIR:./data}/tickets` (git-ignored; set `DATA_DIR` to an absolute path so it does not depend on the working directory), PostgreSQL mode for compatibility.
- Search: `LOWER(title) LIKE %kw% OR LOWER(description) LIKE %kw%` with escaped wildcards.
- Timestamps in UTC (`Instant`), stored as `TIMESTAMP WITH TIME ZONE`.
