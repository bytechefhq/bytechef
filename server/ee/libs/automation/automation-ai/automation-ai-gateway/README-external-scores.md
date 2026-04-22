# AI Gateway — External Scores API

ByteChef accepts scores from external evaluators (RAGAS, LangSmith, DeepEval, internal LLM-as-judge microservices, human reviewers) and attaches them to existing traces or spans. The scores land in `ai_eval_score` with `source = EXTERNAL`, complementing the live-trace scores produced internally by `AiEvalRule` (source = `LLM_JUDGE`) and the manual API scores (source = `API`).

## Endpoints

```
POST /api/ai-gateway/v1/traces/{traceId}/scores
POST /api/ai-gateway/v1/spans/{spanId}/scores
POST /api/ai-gateway/v1/scores/batch
```

All three require:
- `Content-Type: application/json`
- `Authorization: Bearer <gateway-api-key>`
- `X-ByteChef-Workspace-Id: <numeric workspace id>`

## Request Body

Single-score request (`/traces/{id}/scores` or `/spans/{id}/scores`):

```json
{
  "name": "faithfulness",
  "value": 0.87,
  "dataType": "NUMERIC",
  "comment": "verified by ragas@0.2.3",
  "source": "ragas@0.2.3",
  "metadata": {
    "evaluator_version": "0.2.3",
    "run_id": "abc123"
  }
}
```

| Field | Required | Notes |
|-------|----------|-------|
| `name` | yes | Score identifier. Use consistent naming across traces so analytics can aggregate. |
| `value` | yes | Number for NUMERIC / BOOLEAN (0 or 1), string for CATEGORICAL. |
| `dataType` | yes | One of `NUMERIC`, `BOOLEAN`, `CATEGORICAL`. Must match the configured `AiEvalScoreConfig.dataType` for this name. |
| `comment` | no | Human-readable note. |
| `source` | no | Free-text evaluator tag (e.g. `"ragas@0.2.3"`). Indexed for filtering. |
| `metadata` | no | Arbitrary JSON object — persisted as-is for traceability. |

Batch request (`/scores/batch`):

```json
{
  "scores": [
    {
      "traceId": 123,
      "name": "faithfulness",
      "value": 0.87,
      "dataType": "NUMERIC",
      "source": "ragas@0.2.3"
    },
    {
      "spanId": 456,
      "name": "coherence",
      "value": 1,
      "dataType": "BOOLEAN",
      "source": "internal-judge"
    }
  ]
}
```

