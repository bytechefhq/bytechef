# Gap #13 — Per-Request Webhooks (HMAC-signed)

**Date:** 2026-04-22
**Status:** Stub — small scope, follow-up
**Tier:** 3 (P2)
**Edition:** ee
**Parent spec:** [2026-04-21-ai-gateway-gaps-spec.md](2026-04-21-ai-gateway-gaps-spec.md) §5 Tier 3 row 13

## Goal

Fire a webhook after a specific request completes (pass, fail, or on score-threshold breach), HMAC-signed so the receiver can authenticate the gateway as source. Enables customers to trigger downstream workflows (retry, alert, log to their own sink) without polling.

## Comparator

- **Helicone** — per-request webhooks with signed headers.

## Key Constraints

- Signing: HMAC-SHA256 with workspace-scoped secret, standard `X-ByteChef-Signature` header.
- Delivery: at-least-once, exponential backoff, configurable retry budget.
- Filters: match by `model`, `cost ≥ X`, `latency ≥ Y`, or `score ≤ Z`. Filters are AND-composed.
- Must not block the request path — fire-and-forget via existing message broker.

## Dependencies

- Thin layer over existing alert-rule / notification-channel infra.
- No conflict with Specs A/B/C.

## Open Questions

- Dead-letter queue when retries exhaust — persistent `ai_webhook_failure` table or just alert?
- Webhook *payload* schema: full trace blob, or just metadata pointers the receiver can fetch?

## Spec-writing next step

`docs/superpowers/specs/YYYY-MM-DD-ai-gateway-webhooks-design.md`. Small plan (~8 tasks).
