# Gap #8 — Gateway-Side Prompt Rendering (`prompt_id + inputs`)

**Date:** 2026-04-22
**Status:** Stub — small scope, ready to plan
**Tier:** 2 (P1)
**Edition:** ee
**Parent spec:** [2026-04-21-ai-gateway-gaps-spec.md](2026-04-21-ai-gateway-gaps-spec.md) §5 Tier 2 row 8

## Goal

Extend `/chat/completions` so a client can send `{ "prompt_id": "...", "inputs": { "question": "..." } }` instead of the full messages array. The gateway resolves the prompt + version, renders variables through `PromptVariableExtractor`, and dispatches. Collapses one round-trip and one point of version drift.

## Comparators

- **Helicone** — prompt template endpoint with variable substitution.
- **Latitude** — first-class "run prompt" API via PromptL DSL.

## Key Constraints

- Prompt resolution order: explicit `version` > `label` (Gap #7) > latest published.
- Render failures (missing required variable) return HTTP 422 with structured error.
- The gateway logs the *rendered* messages in the span (so teams can debug without re-running), but also records `promptId` + `promptVersionId` so you can reconstruct the template.
- Per-request override: client can pass `messages` AND `prompt_id` — messages win, prompt_id goes on the span as metadata only.

## Dependencies

- Reuses `AiPrompt`, `AiPromptVersion`, `PromptVariableExtractor` — already ship.
- Gap #7 (prompt labels) makes this more useful but is not required.
- No conflict with Specs A/B/C.

## Open Questions

- Streaming: can a rendered prompt stream? Yes — same as messages path.
- Do we permit partial messages + `prompt_id` (append to rendered messages)? Sharp edge — leaning no.

## Spec-writing next step

Small. Plan directly: `docs/superpowers/plans/YYYY-MM-DD-ai-gateway-prompt-rendering.md`. ~5–7 tasks.
