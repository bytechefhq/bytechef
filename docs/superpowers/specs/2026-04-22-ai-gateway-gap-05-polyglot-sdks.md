# Gap #5 — Polyglot SDKs (Python / TS / JS)

**Date:** 2026-04-22
**Status:** Stub — deferred, OTel (Gap #1) mitigates short-term
**Tier:** 1 (P0)
**Edition:** ce + ee
**Parent spec:** [2026-04-21-ai-gateway-gaps-spec.md](2026-04-21-ai-gateway-gaps-spec.md) §5 Tier 1 row 5

## Goal

Native Python + TypeScript + JavaScript SDKs for the gateway — not just an OpenAI-compatible client library, but typed idiomatic SDKs that expose prompts, evals, scores, datasets, and experiments as first-class objects.

## Comparators

- **Langfuse** — `langfuse` (Python), `langfuse-js` (TS/JS), cover prompts + tracing + evals.
- **Latitude** — Python + TS SDKs.

## Key Constraints

- OpenAI-compat `/chat/completions` already works with `openai-python` / `openai-node` by base-URL swap — SDKs layer *on top* of that for the ByteChef-specific surfaces.
- Auto-instrument `openai` / `anthropic` / `google-genai` calls so teams get tracing without manual span creation (Langfuse's SDK does this via monkey-patching).
- Versioning: SDK version ↔ server version compat matrix.
- Published to PyPI + npm under a ByteChef scope.

## Dependencies

- Gap #1 (OTel) ships first — SDKs can use OTel under the hood instead of our own protocol.
- Scores / Datasets / Experiments APIs (Gaps #2, #6) define the SDK object surface.

## Open Questions

- Generated-from-OpenAPI (`openapi-generator` / `orval`) vs hand-written ergonomic wrapper over a thin generated client?
- Auto-instrumentation strategy — OTel SDK dependency vs our own HTTP interceptor?
- JS vs TS: ship two packages, or one TS package that works in JS contexts?

## Spec-writing next step

Not yet — OTel ingest (Gap #1) must ship first and soak for ~2 months to prove it covers the polyglot story. Revisit 2026-Q3.