- Each item must specify **exactly one** of `traceId` or `spanId`.
- Maximum **1000 items** per batch.
- See the [Batch Atomicity](#batch-atomicity) section below for how partial failures are handled.

## Response Shape

Single-score response (200 OK):

```json
{
  "scoreId": 789,
  "accepted": true,
  "rejectionReason": null
}
```

Batch response (200 OK):

```json
{
  "acceptedCount": 2,
  "rejectedCount": 0,
  "rejectionReasons": []
}
```

Per-row failures appear in `rejectionReasons` as `trace 123: <reason>` or `span 456: <reason>`. See [Batch Atomicity](#batch-atomicity) for the full per-row contract.

## Batch Atomicity

**Each item in a batch is persisted in its own transaction.** A single `/scores/batch` call is *not* atomic across rows — there is no all-or-nothing rollback at the request level:
- If every item succeeds, all rows commit. `acceptedCount = N`, `rejectedCount = 0`.
- If an item fails **validation** (e.g. `value` is wrong shape for the `dataType`, workspace mismatch on that item's trace/span, or the targeted trace/span does not exist), that single item is skipped with a row in `rejectionReasons`. Already-persisted rows stay persisted; remaining rows continue.
- If an item fails at the **database level** (constraint violation, conflict), only that item's transaction rolls back. The failure is recorded in `rejectionReasons` and the rest of the batch continues. The client still receives an HTTP 200 with `rejectedCount > 0`.

The response shape (`acceptedCount`, `rejectedCount`, `rejectionReasons`) always reflects the actual outcome — a partial success surfaces as a 200 with both counts populated. Clients that need cross-item atomicity must coordinate at a higher layer.

## Error Responses

| Status | Reason |
|--------|--------|
| `400 Bad Request` | Missing / non-numeric `X-ByteChef-Workspace-Id`, malformed JSON, or `value`/`dataType` mismatch (via facade `IllegalArgumentException`). |
| `403 Forbidden` | Target trace or span belongs to a different workspace (`AiScoreWorkspaceBoundaryException`). The boundary is surfaced, not hidden behind a 404. |
| `404 Not Found` | Target `traceId` / `spanId` does not exist. |

## curl examples

```sh
# Attach a faithfulness score to a trace
curl -X POST https://gateway.example.com/api/ai-gateway/v1/traces/123/scores \
    -H 'Authorization: Bearer bct-...' \
    -H 'X-ByteChef-Workspace-Id: 42' \
    -H 'Content-Type: application/json' \
    -d '{
      "name": "faithfulness",
      "value": 0.87,
      "dataType": "NUMERIC",
      "source": "ragas@0.2.3",
      "metadata": { "run_id": "abc123" }
    }'

# Attach a boolean relevance score to a specific span
curl -X POST https://gateway.example.com/api/ai-gateway/v1/spans/456/scores \
    -H 'Authorization: Bearer bct-...' \
    -H 'X-ByteChef-Workspace-Id: 42' \
    -H 'Content-Type: application/json' \
    -d '{
      "name": "relevance",
      "value": 1,
      "dataType": "BOOLEAN",
      "source": "internal-judge@1.0"
    }'

# Batch-submit scores from a CI pipeline run
curl -X POST https://gateway.example.com/api/ai-gateway/v1/scores/batch \
    -H 'Authorization: Bearer bct-...' \
    -H 'X-ByteChef-Workspace-Id: 42' \
    -H 'Content-Type: application/json' \
    -d @ragas-run-2026-04-23.json
```

## Observability

| Metric | Type | Tags | Purpose |
|--------|------|------|---------|
| `bytechef_ai_score_recorded` | Counter | `source`, `data_type` | Every successful score write. Workspace id is intentionally NOT a tag — high-cardinality breakdowns belong in logs / traces, not Micrometer (matches the AiGatewayMetrics convention which excludes workspace from counter tags). |
| `bytechef_ai_score_value` | Distribution summary | `name` | Numeric score values, bucketed per score name. |

Each score write also emits an `AiScoreRecordedEvent` (Spring `ApplicationEvent`) — consumers include the platform audit log.

## Persistence Model

- Reuses `ai_eval_score` (no new table).
- New columns added by migration `00000000000005_ai_eval_score_external.xml`:
  - `metadata` (CLOB) — JSON-serialized `metadata` object
  - `source_identifier` (VARCHAR 255) — free-text evaluator tag
- New enum value: `AiEvalScoreSource.EXTERNAL` (ordinal 3, append-only).
- Cross-workspace writes are rejected *before* persistence — nothing lands in the DB on a 403.

## Relationship to Existing Scoring Surfaces

| Source | How scores land | Enum value |
|--------|----------------|------------|
| Internal LLM-as-Judge rule | `AiEvalRule` runs on live traces | `LLM_JUDGE` |
| Manual API (existing `/scores` endpoint) | Single-score POST with hardcoded source | `API` |
| **External evaluators (this API)** | POST to `/traces/{id}/scores`, `/spans/{id}/scores`, or `/scores/batch` | `EXTERNAL` |
| GraphQL admin UI (manual) | `AiEvalScoreGraphQlController` | `MANUAL` |

All sources persist into the same `ai_eval_score` table — downstream analytics (trend queries, rollups) treat them uniformly, with the `source` column available as a filter when you need to distinguish.

## Known Limitations

- No per-source rate limiting yet — relies on global gateway rate limits. A follow-up could add `bytechef.ai.gateway.external_score.rpm` keyed on workspace.
- `sourceIdentifier` is free-text and never validated — malformed values (`"\x00..."`, very long strings) are accepted up to 255 chars.
- No built-in retry for transient DB failures — clients should retry on 5xx responses.
- Cross-source score correlation (e.g. "show me faithfulness scores where RAGAS said OK but internal judge said FAIL") is not yet first-class in the GraphQL layer — query `AiEvalScore` directly by `source` + `sourceIdentifier` for now.
