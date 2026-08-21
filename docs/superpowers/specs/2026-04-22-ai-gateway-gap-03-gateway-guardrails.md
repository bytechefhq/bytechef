# Gap #3 — Gateway-Level Guardrails

**Date:** 2026-04-22
**Status:** Stub — needs design work
**Tier:** 1 (P0)
**Edition:** ee
**Parent spec:** [2026-04-21-ai-gateway-gaps-spec.md](2026-04-21-ai-gateway-gaps-spec.md) §5 Tier 1 row 3

## Goal

Apply content-safety detectors (prompt injection, PII, moderation, Llama Guard) *at the gateway request path* — i.e., before the request is dispatched to a provider. Policy-level enforcement, distinct from the component-level `Guardrails v2` that workflow authors opt into.

## Comparator

- Helicone — Prompt Guard, Llama Guard, Moderation (header-driven opt-in).

## Key Constraints

- Must not regress provider latency by more than ~200 ms p95 — expensive detectors must be async-scored post-response, not blocking.
- Reuse `Guardrails v2` detector classes where possible (see `docs/superpowers/specs/2026-04-16-guardrails-v2-design.md`) to avoid a parallel stack.
- Per-workspace policy (`block | redact | score-only`) with per-API-key override.
- Must be **disabled by default** — opt-in is safer than opt-out for production traffic.

## Dependencies

- None hard. Independent of Specs A/B/C.
- Related: `Guardrails v2` (component-level) — need a shared detector interface.

## Open Questions

- Do we bundle Llama Guard weights with the gateway, or require BYOM?
- Per-request rejection response: HTTP 400, 422, or new `x-bytechef-blocked` header + 200 with sanitized body?
- Which detectors run inline (before dispatch) vs which run async (after response)? Latency budget math needed.

## Spec-writing next step

Full design spec at `docs/superpowers/specs/YYYY-MM-DD-ai-gateway-gateway-guardrails-design.md`.
