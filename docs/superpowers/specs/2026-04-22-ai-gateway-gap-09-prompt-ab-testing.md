# Gap #9 — Prompt A/B Testing Primitive

**Date:** 2026-04-22
**Status:** Stub — blocked on Gap #7 (labels)
**Tier:** 2 (P1)
**Edition:** ee
**Parent spec:** [2026-04-21-ai-gateway-gaps-spec.md](2026-04-21-ai-gateway-gaps-spec.md) §5 Tier 2 row 9

## Goal

Route a configurable fraction of requests referencing a prompt-by-label to an alternate version. Persist the arm (`A | B`) on the span so downstream evals (live-trace or dataset) can attribute score deltas.

## Comparators

- **Langfuse** — prompt version A/B with deterministic hashing of session id.

## Key Constraints

- Deterministic per `session.id` (or `user.id`) — same session always sees the same arm.
- Configurable split: not just 50/50; 95/5 canary, 10/10/80 three-way, etc.
- Arm selection is *pre-dispatch* but logged on the span (`metadata.ab_arm = "B"`).
- No impact on latency — arm selection is a cheap hash.

## Dependencies

- **Blocked by Gap #7 (prompt labels)** — A/B only makes sense when you can reference versions by label.
- Spec C (Datasets + Experiments) is a sibling — batch evals of the two arms complement production-traffic A/B.

## Open Questions

- Do we stop the experiment automatically on statistically-significant score win (like a feature flag rollout tool)? Nice-to-have; probably follow-up.

## Spec-writing next step

Wait for Gap #7 to ship. Then `docs/superpowers/specs/YYYY-MM-DD-ai-gateway-prompt-ab-testing-design.md`.
