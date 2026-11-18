# API Standards

Source of truth: `spec/api-contract.md`. Change the spec first, then code.

- Base path `/api`; plural nouns; JSON only; ISO-8601 UTC timestamps; camelCase fields.
- Status codes: 201 + `Location` on create, 200 on read/update, 400 validation, 404 missing, 409 business-rule/state conflict, 500 unexpected.
- Pagination: `page` (0-based), `size` (default 20, max 100); response is the `Page` shape from the contract.
- PATCH is partial, rejects unknown fields (`FAIL_ON_UNKNOWN_PROPERTIES=true`), and rejects an empty body.
- Lifecycle changes use action endpoints (`/transitions`), never PATCH.
- All errors use the single error body (`timestamp, status, code, message, path, fieldErrors?`). Codes are stable constants.
- Validate at the boundary; trim strings; reject blank values.
- No internal details (stack traces, SQL, class names) in error messages.
- CORS: only the configured frontend origin.
