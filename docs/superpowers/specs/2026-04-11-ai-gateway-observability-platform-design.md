# AI Gateway Observability Platform Design

## Overview

Evolve ByteChef's AI LLM Gateway from a gateway/proxy into a full observability platform comparable to Langfuse and Helicone. The gateway infrastructure (providers, routing, budgets, caching, retries) remains as-is. New observability features are added in 6 phases as new sidebar tabs within the existing AI Gateway UI at `/automation/ai-gateway`.

## Current State

The AI Gateway already provides:

- **8 LLM providers**: OpenAI, Anthropic, Azure OpenAI, Google Gemini, Groq, Cohere, Mistral, DeepSeek
- **9 routing strategies**: Simple, Weighted Random, Cost Optimized, Latency Optimized, Priority Fallback, Tag Based, Intelligent Balanced/Cost/Quality
- **Request logging**: Flat `ai_gateway_request_log` table with tokens, cost, latency, status, cache hit
- **Monitoring dashboard**: 4 stat cards, request volume chart, latency percentiles (p50/p95/p99), error rate by provider, cost breakdown by provider, request log table
- **Budget enforcement**: Hard/soft modes with alert thresholds
- **Caching**: Response caching with configurable TTL per project
- **Context compression**: Automatic at 85% context window utilization
- **Retry handling**: Configurable max attempts with backoff

## Table Prefix Convention

- `ai_gateway_` — Gateway infrastructure tables (existing + rate limiting)
- `ai_observability_` — New observability tables (tracing, prompts, alerting, exports)

## Database Migration Strategy

- **`ai_gateway_*` changes** (rate_limit, custom_property): Add new changesets to the existing `00000000000001_ai_gateway_init.xml`
- **`ai_observability_*` tables**: New Liquibase file `00000000000002_ai_observability_init.xml` in the same changelog directory (`automation/ai_gateway/`)

## Phase 1: Tracing & Sessions

### Purpose

Transform flat request logs into hierarchical traces. A single user interaction (trace) can contain multiple spans and generations in a parent-child tree, grouped into sessions for conversations/workflows.

### Data Model

#### ai_observability_trace

| Column | Type | Description |
|---|---|---|
| id | BIGINT PK | Auto-increment |
| workspace_id | BIGINT NOT NULL | Workspace scope |
| project_id | BIGINT | Optional project scope |
| session_id | BIGINT FK | Optional link to session |
| name | VARCHAR(256) | Trace name (e.g., "chat-completion", "rag-pipeline") |
| user_id | VARCHAR(256) | Caller-provided end user identifier |
| external_trace_id | VARCHAR(256) | Caller-provided trace ID for correlating multiple requests |
| input | TEXT | Request payload |
| output | TEXT | Final response |
| metadata | TEXT | JSON key-value map |
| source | INT | API (0), PLAYGROUND (1) |
| status | INT | ACTIVE (0), COMPLETED (1), ERROR (2) |
| total_cost | DECIMAL(10,6) | Aggregated from spans |
| total_input_tokens | INT | Aggregated from spans |
| total_output_tokens | INT | Aggregated from spans |
| total_latency_ms | INT | End-to-end latency |
| created_date | TIMESTAMP NOT NULL | |
| last_modified_date | TIMESTAMP NOT NULL | |
| version | BIGINT NOT NULL | Optimistic locking |

Indexes: workspace_id, project_id, session_id, user_id, external_trace_id, created_date, source

#### ai_observability_span

