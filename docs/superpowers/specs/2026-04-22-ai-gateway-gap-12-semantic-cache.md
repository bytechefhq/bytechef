# Gap #12 — Semantic Cache (Vector Similarity)

**Date:** 2026-04-22
**Status:** Stub — follow-up, not in the current bundle
**Tier:** 3 (P2)
**Edition:** ee
**Parent spec:** [2026-04-21-ai-gateway-gaps-spec.md](2026-04-21-ai-gateway-gaps-spec.md) §5 Tier 3 row 12

## Goal

Cache LLM responses keyed on prompt *semantic similarity* (embedding distance), not SHA-256 of the raw text. Dramatically increases hit rate for paraphrased queries — table-stakes for support-bot and customer-care use cases.

## Comparator status

**Neither Helicone, Langfuse, nor Latitude ship this.** Differentiator opportunity. Multiple third-party tools (GPTCache, Redis Semantic Cache) exist.

## Key Constraints

- Similarity threshold is per-workspace tunable (default e.g. cosine ≥ 0.95).
- Cache entries expire (TTL) AND have max-entries-per-prompt cap to prevent drift.
- Must work alongside existing exact-match cache (exact wins if present).
- Cache-hit traces must be obvious in the UI — fake latency would distort alerting.

## Dependencies

- Embedding backend — reuse existing `automation-knowledge-base` vector-store abstraction? Or gateway-owned?
- No conflict with Specs A/B/C.

## Open Questions

- Which embedding model is authoritative for similarity? Per-workspace config, or single gateway-wide model?
- Key space: per-workspace (safest), per-prompt-version, or shared-across-similar-prompts (dangerous, could leak one customer's answer to another's unrelated query)?
- Cost: semantic cache adds embedding cost per miss; when is the net savings positive?

## Spec-writing next step

Deferred. Revisit after OTel + Scores + Datasets are shipped and we have usage data on cache miss rates.
