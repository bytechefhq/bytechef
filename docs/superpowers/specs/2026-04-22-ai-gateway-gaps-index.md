# AI Gateway Gaps — Index

**Date:** 2026-04-22
**Parent spec:** [2026-04-21-ai-gateway-gaps-spec.md](2026-04-21-ai-gateway-gaps-spec.md)

Tracking file mapping each of the 17 gaps identified in the parent spec to its implementation status.

| # | Gap | Tier | Status | Artifact |
|---|-----|------|--------|----------|
| 1 | OTel-native trace ingestion | 1 | **Plan written** | [plans/2026-04-22-ai-gateway-otel-ingestion.md](../plans/2026-04-22-ai-gateway-otel-ingestion.md) |
| 2 | External Scores ingestion API | 1 | Design ready (spec §7) | [gap-02-external-scores-api.md](2026-04-22-ai-gateway-gap-02-external-scores-api.md) |
| 3 | Gateway-level guardrails | 1 | Stub — needs design | [gap-03-gateway-guardrails.md](2026-04-22-ai-gateway-gap-03-gateway-guardrails.md) |
| 4 | Data masking / payload omission | 1 | Stub — needs design | [gap-04-data-masking.md](2026-04-22-ai-gateway-gap-04-data-masking.md) |
| 5 | Polyglot SDKs (Python / TS / JS) | 1 | Deferred (OTel mitigates) | [gap-05-polyglot-sdks.md](2026-04-22-ai-gateway-gap-05-polyglot-sdks.md) |
| 6 | Datasets + Experiments framework | 2 | Design ready (spec §8) | [gap-06-datasets-experiments.md](2026-04-22-ai-gateway-gap-06-datasets-experiments.md) |
| 7 | Prompt environments / labels | 2 | Stub — small, ready to plan | [gap-07-prompt-environments.md](2026-04-22-ai-gateway-gap-07-prompt-environments.md) |
| 8 | Gateway-side prompt rendering | 2 | Stub — small, ready to plan | [gap-08-gateway-prompt-rendering.md](2026-04-22-ai-gateway-gap-08-gateway-prompt-rendering.md) |
| 9 | Prompt A/B testing | 2 | Blocked on #7 | [gap-09-prompt-ab-testing.md](2026-04-22-ai-gateway-gap-09-prompt-ab-testing.md) |
| 10 | Prompt composition / snippets | 2 | Stub — needs design | [gap-10-prompt-composition.md](2026-04-22-ai-gateway-gap-10-prompt-composition.md) |
| 11 | Playground / prompt IDE | 2 | Stub — large UI scope | [gap-11-playground.md](2026-04-22-ai-gateway-gap-11-playground.md) |
| 12 | Semantic cache (vector similarity) | 3 | Deferred | [gap-12-semantic-cache.md](2026-04-22-ai-gateway-gap-12-semantic-cache.md) |
| 13 | Per-request webhooks (HMAC-signed) | 3 | Stub — small, ready | [gap-13-per-request-webhooks.md](2026-04-22-ai-gateway-gap-13-per-request-webhooks.md) |
| 14 | Issue clustering / failure-mode | 3 | Deferred | [gap-14-issue-clustering.md](2026-04-22-ai-gateway-gap-14-issue-clustering.md) |
| 15 | Prompt optimizer (GEPA) | 3 | Blocked on #6 | [gap-15-prompt-optimizer.md](2026-04-22-ai-gateway-gap-15-prompt-optimizer.md) |
| 16 | Payload offloading to S3 | 3 | Stub — medium | [gap-16-payload-s3-offload.md](2026-04-22-ai-gateway-gap-16-payload-s3-offload.md) |
| 17 | Human-in-the-loop annotation | 3 | Blocked on #6 | [gap-17-annotation-queues.md](2026-04-22-ai-gateway-gap-17-annotation-queues.md) |

## Suggested execution order

1. **#1 OTel ingestion** — plan ready, start here.
2. **#2 External Scores API** — write plan from spec §7, then implement. Small scope (~10 tasks).
3. **#7 Prompt labels** — small scope, independent.
4. **#8 Gateway prompt rendering** — small scope, benefits from #7.
5. **#6 Datasets + Experiments** — write plan from spec §8, then implement. Largest scope.
6. **#3 Guardrails at gateway level** — needs design spec first.
7. **#4 Data masking** — needs design spec first.
8. **#13 Webhooks** — nice-to-have, small.
9. **#16 Payload S3 offload** — after #1 starts filling the trace table.
10. Remaining: **#5 SDKs** (after #1 soaks), **#9 A/B** (after #7), **#10 composition**, **#11 playground**, **#14 clustering**, **#15 optimizer**, **#17 annotations**, **#12 semantic cache**.

## Conventions for new specs/plans in this bundle

- File name: `YYYY-MM-DD-ai-gateway-<slug>.md`
- Spec lives under `docs/superpowers/specs/`, plan under `docs/superpowers/plans/`
- Plans follow the template in [plans/2026-04-22-ai-gateway-otel-ingestion.md](../plans/2026-04-22-ai-gateway-otel-ingestion.md)
- All new code under `server/ee/**` uses the ByteChef Enterprise license header + `@version ee` Javadoc
- All new code under `server/libs/**` uses Apache 2.0 + `@author Ivica Cardic`
- All new enums persist as INT ordinals, append-only, per `feedback_enum_storage`
- Every new feature ships at least one `bytechef_<feature>_*` counter via `ObjectProvider<MeterRegistry>`