| Column | Type | Description |
|---|---|---|
| id | BIGINT PK | Auto-increment |
| trace_id | BIGINT FK NOT NULL | Parent trace |
| parent_span_id | BIGINT FK | Self-referencing for tree structure (nullable = root span) |
| name | VARCHAR(256) | Span name |
| type | INT | GENERATION (0), SPAN (1), EVENT (2), TOOL_CALL (3) |
| model | VARCHAR(256) | For GENERATION type |
| provider | VARCHAR(64) | For GENERATION type |
| prompt_id | BIGINT | Nullable, no FK constraint — FK added in Phase 2 migration |
| prompt_version_id | BIGINT | Nullable, no FK constraint — FK added in Phase 2 migration |
| input | TEXT | Span input |
| output | TEXT | Span output |
| metadata | TEXT | JSON key-value map |
| input_tokens | INT | |
| output_tokens | INT | |
| cost | DECIMAL(10,6) | |
| latency_ms | INT | |
| status | INT | ACTIVE (0), COMPLETED (1), ERROR (2) |
| level | INT | DEBUG (0), DEFAULT (1), WARNING (2), ERROR (3) |
| start_time | TIMESTAMP | |
| end_time | TIMESTAMP | |
| created_date | TIMESTAMP NOT NULL | |
| version | BIGINT NOT NULL | |

Indexes: trace_id, parent_span_id, type, model, created_date

#### ai_observability_session

| Column | Type | Description |
|---|---|---|
| id | BIGINT PK | Auto-increment |
| workspace_id | BIGINT NOT NULL | |
| project_id | BIGINT | |
| name | VARCHAR(256) | Optional session name |
| user_id | VARCHAR(256) | End user identifier |
| created_date | TIMESTAMP NOT NULL | |
| last_modified_date | TIMESTAMP NOT NULL | |
| version | BIGINT NOT NULL | |

Indexes: workspace_id, project_id, user_id

#### ai_observability_trace_tag (join table)

| Column | Type | Description |
|---|---|---|
| ai_observability_trace | BIGINT FK | |
| tag_id | BIGINT FK | References existing tag infrastructure |

### API — Header-Based Integration

Callers opt into tracing via HTTP headers on gateway requests:

| Header | Purpose |
|---|---|
| `X-ByteChef-Trace-Id` | Correlate multiple requests into one trace |
| `X-ByteChef-Session-Id` | Group traces into a session |
| `X-ByteChef-Span-Name` | Name the current span |
| `X-ByteChef-Parent-Span-Id` | Build parent-child span hierarchy |
| `X-ByteChef-User-Id` | Identify the end user |
| `X-ByteChef-Metadata-*` | Arbitrary key-value metadata |

When tracing headers are present, `AiGatewayFacade` creates trace/span records in addition to the existing request log. The request log continues to work unchanged for callers that don't use tracing headers.

### UI — New Sidebar Tabs

**Traces tab**:
- Filterable list: time range, model, user, status, tags, source
- Click trace to see span tree with timing waterfall visualization
- Span detail: input/output, tokens, cost, latency, metadata, model/provider

**Sessions tab**:
- List of sessions with user, trace count, time range
- Click session to see all traces in conversation order

### GraphQL

New queries: `aiObservabilityTraces`, `aiObservabilityTrace`, `aiObservabilitySpans`, `aiObservabilitySessions`, `aiObservabilitySession`

---

## Phase 2: Prompt Management

### Purpose

Version-controlled prompts served through the gateway with environment-based deployment and no-code rollback.

### Data Model

#### ai_observability_prompt

| Column | Type | Description |
|---|---|---|
| id | BIGINT PK | |
| workspace_id | BIGINT NOT NULL | |
| project_id | BIGINT | |
| name | VARCHAR(256) NOT NULL | Unique within project |
| description | TEXT | |
| created_date | TIMESTAMP NOT NULL | |
| last_modified_date | TIMESTAMP NOT NULL | |
| version | BIGINT NOT NULL | |

Unique constraint: (workspace_id, project_id, name)

#### ai_observability_prompt_version

| Column | Type | Description |
|---|---|---|
| id | BIGINT PK | |
| prompt_id | BIGINT FK NOT NULL | |
| version_number | INT NOT NULL | Auto-increment per prompt |
| type | INT | TEXT (0), CHAT (1) |
| content | TEXT NOT NULL | Prompt template (plain text or JSON message array) |
| variables | TEXT | JSON array of variable definitions [{name, type, default}] |
| environment | VARCHAR(64) | "production", "staging", "development" |
| commit_message | VARCHAR(512) | What changed in this version |
| active | BOOLEAN NOT NULL | Is this the active version for its environment |
| created_by | VARCHAR(50) NOT NULL | |
| created_date | TIMESTAMP NOT NULL | |

