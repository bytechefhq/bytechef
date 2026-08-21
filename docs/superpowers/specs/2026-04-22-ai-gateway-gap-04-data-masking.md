# Gap #4 — Data Masking / Payload Omission

**Date:** 2026-04-22
**Status:** Stub — needs design work
**Tier:** 1 (P0)
**Edition:** ee
**Parent spec:** [2026-04-21-ai-gateway-gaps-spec.md](2026-04-21-ai-gateway-gaps-spec.md) §5 Tier 1 row 4

## Goal

Give customers a lever to prevent specified fields (headers, prompts, completions) from being persisted in traces/spans. Supports compliance for regulated workloads where even logging the prompt body is a violation.

## Comparators

- **Helicone** — `Helicone-Omit-Headers`, `Helicone-Omit-Body` request headers.
- **Langfuse** — configurable masking function applied to payload before persistence.

## Key Constraints

- Per-workspace *and* per-request control. Per-request header wins.
- Must redact deterministically — same input produces same mask string so downstream analytics that key off hashes still work.
- Must apply before `AiObservabilitySpan.{input,output}` is written. Redaction at read-time is too late for subpoenas.

## Dependencies

- None hard.
- Interacts with `Guardrails v2` (component-level PII detector can double as a masking source).

## Open Questions

- Redaction algorithms: regex patterns only, or support for LLM-based redaction (hallucinates PII that wasn't there)?
- Do we hash + prefix (`[REDACTED:sha256:abc…]`) or just `[REDACTED]`? First is richer but leaks length.
- Retroactive redaction for existing traces — separate tool, or out of scope?

## Spec-writing next step

Full design spec at `docs/superpowers/specs/YYYY-MM-DD-ai-gateway-data-masking-design.md`.
