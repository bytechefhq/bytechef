# AI LLM Gateway & Observability Platform

ByteChef's AI Gateway provides routing, cost control, observability, prompt management, alerting,
evaluation, and rate limiting for LLM traffic.

## Architecture

The platform is split into two logical planes that share the same deployment and database:

- **Gateway (data plane)** — Sits on the request path for every LLM call. Resolves the target
  provider and model from a routing policy, enforces budgets and rate limits, serves cached
  responses, compresses context when the model window fills up, retries on transient failures, and
  forwards the request to the upstream provider. The gateway exposes an OpenAI-compatible
  `/api/ai-gateway/v1/chat/completions` and `/embeddings` surface so existing SDKs can point at it
  unchanged.
- **Observability (control plane)** — Sits off the request path. Every request is recorded as a
  span on a trace, traces are grouped into sessions, and metrics, logs, prompts, alerts, scores,
  and exports are built on top of that data. The control plane is what the ByteChef UI and the
  public evaluation/scoring APIs talk to.

The split lets gateway traffic stay fast (the hot path only writes a request log and an
observability span) while the control plane aggregates, queries, and reacts to that data
asynchronously.

## Features

### Gateway (Infrastructure)

- Multi-provider routing with 9 strategies (round-robin, weighted, least-cost, least-latency,
  priority/failover, tag-based, model-affinity, sticky-session, canary).
- Budget enforcement per project/provider/policy with hard (block) and soft (warn) modes across
  daily, weekly, and monthly periods.
- Response caching keyed on normalized request content with configurable TTL.
- Context compression triggered at 85% model window utilization.
- Retry with exponential backoff on transient upstream failures.
- Rate limiting with pluggable backend — in-memory for single-node, Redis for distributed
  deployments.

### Observability

- Hierarchical tracing — each request becomes a trace containing a tree of spans built from
  `X-ByteChef-Parent-Span-Id` headers.
- Session grouping — correlate multiple traces belonging to one user interaction.
- Request/response logging with full payloads, token counts, latency, and cost.
- Real-time metrics dashboard (volume, error rate, P50/P95/P99 latency, tokens, cost).

### Prompt Management

- Version-controlled prompts — every save creates a new immutable version.
- Environment-based deployment across `production`, `staging`, and `development`.
- Variable substitution with `{{variable}}` syntax.
- One-click rollback to any previous version.

### Alerting

- Threshold-based rules on error rate, latency P95, cost, tokens, and request volume.
- Multi-channel notifications — webhook, email, and Slack.
- Alert history with acknowledgment and resolution tracking.

### Playground

- Interactive prompt testing from the UI.
- Side-by-side model comparison.
- Automatic trace creation tagged `source=PLAYGROUND` so playground runs are visible in
  observability like any other traffic.

### Evaluation & Scoring

- Manual annotation — thumbs up/down and free-text comments directly from the trace detail view.
- Programmatic API scoring via `POST /api/ai-gateway/v1/scores`.
- LLM-as-judge with configurable rules, sampling rates, and custom rubrics.

### Data Export

- On-demand export of traces, logs, sessions, and prompts in CSV, JSON, or JSONL.
- Webhook subscriptions with HMAC-SHA256 request signing for push-based integrations.

## HTTP Headers

| Header                              | Purpose                                              |
|-------------------------------------|------------------------------------------------------|
| `X-ByteChef-Trace-Id`               | Correlate requests into one trace                    |
| `X-ByteChef-Session-Id`             | Group traces into a session                          |
| `X-ByteChef-Span-Name`              | Name the current span                                |
| `X-ByteChef-Parent-Span-Id`         | Build parent-child hierarchy                         |
| `X-ByteChef-User-Id`                | Identify the end user                                |
| `X-ByteChef-Metadata-*`             | Arbitrary key-value metadata                         |
| `X-ByteChef-Tags`                   | Comma-separated tag names                            |
| `X-ByteChef-Prompt-Name`            | Use a managed prompt                                 |
| `X-ByteChef-Prompt-Environment`     | Which environment (default: `production`)            |
| `X-ByteChef-Property-*`             | Custom properties for rate limiting/analytics        |

## Gateway Response Headers

Every successful response from `/api/ai-gateway/v1/chat/completions` carries optional
observability headers for client-side tracing and debugging:

| Header                      | Value                                                              |
|-----------------------------|--------------------------------------------------------------------|
| `x-gateway-provider`        | Resolved provider name (e.g. `openai`, `anthropic`)                |
| `x-gateway-model`           | Actual model that served the request after routing                 |
| `x-gateway-latency-ms`      | Gateway-observed end-to-end latency                                |
| `x-gateway-cache-hit`       | `true` when the response came from the request cache               |
| `x-gateway-routing-policy`  | Policy name or ID that selected the provider/model                 |
| `x-gateway-request-id`      | Opaque request correlation ID                                      |