Unique constraint: (prompt_id, version_number)

### Variable Syntax

`{{variable_name}}` in prompt content. Variables are resolved from the request body at gateway time.

### Environment-Based Deployment

Each prompt can have different active versions per environment. Only one version is active per (prompt, environment) pair. Promoting staging to production is a single mutation that sets `active = true` on the staging version's content for the production environment.

### Linking to Traces

When a prompt is used via header, the generated span records `prompt_id` and `prompt_version_id` (columns already defined in `ai_observability_span`).

### API — Header-Based

| Header | Purpose |
|---|---|
| `X-ByteChef-Prompt-Name` | Resolve prompt by name |
| `X-ByteChef-Prompt-Environment` | Which environment's active version to use (default: production) |

Gateway resolves the active version, substitutes variables from the request body, sends to the LLM.

### UI — New Sidebar Tab: Prompts

- Prompt list: name, latest version, active environments
- Prompt detail: version history with diffs, per-version performance metrics (avg latency, cost, error rate from linked spans)
- Version editor: content editor with variable extraction, commit message
- Environment controls: set active version per environment, one-click rollback

### GraphQL

Queries: `aiObservabilityPrompts`, `aiObservabilityPrompt`, `aiObservabilityPromptVersions`
Mutations: `createAiObservabilityPrompt`, `createAiObservabilityPromptVersion`, `setActivePromptVersion`, `deleteAiObservabilityPrompt`

---

## Phase 3: Alerting & Notifications

### Purpose

Threshold-based alerts on gateway metrics with multi-channel notifications.

### Data Model

#### ai_observability_alert_rule

| Column | Type | Description |
|---|---|---|
| id | BIGINT PK | |
| workspace_id | BIGINT NOT NULL | |
| project_id | BIGINT | Optional project scope |
| name | VARCHAR(256) NOT NULL | |
| metric | INT | ERROR_RATE (0), LATENCY_P95 (1), COST (2), TOKEN_USAGE (3), REQUEST_VOLUME (4) |
| condition | INT | GREATER_THAN (0), LESS_THAN (1), EQUALS (2) |
| threshold | DECIMAL(12,4) NOT NULL | |
| window_minutes | INT NOT NULL | Evaluation window |
| cooldown_minutes | INT NOT NULL | Suppress repeated alerts |
| filters | TEXT | JSON — optional model/provider/environment scoping |
| enabled | BOOLEAN NOT NULL | |
| created_date | TIMESTAMP NOT NULL | |
| last_modified_date | TIMESTAMP NOT NULL | |
| version | BIGINT NOT NULL | |

#### ai_observability_notification_channel

| Column | Type | Description |
|---|---|---|
| id | BIGINT PK | |
| workspace_id | BIGINT NOT NULL | |
| name | VARCHAR(256) NOT NULL | |
| type | INT | WEBHOOK (0), EMAIL (1), SLACK (2) |
| config | TEXT NOT NULL | JSON (encrypted where needed). WEBHOOK: {url, headers, method}. EMAIL: {recipients[]}. SLACK: {webhookUrl, channel} |
| enabled | BOOLEAN NOT NULL | |
| created_date | TIMESTAMP NOT NULL | |
| last_modified_date | TIMESTAMP NOT NULL | |
| version | BIGINT NOT NULL | |

#### ai_observability_alert_rule_channel (join table)

| Column | Type | Description |
|---|---|---|
| alert_rule_id | BIGINT FK | |
| notification_channel_id | BIGINT FK | |

#### ai_observability_alert_event

| Column | Type | Description |
|---|---|---|
| id | BIGINT PK | |
| alert_rule_id | BIGINT FK NOT NULL | |
| triggered_value | DECIMAL(12,4) | Actual metric value that triggered |
| message | TEXT | Human-readable summary |
| status | INT | TRIGGERED (0), RESOLVED (1), ACKNOWLEDGED (2) |
| created_date | TIMESTAMP NOT NULL | |

### Scheduler Abstraction

