# Gap #11 — Playground / Prompt IDE

**Date:** 2026-04-22
**Status:** Stub — large UI scope
**Tier:** 2 (P1)
**Edition:** ee
**Parent spec:** [2026-04-21-ai-gateway-gaps-spec.md](2026-04-21-ai-gateway-gaps-spec.md) §5 Tier 2 row 11

## Goal

In-product UI for editing prompts, running them against models, comparing outputs side-by-side across models/versions, and publishing to an environment label. Replaces the "copy-paste into an OpenAI playground" workflow with a tool that has ByteChef's cost/latency/eval context one click away.

## Comparators

- **Langfuse** — prompt playground with model picker + diff.
- **Latitude** — full prompt IDE with agent primitives.

## Key Constraints

- Must reuse existing gateway routing (so playground runs respect provider budgets, rate limits, and show real cost).
- Playground runs create traces flagged `source = PLAYGROUND` so they can be filtered out of analytics.
- Streaming output must feel native (incremental tokens, not batch).
- Diff view: token-level diff when two runs share a prefix, otherwise full side-by-side.

## Dependencies

- Gap #7 (labels) — needed for "promote this version to production" button.
- Gap #8 (gateway-side rendering) — needed for rendering prompts with input forms.
- Gap #10 (composition) — nice to have; without it, snippets are just prompts.
- Large frontend work — React 19 + Tailwind, per CLAUDE.md conventions.

## Open Questions

- Do we support non-chat models (completion, embedding, image) or scope to chat?
- Collaborative editing (Google-Docs-style) or single-author with locking?
- Export: JSON for version-control commit, or markdown for docs?

## Spec-writing next step

`docs/superpowers/specs/YYYY-MM-DD-ai-gateway-playground-design.md`. Expect this to split into multiple PRs (backend API, frontend shell, diff viewer, streaming).
