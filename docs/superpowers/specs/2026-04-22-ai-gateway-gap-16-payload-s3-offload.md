# Gap #16 — Payload Offloading to S3

**Date:** 2026-04-22
**Status:** Stub — storage-cost optimization
**Tier:** 3 (P2)
**Edition:** ee
**Parent spec:** [2026-04-21-ai-gateway-gaps-spec.md](2026-04-21-ai-gateway-gaps-spec.md) §5 Tier 3 row 16

## Goal

Offload large prompt/completion payloads to object storage (S3 / MinIO / GCS / Azure Blob), keeping only a pointer in the `AiObservabilitySpan.{input,output}` columns. Postgres row size stays sane for high-throughput workloads with long contexts.

## Comparators

- **Helicone** — MinIO / S3 for payload storage (required for self-host).
- **Langfuse** — optional payload offload.

## Key Constraints

- Threshold-based: offload only if payload > configured size (e.g. 16 KB) — don't object-store every one-sentence prompt.
- Pointer format: `s3://bucket/ws-{id}/span-{id}.json` — self-descriptive.
- Authentication: signed-URL fetch from the ByteChef API (don't expose raw bucket credentials).
- Deletion: retention job must delete both DB rows *and* objects — no orphan payloads.
- Must reuse `platform-file-storage` abstraction (supports S3, GCS, Azure, local).

## Dependencies

- `platform-file-storage` already abstracts multi-provider object storage — reuse rather than roll new.
- No conflict with Specs A/B/C, but A (OTel ingest) benefits most since OTLP spans often carry full prompts.

## Open Questions

- Transparent re-fetch on read (fast) vs lazy pointer (cheaper for scans that don't need body)?
- Compression — gzip before upload?

## Spec-writing next step

`docs/superpowers/specs/YYYY-MM-DD-ai-gateway-payload-offload-design.md`. Medium plan.