Alert evaluation uses a new scheduler abstraction in `platform-scheduler`, following the `TriggerScheduler` pattern:

```java
// In platform-scheduler-api
public interface AlertScheduler {

    void scheduleAlertEvaluation(long alertRuleId, int windowMinutes);

    void cancelAlertEvaluation(long alertRuleId);
}
```

Implementation in `platform-scheduler-impl` uses the same underlying scheduling infrastructure that backs `TriggerScheduler`. The gateway module depends on `platform-scheduler-api`, not the implementation.

### Evaluation Flow

1. When an alert rule is created/enabled, `AlertScheduler.scheduleAlertEvaluation()` is called
2. On each scheduled tick, the evaluator queries request log / trace data within `window_minutes`
3. If threshold is breached and cooldown has elapsed, creates `ai_observability_alert_event` and sends notifications to linked channels
4. When alert rule is disabled/deleted, `AlertScheduler.cancelAlertEvaluation()` is called

### UI — New Sidebar Tab: Alerts

- Alert rules list: name, metric, threshold, enabled, last triggered timestamp
- Create/edit dialog: metric dropdown, condition, threshold, window, cooldown, notification channel selection
- Notification channels management sub-section
- Alert history timeline: fired alerts with triggered value, status

### GraphQL

Queries: `aiObservabilityAlertRules`, `aiObservabilityAlertRule`, `aiObservabilityAlertEvents`, `aiObservabilityNotificationChannels`
Mutations: CRUD for alert rules, notification channels

---

## Phase 4: Playground

### Purpose

Interactive prompt testing with model switching directly in the gateway UI.

### Data Model

No new tables. Playground uses existing gateway infrastructure (`AiGatewayFacade`) and Phase 1 tracing. Playground requests are marked with `source = PLAYGROUND` on the trace.

### How It Works

The playground sends requests through `AiGatewayFacade`, so routing, cost tracking, and tracing all work automatically. The monitoring dashboard filters by `source = API` by default, keeping playground traffic separate from production analytics.

### UI — New Sidebar Tab: Playground

- **Model selector**: dropdown of all configured models across providers
- **Prompt input**:
  - Text mode: single text area
  - Chat mode: message list (system, user, assistant) with add/remove
  - Managed prompt mode: select a prompt by name, auto-load template with variable input fields
- **Parameters panel**: temperature, max_tokens, top_p, etc.
- **Response panel**: rendered output with token count, cost, latency
- **Side-by-side comparison**: run same prompt against 2 models simultaneously, results in two columns
- **Trace link**: every execution creates a trace viewable in the Traces tab

### GraphQL

No new schema needed — playground uses existing gateway chat completion mutations.

---

## Phase 5: User/Tag Analytics & Rate Limiting

### Purpose

Per-user tracking, custom property segmentation, and configurable rate limiting with pluggable backend (in-memory or Redis).

### Data Model

#### ai_gateway_rate_limit

| Column | Type | Description |
|---|---|---|
| id | BIGINT PK | |
| workspace_id | BIGINT NOT NULL | |
| project_id | BIGINT | |
| name | VARCHAR(256) NOT NULL | |
| scope | INT | GLOBAL (0), PER_USER (1), PER_PROPERTY (2) |
| property_key | VARCHAR(256) | When scope=PER_PROPERTY (e.g., "organization_id") |
| limit_type | INT | REQUESTS (0), TOKENS (1), COST (2) |
| limit_value | INT NOT NULL | |
| window_seconds | INT NOT NULL | |
| enabled | BOOLEAN NOT NULL | |
| created_date | TIMESTAMP NOT NULL | |
| last_modified_date | TIMESTAMP NOT NULL | |
| version | BIGINT NOT NULL | |

#### ai_gateway_custom_property

| Column | Type | Description |
|---|---|---|
| id | BIGINT PK | |
| trace_id | BIGINT FK | Nullable, link to trace |
| request_log_id | BIGINT FK | Nullable, link to request log |
| key | VARCHAR(256) NOT NULL | |
| value | VARCHAR(1024) NOT NULL | |
| workspace_id | BIGINT NOT NULL | |

