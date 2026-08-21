# Gap #10 — Prompt Composition / Snippets

**Date:** 2026-04-22
**Status:** Stub — needs design work
**Tier:** 2 (P1)
**Edition:** ee
**Parent spec:** [2026-04-21-ai-gateway-gaps-spec.md](2026-04-21-ai-gateway-gaps-spec.md) §5 Tier 2 row 10

## Goal

Reusable prompt snippets (system persona, tone rules, output-format rules, tool instructions) that compose into full prompts via references. Prevents the "we updated the safety preamble in 17 places" failure mode.

## Comparators

- **Langfuse** — prompt references (a prompt can include another).
- **Latitude** — PromptL DSL has `include` / snippet semantics.

## Key Constraints

- Versioned independently of the including prompt — snippet v3 used by prompt X locks to v3, doesn't float.
- Circular references rejected at save time, not render time.
- Render-time variable scope: snippets can access parent variables, but can declare their own required variables which propagate upward.

## Dependencies

- Extends `AiPrompt` schema (new `ai_prompt_snippet` table or reuse `AiPrompt` with a `kind = SNIPPET` discriminator — lean toward discriminator, snippets aren't actually different from prompts that no one calls directly).
- Gap #8 (gateway-side rendering) renders these server-side.
- Gap #11 (playground) needs UX for browsing snippets.

## Open Questions

- DSL: Handlebars-style `{{> my_snippet}}` vs Python-f-string-like `@snippet(my_snippet)`? Pick one and commit.
- Cross-workspace sharing (like a library of "company-standard" snippets) — out of scope or necessary?

## Spec-writing next step

`docs/superpowers/specs/YYYY-MM-DD-ai-gateway-prompt-composition-design.md`.