Budget handling:

- **Soft limit** (coming) — `x-gateway-budget-warning: <remaining-usd>` when cumulative spend
  crosses the warning threshold.
- **Hard limit** — `HTTP 402 Payment Required` with `{ error: { type: "budget_exceeded",
  message, budgetUsd, spentUsd } }`.

## Metrics

Micrometer counters/timers emitted by `AiGatewayMetrics`:

| Metric                              | Tags                    | Source                                       |
|-------------------------------------|-------------------------|----------------------------------------------|
| `ai_gateway.trace.completed`        | model, status           | On `AiGatewayTraceCompletedEvent`            |
| `ai_gateway.trace.latency`          | model, status           | Timer on trace completion                    |
| `ai_gateway.budget.exceeded`        | model                   | On `AiGatewayBudgetExceededEvent`            |
| `ai_gateway.rate_limit.rejections`  | rate_limit              | On 429 from rate-limit checker               |
| `ai_gateway.alert.triggered`        | metric                  | When an alert rule fires (TRIGGERED event)   |
| `ai_gateway.webhook.delivery`       | event_type, status      | On each terminal webhook delivery attempt    |

Workspace and user tags are intentionally omitted to keep metric cardinality bounded;
use logs or traces for per-workspace breakdowns.

## Data Privacy & Retention

- **Trace payloads** (`ai_observability_trace.input` / `.output`, span inputs/outputs) are persisted
  verbatim by default. The `pii_redacted` boolean column on `ai_observability_trace` is reserved
  for future workspace-level redaction: when `true`, the facade will persist a content digest in
  place of the body. Until that wiring lands, operators concerned about PII exposure should rely
  on the data-cleanup retention job.
- **Retention** is handled by `AiObservabilityDataCleanupService` on a fixed schedule. Per-workspace
  retention days are planned as part of workspace settings but not yet enforced.
- **Encryption-at-rest** is provided by PostgreSQL-level disk encryption (deployment-specific) plus
  encryption of `workspace_ai_gateway_provider.api_key` (platform `EncryptedMapWrapper`). Request
  payloads are currently stored unencrypted in TEXT columns.
- **GDPR / workspace deletion** — workspace-scoped FK cascades clean up the `ai_gateway_*` and
  `ai_observability_*` rows when a workspace is removed; the platform `Property`-store rows
  (`ai_gateway_workspace_settings` values) are also scoped by `workspace_id` and removed by the same
  path.

## Configuration

```yaml
bytechef:
  ai:
    gateway:
      enabled: true
      rate-limiting:
        enabled: false
        provider: memory  # memory | redis
```

## Module Structure

- `automation-ai-gateway-api` — Domain model, DTOs, and SPI interfaces shared across modules.
- `automation-ai-gateway-service` — Service and facade implementations, persistence,
  Liquibase changelogs.
- `automation-ai-gateway-graphql` — Internal GraphQL schema and resolvers consumed by the
  ByteChef UI.
- `automation-ai-gateway-public-rest` — OpenAPI-generated public REST API
  (`/api/ai-gateway/v1/*`).
- `automation-ai-gateway-remote-client` — Remote client stubs used by distributed EE apps.

## Database Schema

Tables are grouped by feature prefix:

**`ai_gateway_*`** — gateway infrastructure
- `ai_gateway_budget`
- `ai_gateway_custom_property`
- `ai_gateway_model` (includes `default_routing_policy_id` override)
- `ai_gateway_model_deployment`
- `ai_gateway_project`
- `ai_gateway_provider`
- `ai_gateway_rate_limit`
- `ai_gateway_request_log`
- `ai_gateway_routing_policy`
- `ai_gateway_routing_policy_tag`
- `ai_gateway_spend_summary`
- `ai_gateway_tag` — workspace-scoped tags with color metadata
- `workspace_ai_gateway_provider`
- `workspace_ai_gateway_routing_policy`

Workspace-level AI Gateway settings overrides are persisted as rows in the
platform `property` table (scope=`WORKSPACE`, key=`ai_gateway_workspace_settings`)
rather than a dedicated table — the property store already handles scope, audit,
versioning, and encryption.

**`ai_observability_*`** — tracing, alerts, exports
- `ai_observability_trace`
- `ai_observability_span`
- `ai_observability_session`
- `ai_observability_trace_tag`
- `ai_observability_alert_rule`
- `ai_observability_alert_rule_channel`
- `ai_observability_alert_event`
- `ai_observability_notification_channel`
- `ai_observability_webhook_subscription`
- `ai_observability_export_job`

**`ai_prompt_*`** — prompt management
- `ai_prompt`
- `ai_prompt_version`

**`ai_eval_*`** — evaluation and scoring
- `ai_eval_rule`
- `ai_eval_execution`
- `ai_eval_score`
- `ai_eval_score_config`