Indexes: trace_id, request_log_id, (workspace_id, key)

### Rate Limiter Abstraction

```java
// In ai-gateway-api
public interface AiGatewayRateLimiter {

    RateLimitResult tryAcquire(String key, int limit, int windowSeconds);

    void reset(String key);
}
```

Two implementations:

- **InMemoryAiGatewayRateLimiter** (default) — `ConcurrentHashMap` with sliding window counters. Suitable for single-instance deployments.
- **RedisAiGatewayRateLimiter** — Redis `INCR` + `EXPIRE` for distributed sliding windows. Required for multi-instance deployments.

### Configuration

```yaml
# application-bytechef.yml
bytechef:
  ai:
    gateway:
      rate-limiting:
        enabled: true          # default: false
        provider: memory       # memory (default) | redis
```

Conditional bean registration via `@ConditionalOnProperty`.

### Rate Limiting Flow

Evaluated in `AiGatewayFacade` pre-request pipeline (alongside budget checking):
1. Extract user ID and custom properties from headers
2. Look up applicable rate limit rules for the workspace/project
3. Call `AiGatewayRateLimiter.tryAcquire()` for each matching rule
4. If any limit exceeded, return HTTP 429 with `X-RateLimit-Remaining` and `X-RateLimit-Reset` headers

### Headers

| Header | Purpose |
|---|---|
| `X-ByteChef-User-Id` | Already from Phase 1, now also used for per-user rate limiting |
| `X-ByteChef-Property-*` | Arbitrary key-value properties stored and used for per-property rate limiting and analytics |

### User Analytics

No separate tables. Built as aggregations over traces + custom properties:
- Per-user: request count, total cost, total tokens, avg latency, error rate
- Per-property: same aggregations sliced by any custom property
- Exposed as new filter dimensions in the existing Monitoring dashboard

### UI Changes

- New sidebar tab: **Rate Limits** — list of rate limit rules, create/edit/delete
- **Monitoring** tab enhanced: user filter dropdown, custom property filters, top users table widget

### GraphQL

Queries: `aiGatewayRateLimits`, `aiGatewayRateLimit`, `aiGatewayUserAnalytics`
Mutations: CRUD for rate limits

---

## Phase 6: Data Export

### Purpose

Get observability data out of ByteChef for external analysis, integration, and event-driven workflows.

### Data Model

#### ai_observability_export_job

| Column | Type | Description |
|---|---|---|
| id | BIGINT PK | |
| workspace_id | BIGINT NOT NULL | |
| project_id | BIGINT | |
| type | INT | ON_DEMAND (0), SCHEDULED (1) |
| format | INT | CSV (0), JSON (1), JSONL (2) |
| scope | INT | TRACES (0), REQUEST_LOGS (1), SESSIONS (2), PROMPTS (3) |
| filters | TEXT | JSON — date range, model, user, status filters |
| status | INT | PENDING (0), PROCESSING (1), COMPLETED (2), FAILED (3) |
| file_path | VARCHAR(512) | Path in file storage |
| record_count | INT | |
| error_message | TEXT | |
| created_by | VARCHAR(50) NOT NULL | |
| created_date | TIMESTAMP NOT NULL | |

#### ai_observability_webhook_subscription

| Column | Type | Description |
|---|---|---|
| id | BIGINT PK | |
| workspace_id | BIGINT NOT NULL | |
| project_id | BIGINT | |
| name | VARCHAR(256) NOT NULL | |
| url | VARCHAR(1024) NOT NULL | |
| secret | VARCHAR(256) | HMAC signature verification |
| events | TEXT NOT NULL | JSON array, e.g., ["trace.completed", "alert.triggered", "budget.exceeded"] |
| enabled | BOOLEAN NOT NULL | |
| last_triggered_date | TIMESTAMP | |
| created_date | TIMESTAMP NOT NULL | |
| last_modified_date | TIMESTAMP NOT NULL | |
| version | BIGINT NOT NULL | |

### Export Execution

Uses existing `platform-file-storage` abstraction. On-demand exports triggered via GraphQL mutation, processed asynchronously. Scheduled exports use the same scheduler abstraction from Phase 3.

