# Spec B — External Scores Ingestion API

**Date:** 2026-04-22
**Status:** Design (full spec exists at §7 of `2026-04-21-ai-gateway-gaps-spec.md`)
**Tier:** 1 (P0)
**Edition:** ee
**Gap #:** 2 (of 17 in the bundle)

## Context

Full design landed in §7 of [2026-04-21-ai-gateway-gaps-spec.md](2026-04-21-ai-gateway-gaps-spec.md). This stub exists so the spec appears in the specs directory alongside peers and is discoverable as a standalone work item.

## Goal (one line)

Accept score submissions from external evaluators (RAGAS, LangSmith, DeepEval, internal judges) against existing traces/spans, persisting into `AiEvalScore` with a new `source = EXTERNAL` discriminator.

## Endpoints

```
POST /api/ai-gateway/v1/traces/{traceId}/scores
POST /api/ai-gateway/v1/spans/{spanId}/scores
POST /api/ai-gateway/v1/scores/batch                  # cap: 1000 entries
```

## Dependencies

- Nothing hard. Independent of Spec A (OTel) and Spec C (Datasets).
- Reuses `AiEvalScore`, `AiGatewayApiKeyAuthenticationProvider`, `ConnectionAuditAspect` pattern.

## Open Questions (from spec §12.2)

- Cross-workspace write semantics: `403` vs `404`. Pick before shipping.

## Next step

Write the full TDD plan at `docs/superpowers/plans/YYYY-MM-DD-ai-gateway-external-scores.md` before implementation. Model after `2026-04-22-ai-gateway-otel-ingestion.md`.
