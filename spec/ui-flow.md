# UI Flow

React + Vite + TypeScript, React Router. Plain fetch API client, one place for error parsing.

## Screens
| Route | Screen | Contents |
|-------|--------|----------|
| `/` | Ticket list | Search box (debounced), status filter dropdown, table (id, title, status badge, priority, assignee, updated), pagination, "New ticket" button |
| `/tickets/new` | Create form | title, description, priority, assignee; inline field errors |
| `/tickets/:id` | Ticket detail | Fields, edit form (title/description/priority/assignee), status action buttons, comments list, add-comment form |

## Flows
1. **Create**: submit → on 201 navigate to detail; on 400 show field errors beside inputs.
2. **List/search/filter**: changing search or status refetches page 1; empty state message "No tickets match".
3. **Edit**: save → updated values shown; on 409 `TICKET_NOT_EDITABLE` show banner.
4. **Transition**: buttons rendered from `allowedTransitions` returned by the API (no rules copied into the frontend; backend still enforces). On 409 show banner with server message and refresh ticket.
5. **Comment**: submit → appended to list; validation errors inline.

## Error display
- Field errors → inline under field.
- Non-field errors (409, 404, 500) → dismissible banner with server `message`.
- Network failure → "Cannot reach server. Try again." with retry.
- 404 on detail → "Ticket not found" page with link back.
- Loading and submitting states disable buttons.