### Webhook Delivery

When subscribed events occur (trace completed, alert triggered, budget exceeded), the system posts a signed JSON payload (HMAC-SHA256 using `secret`) to the subscriber's URL. Retry with exponential backoff on failure (3 attempts).

### UI — New Sidebar Tab: Exports

- On-demand export: select scope, filters, format, trigger export, download when ready
- Export history: list of past exports with status, record count, download link
- Webhook subscriptions: list, create/edit/delete, event selection, test webhook button

### GraphQL

Queries: `aiObservabilityExportJobs`, `aiObservabilityExportJob`, `aiObservabilityWebhookSubscriptions`
Mutations: `createAiObservabilityExportJob`, CRUD for webhook subscriptions

---

## Module Structure

All new code lives within the existing `automation-ai-gateway` module hierarchy under `server/ee/libs/automation/automation-ai/automation-ai-gateway/`:

- **automation-ai-gateway-api**: New domain classes, service interfaces, `AiGatewayRateLimiter` interface
- **automation-ai-gateway-service**: Service implementations, rate limiter implementations, alert evaluation
- **automation-ai-gateway-graphql**: New GraphQL controllers, schema extensions
- **automation-ai-gateway-public-rest**: Extended REST endpoints (tracing headers processing)

New `platform-scheduler-api` interface: `AlertScheduler` alongside existing `TriggerScheduler`.

## Phase 7: Evaluation & Scoring

### Purpose

Gateway-native evaluation and scoring for production traces. Supports manual annotation, programmatic API scoring, and automated LLM-as-judge evaluation with configurable rules and sampling.

### Data Model

#### ai_observability_score

| Column | Type | Description |
|---|---|---|
| id | BIGINT PK | |
| workspace_id | BIGINT NOT NULL | |
| trace_id | BIGINT FK NOT NULL | Parent trace |
| span_id | BIGINT FK | Optional, score a specific span |
| name | VARCHAR(256) NOT NULL | Score name (e.g., "relevance", "helpfulness", "safety") |
| value | DECIMAL(10,4) | Numeric value (0-1 normalized, or custom range) |
| string_value | VARCHAR(256) | For CATEGORICAL type (e.g., "good", "bad") |
| data_type | INT | NUMERIC (0), BOOLEAN (1), CATEGORICAL (2) |
| source | INT | MANUAL (0), API (1), LLM_JUDGE (2) |
| comment | TEXT | Optional annotation comment |
| eval_rule_id | BIGINT FK | If scored by an eval rule, link to it |
| created_by | VARCHAR(50) | Who created the score (user or "system") |
| created_date | TIMESTAMP NOT NULL | |

Indexes: trace_id, span_id, name, source, created_date

#### ai_observability_score_config

| Column | Type | Description |
|---|---|---|
| id | BIGINT PK | |
| workspace_id | BIGINT NOT NULL | |
| name | VARCHAR(256) NOT NULL | Score dimension name |
| data_type | INT | NUMERIC (0), BOOLEAN (1), CATEGORICAL (2) |
| min_value | DECIMAL(10,4) | For NUMERIC: minimum value |
| max_value | DECIMAL(10,4) | For NUMERIC: maximum value |
| categories | TEXT | For CATEGORICAL: JSON array of allowed values |
| description | TEXT | What this score measures |
| created_date | TIMESTAMP NOT NULL | |
| last_modified_date | TIMESTAMP NOT NULL | |
| version | BIGINT NOT NULL | |

Unique constraint: (workspace_id, name)

#### ai_observability_eval_rule

