# AI Gateway — OTLP Trace Ingestion

ByteChef accepts OpenTelemetry OTLP/HTTP trace exports with GenAI semantic-convention attributes, mapping them onto the existing `AiObservabilityTrace` / `AiObservabilitySpan` observability store.

## Endpoint

```
POST /api/ai-gateway/v1/otlp/traces
Content-Type: application/x-protobuf
X-ByteChef-Workspace-Id: <numeric workspace id>
Authorization: Bearer <gateway-api-key>
```

Response: `202 Accepted` with a JSON body:

```json
{
  "acceptedSpans": 1,
  "deduplicatedSpans": 0,
  "rejectedSpans": 0,
  "rejectionReasons": [],
  "warnings": []
}
```

Three structurally separate persistence buckets plus a fidelity-warning channel:

- `acceptedSpans` — newly persisted gen_ai spans (one DB row each).
- `deduplicatedSpans` — spans already persisted by an earlier OTLP delivery and matched the partial unique index on `(trace_id, external_span_id)`. Surfaced as its own bucket (rather than folded into `accepted`) so dashboards do not over-count, while the wire response stays a non-error outcome that does not require client retry — this keeps OTel exporters out of a retry loop.
- `rejectedSpans` — pre-persist boundary violations (missing `gen_ai.system`, missing status), persist-time failures (DB constraint other than dedup, transient connection errors), or mapper-rejected inputs (malformed trace/span ids, etc.). The accompanying `rejectionReasons` list MAY aggregate identical mapper-batch rejections into a single row, so `rejectionReasons.length <= rejectedSpans`; clients must not assume 1:1.
- `warnings` — degraded-outcome breadcrumbs for spans that DID persist but with reduced fidelity (e.g. resource-attribute decode failed and `service.name` is missing on the persisted row). Structurally separate from `rejectionReasons` because the affected spans WERE accepted; folding them in would either inflate `rejectedSpans` (so the persistence buckets no longer reflect persistence outcomes) or violate the `rejectionReasons.length <= rejectedSpans` invariant. OTel exporters reading the response can distinguish "all persisted, some with reduced fidelity" from "some rejected, retry these" — different operator actions for different outcomes.

Conservation invariant: `acceptedSpans + deduplicatedSpans + rejectedSpans` equals the count of spans the mapper attempted to persist plus the count rejected at the mapper boundary.

Spans without any `gen_ai.*` attribute are silently discarded at the mapper — only LLM calls land as traces. Spans missing `gen_ai.system` are counted in `rejectedSpans` with a reason per span id.

Per-request server-side log cap: the mapper emits at most 5 warn-level rejection lines per OTLP request (with a single summary line for any overflow) so a malformed-batch attack or misconfigured exporter cannot DoS structured-log pipelines that pay per-line. The `rejectedSpans` count is exact — only the per-line breadcrumb logging is capped.

## Configuring an OpenTelemetry Exporter

```sh
export OTEL_EXPORTER_OTLP_TRACES_ENDPOINT=https://gateway.example.com/api/ai-gateway/v1/otlp/traces
export OTEL_EXPORTER_OTLP_HEADERS=authorization=Bearer%20bct-...,x-bytechef-workspace-id=42
export OTEL_RESOURCE_ATTRIBUTES=service.name=your-service
```

Works unchanged with any OTel SDK (Python, JS/TS, Go, Rust, JVM, .NET). Cost is computed server-side from `gen_ai.usage.{input,output}_tokens` × `AiGatewayModel.{input,output}CostPerMTokens`, so inbound traces carry accurate cost regardless of the SDK that produced them.

## Error Responses

| Status | Reason |
|--------|--------|
| `400 Bad Request` | Missing / non-numeric `X-ByteChef-Workspace-Id` header |
| `413 Payload Too Large` | Batch exceeds `bytechef.ai.gateway.otlp.maxSpansPerRequest` (default: 1 000) |
| `429 Too Many Requests` | Per-workspace rate limit exceeded (default: 10 000 req/min). Response carries `Retry-After` header in seconds |

## Configuration

| Property | Default | Purpose |
|----------|---------|---------|
| `bytechef.ai.gateway.otlp.maxSpansPerRequest` | `1000` | Hard cap on spans per request body |
| `ai_gateway_otlp_rate_limit` (workspace-scoped Property) | `{"rpm": 10000}` | Per-workspace RPM override |

## GenAI Attribute Mapping

| OTel attribute | `AiObservabilitySpan` field |
|----------------|----------------------------|
| `gen_ai.system` | `provider` |
| `gen_ai.request.model` | `model` (requested) |
| `gen_ai.response.model` | `model` (actual, after routing) |
| `gen_ai.usage.input_tokens` | `inputTokens` |
| `gen_ai.usage.output_tokens` | `outputTokens` |
| `gen_ai.prompt` | `input` |
| `gen_ai.completion` | `output` |
| span trace_id (lowercase hex of the 16-byte protobuf id, via `HexFormat.formatHex`) | `AiObservabilityTrace.externalTraceId` (dedup key paired with `workspaceId`) |
| span span_id (lowercase hex of the 8-byte protobuf id, via `HexFormat.formatHex`) | `AiObservabilitySpan.externalSpanId` (idempotency key paired with `traceId`) |
| span status | `AiObservabilitySpan.status` (`OK`/`UNSET` → `COMPLETED`, `ERROR` → `ERROR`) |
| span start/end time | `AiObservabilitySpan.{startTime,endTime,latencyMs}` |

## Module Layout

- `automation-ai-gateway-otlp-api/` — OTel Protobuf runtime (`io.opentelemetry.proto:opentelemetry-proto`) + neutral DTOs (`OtelSpanBatch`, `OtelGenAiSpan`, `OtelSpanStatus`) + mapper interface.
- `automation-ai-gateway-otlp-service/` — `OtlpProtobufMapperImpl` + `@AutoConfiguration`.

The REST controller (`AiGatewayOtlpController`), persistence facade (`AiObservabilityOtlpIngestFacadeImpl`), and cost resolver (`OtlpCostResolver`) live in the sibling `automation-ai-gateway-public-rest` / `automation-ai-gateway-service` modules alongside the chat-completion path.

## Known Limitations

- Only OTLP/HTTP (protobuf body) is accepted. OTLP/gRPC (`:4317`) is deferred.
- Only traces are ingested — OTel metrics and logs are out of scope.
- Cost is computed server-side only when the `gen_ai.response.model` (or `gen_ai.request.model` as fallback) matches a registered `AiGatewayModel`. Unknown models leave `cost = null`.
