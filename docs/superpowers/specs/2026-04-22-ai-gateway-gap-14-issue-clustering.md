# Gap #14 — Issue Clustering / Failure-Mode Discovery

**Date:** 2026-04-22
**Status:** Stub — requires embedding infra
**Tier:** 3 (P2)
**Edition:** ee
**Parent spec:** [2026-04-21-ai-gateway-gaps-spec.md](2026-04-21-ai-gateway-gaps-spec.md) §5 Tier 3 row 14

## Goal

Automatically surface clusters of similar failed or low-scoring traces so product teams can see "here are the 30 questions the bot struggled with this week" without manual triage.

## Comparator

- **Latitude** — issue clustering as a first-class feature.

## Key Constraints

- Offline batch job — runs nightly or on-demand, not inline.
- Clustering algorithm: HDBSCAN or k-means over prompt embeddings, top-N cluster sizes surfaced.
- Cluster labels auto-generated (LLM summarizes the cluster members). Human can rename.
- Drift detection: alert when a new cluster appears / grows quickly week-over-week.

## Dependencies

- Requires embedding infrastructure (same question as Gap #12 semantic cache — probably shared).
- Relies on production scoring (`AiEvalRule` or Scores API — Gap #2) for "low-scoring" filter.
- Ideally composes with Dataset promotion (Spec C) — "promote this whole cluster to regression test".

## Open Questions

- Real-time clustering (streaming as traces arrive) or pure batch? Batch is pragmatic.
- Per-workspace only, or cross-workspace (with careful anonymization) for benchmarking?

## Spec-writing next step

Deferred. Wait on Gap #12 (embedding infra decision) and Spec C to ship.
