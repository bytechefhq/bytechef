# Spec C — Datasets + Experiments Framework

**Date:** 2026-04-22
**Status:** Design (full spec exists at §8 of `2026-04-21-ai-gateway-gaps-spec.md`)
**Tier:** 2 (P1)
**Edition:** ee
**Gap #:** 6 (of 17 in the bundle)

## Context

Full design landed in §8 of [2026-04-21-ai-gateway-gaps-spec.md](2026-04-21-ai-gateway-gaps-spec.md). This stub exists for discoverability as a standalone work item.

## Goal (one line)

Curate + version datasets (promote from traces, upload CSV/JSONL, or POST single items), then run experiments that replay a dataset against a prompt version / model and score the results — enabling prompt regression testing.

## New Modules

- `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-dataset/` (api + service + rest)
- `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-experiment/` (api + service + rest)

Sibling to the existing `automation-ai-gateway-{api,service,public-rest}` submodules. EE-only — no platform-level placement since datasets + experiments are gateway concerns, not general infrastructure.

## Tables (6 new, per §8.2)

`ai_dataset`, `ai_dataset_version`, `ai_dataset_item`, `ai_experiment`, `ai_experiment_run`, plus junction/index metadata.

## Dependencies

- Independent of Spec A (OTel) and Spec B (External Scores) at the schema level.
- Reuses `atlas-coordinator` task dispatch (same pattern as workflow execution), `AiEvalRule` evaluators, and experiment-run traces via Spec B if shipped.

## Open Questions (from spec §12)

- §12.3 — Experiment-run traces vs observability retention quota.
- §12.4 — Dataset version `frozen` flag: user-set or auto-set?
- §12.5 — Does `AiEvalRule` need a `target = EXPERIMENT_TRACE` discriminator?

## Next step

Write the full TDD plan at `docs/superpowers/plans/YYYY-MM-DD-ai-gateway-datasets-experiments.md` before implementation. Largest scope of the three sub-specs; expect it to break into multiple PRs.
