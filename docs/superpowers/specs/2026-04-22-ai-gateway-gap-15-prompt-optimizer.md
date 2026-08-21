# Gap #15 — Prompt Optimizer (GEPA-style)

**Date:** 2026-04-22
**Status:** Stub — deferred, depends on Spec C
**Tier:** 3 (P2)
**Edition:** ee
**Parent spec:** [2026-04-21-ai-gateway-gaps-spec.md](2026-04-21-ai-gateway-gaps-spec.md) §5 Tier 3 row 15

## Goal

Automatic prompt optimization: given a dataset and an evaluator, mutate the prompt (via LLM-generated variants) and measure score deltas, converging on a better-performing prompt.

## Comparator

- **Latitude** — GEPA optimizer for systematic prompt search.

## Key Constraints

- **Hard dependency on Spec C (Datasets + Experiments)** — optimizer is "run a series of experiments with auto-generated prompt variants".
- Budget-aware: each optimization run can cost real money; customer must set a cost ceiling.
- Convergence: stop when score plateaus or budget exhausts.
- Optimization strategies: mutation + crossover (genetic), gradient-style (rewrite-and-score), or exhaustive grid over a small candidate set.

## Dependencies

- **Blocked by Spec C.**
- Reuses `AiEvalRule` evaluators to score candidates.
- Likely new sibling module `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-optimizer/` or folded into `automation-ai-gateway-experiment`.

## Open Questions

- Which optimization strategy first? Gradient-style ("rewrite this to score higher") is cheapest; genetic is most interpretable.
- Do we ship with pre-baked mutation templates, or BYO?
- Multi-objective (optimize for score *and* cost *and* latency) vs single-objective?

## Spec-writing next step

Deferred. Ship Spec C first, let customers actually run experiments, then revisit.
