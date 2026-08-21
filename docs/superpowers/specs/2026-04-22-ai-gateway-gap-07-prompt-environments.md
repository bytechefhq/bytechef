# Gap #7 — Prompt Environments / Labels

**Date:** 2026-04-22
**Status:** Stub — small scope, ready to plan
**Tier:** 2 (P1)
**Edition:** ee
**Parent spec:** [2026-04-21-ai-gateway-gaps-spec.md](2026-04-21-ai-gateway-gaps-spec.md) §5 Tier 2 row 7

## Goal

Let each `AiPromptVersion` carry zero-to-many environment labels (e.g. `production`, `staging`, `dev`, `experiment-v2`). Clients fetch prompts by `name + label` instead of `name + version`, letting product teams promote a version without redeploying clients.

## Comparators

- **Langfuse** — labels on prompt versions, `production` / `latest` are reserved.
- **Helicone** — environment-tagged prompt versions.

## Key Constraints

- Labels are unique per prompt (one version = one label at a time for a given label name). Promoting moves the label.
- `production` is reserved — can only be assigned by an admin.
- Label move emits audit event (`ai.prompt.label_moved`) so you can trace who promoted what.
- Backward compat: existing `publishedAt` → automatically maps to `production` label on migration.

## Dependencies

- Extends `AiPromptVersion` — additive (new `ai_prompt_label` table + junction).
- No dependency on Specs A/B/C.

## Open Questions

- Do labels version themselves (label history table) or is the latest move all we track?
- Can a label point to a DRAFT version, or only PUBLISHED?

## Spec-writing next step

Small enough to write the plan directly. `docs/superpowers/plans/YYYY-MM-DD-ai-gateway-prompt-labels.md`. ~6–8 tasks.
