---
name: documentation
description: Keep project docs in sync with code and specs — README, spec/ updates, prompt history and AI-mistakes log. Use after completing a task, changing behaviour, or when the user asks to document something.
---

# Documentation skill

## When code or behaviour changes
1. Update the relevant `spec/*.md` FIRST if the contract, model or state machine changes; then code.
2. Keep `README.md` accurate: prerequisites, how to run backend/frontend/tests, config and env vars.
3. Javadoc only for public service methods whose behaviour isn't obvious; explain *why*, not *what*.

## Prompt history
After each phase, append a row to `docs/prompt-history.md`: `# | Phase | Prompt summary | Outcome`. Raw transcripts are in `.specstory/history/`.

## AI mistakes log (`docs/ai-mistakes.md`)
Whenever AI output is wrong, unsafe or needed correction, append an entry:
- **Context**: what was asked
- **AI output**: what was wrong
- **How detected**: review / test / manual
- **Fix**: what changed
- **Lesson**: guideline or rule updated (if any)
Only log real mistakes; never fabricate.

## Style
Concise, factual, tables over prose, no marketing language. Link to spec files instead of duplicating them.
