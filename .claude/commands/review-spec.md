---
description: Review specification files for gaps, contradictions and ambiguity
---

Review `spec/` (or `$ARGUMENTS`).

Check:
1. **Completeness**: every FR/NFR and acceptance criterion maps to API, data model, UI and a test.
2. **Consistency**: names, enums, status codes, field limits agree across all files.
3. **State machine**: matrix, diagram and tests table agree; terminal states and same-state cases defined.
4. **Ambiguity**: undefined behaviour (pagination limits, empty/blank input, concurrent edits, terminal-state edits).
5. **Testability**: each requirement can be verified by a concrete test.
6. **Open assumptions**: list those still needing a decision.

Output: table `Severity | File | Finding | Recommendation`, then a list of open decisions. Do not edit files unless asked.