| Column | Type | Description |
|---|---|---|
| id | BIGINT PK | |
| workspace_id | BIGINT NOT NULL | |
| project_id | BIGINT | Optional project scope |
| name | VARCHAR(256) NOT NULL | Rule name (e.g., "Relevance check on production") |
| score_config_id | BIGINT FK NOT NULL | Which score dimension to produce |
| prompt_template | TEXT NOT NULL | LLM prompt template with variables: {{input}}, {{output}}, {{metadata}} |
| model | VARCHAR(256) NOT NULL | Which model to use for evaluation (e.g., "openai/gpt-4o-mini") |
| filters | TEXT | JSON — trace filters (model, user, source, environment) |
| sampling_rate | DECIMAL(5,4) NOT NULL | 0.0-1.0, fraction of matching traces to evaluate |
| delay_seconds | INT | Delay before evaluating (wait for trace to complete) |
| enabled | BOOLEAN NOT NULL | |
| created_date | TIMESTAMP NOT NULL | |
| last_modified_date | TIMESTAMP NOT NULL | |
| version | BIGINT NOT NULL | |

#### ai_observability_eval_execution

| Column | Type | Description |
|---|---|---|
| id | BIGINT PK | |
| eval_rule_id | BIGINT FK NOT NULL | |
| trace_id | BIGINT FK NOT NULL | |
| score_id | BIGINT FK | The resulting score (null if failed) |
| status | INT | PENDING (0), COMPLETED (1), ERROR (2) |
| error_message | TEXT | |
| created_date | TIMESTAMP NOT NULL | |

### Evaluation Flow

1. When a trace completes, the system checks all enabled eval rules
2. For each matching rule (filters match the trace), apply sampling rate
3. If sampled, create an `ai_observability_eval_execution` with status PENDING
4. After `delay_seconds`, run the evaluation:
   a. Build the LLM prompt from `prompt_template`, substituting trace input/output/metadata
   b. Call the specified model via the gateway's own `AiGatewayChatModelFactory`
   c. Parse the LLM response as a score value
   d. Create an `ai_observability_score` with source=LLM_JUDGE
   e. Update execution status to COMPLETED (or ERROR)

### Scoring API

**Manual scoring**: GraphQL mutation from the UI (user annotates a trace in the trace detail view)

**Programmatic scoring**: Public REST endpoint for external evaluators:
- `POST /api/ai-gateway/v1/scores` with body: `{ traceId, spanId?, name, value, dataType, comment? }`
- Authenticated via API key (same as other gateway endpoints)

### UI — New Sidebar Tab: Scores

- **Score configs list**: Define score dimensions (name, type, range/categories)
- **Eval rules list**: Create/edit/delete eval rules with prompt template, model, filters, sampling
- **Score analytics**: Distribution charts per score dimension, trends over time
- **Trace detail enhancement**: Add inline scoring controls (thumbs up/down, star rating, category dropdown) in the existing trace detail view

### GraphQL

Queries: `aiObservabilityScores`, `aiObservabilityScoreConfigs`, `aiObservabilityEvalRules`, `aiObservabilityEvalExecutions`
Mutations: CRUD for score configs, eval rules. `createAiObservabilityScore` for manual/API scoring.

---

## UI Structure

All new tabs added to the existing AI Gateway sidebar in `client/src/pages/automation/ai-gateway/`:

| Existing Tabs | New Tabs (by phase) |
|---|---|
| Providers | Traces (Phase 1) |
| Models | Sessions (Phase 1) |
| Projects | Prompts (Phase 2) |
| Routing Policies | Alerts (Phase 3) |
| Budget | Playground (Phase 4) |
| Settings | Rate Limits (Phase 5) |
| Monitoring | Exports (Phase 6) |
| | Scores (Phase 7) |

## Phase Dependencies

```
Phase 1 (Tracing & Sessions) ─── foundation for all
  ├── Phase 2 (Prompt Management) ─── links prompts to spans
  ├── Phase 3 (Alerting) ─── evaluates against trace/request data
  │     └── Phase 6 (Data Export) ─── reuses scheduler, webhook events include alerts
  ├── Phase 4 (Playground) ─── creates traces with source=PLAYGROUND
  ├── Phase 5 (Rate Limiting & User Analytics) ─── uses user_id from traces
  └── Phase 7 (Evaluation & Scoring) ─── scores attached to traces/spans
```

Phase 1 must come first. Phases 2-5 can be parallelized after Phase 1 is complete. Phase 6 benefits from all prior phases but only strictly depends on Phase 1 and Phase 3 (scheduler pattern).
