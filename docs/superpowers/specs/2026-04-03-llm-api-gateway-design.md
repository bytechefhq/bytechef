# LLM API Gateway — Design Specification

**Date:** 2026-04-03
**Status:** Draft
**Module:** `server/ee/libs/automation/automation-ai/automation-ai-llm-gateway/`

## 1. Overview

The LLM API Gateway is a unified proxy layer that sits between ByteChef users and multiple LLM providers (OpenAI, Anthropic, Google Gemini, Azure OpenAI, Mistral, Cohere, DeepSeek, Groq). It provides a single OpenAI-compatible REST API, intelligent routing, automatic failover, spend tracking, response caching, and a web UI for configuration and monitoring.

Inspired by [Merge Gateway](https://www.merge.dev/gateway) and [LiteLLM](https://www.litellm.ai/), built on Spring Boot and Spring AI.

## 2. Functionalities

### F1. Provider Management
Configure and manage LLM provider connections.

- CRUD operations for provider configurations
- Supported provider types: `OPENAI`, `ANTHROPIC`, `GOOGLE_GEMINI`, `AZURE_OPENAI`, `MISTRAL`, `COHERE`, `DEEPSEEK`, `GROQ`
- Each provider stores: name, type (enum), API key (encrypted), base URL (optional override), enabled flag, custom config (JSON)
- Test connectivity: send a lightweight request to validate credentials
- API keys encrypted at rest using ByteChef's existing `EncryptionService`

### F2. Model Registry
Catalog of available models across all configured providers.

- Each model belongs to a provider
- Fields: name (e.g., `gpt-4o`), alias (optional logical name), context window size, input cost per 1M tokens, output cost per 1M tokens, capabilities (chat, embeddings, vision), enabled flag
- Provider-prefixed addressing: `openai/gpt-4o`, `anthropic/claude-sonnet-4-20250514`
- Manual model registration (auto-discovery deferred to future)

### F3. Core Gateway API (Public REST API)
Unified OpenAI-compatible HTTP API.

**Endpoints:**

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/llm-gateway/v1/chat/completions` | Chat completions (streaming + non-streaming) |
| `POST` | `/api/llm-gateway/v1/embeddings` | Generate embeddings |
| `GET` | `/api/llm-gateway/v1/models` | List available models |
| `GET` | `/api/llm-gateway/v1/models/{model}` | Retrieve model details |

**Request format (chat completions):**
```json
{
  "model": "openai/gpt-4o",
  "messages": [
    {"role": "system", "content": "You are a helpful assistant."},
    {"role": "user", "content": "Hello"}
  ],
  "temperature": 0.7,
  "max_tokens": 1000,
  "stream": false,
  "routing_policy": "my-policy",
  "cache": true
}
```

**Response format:** Standard OpenAI chat completion response with added `x-gateway-*` headers:
- `x-gateway-provider`: actual provider used
- `x-gateway-model`: actual model used
- `x-gateway-latency-ms`: total latency
- `x-gateway-cache-hit`: true/false
- `x-gateway-routing-policy`: policy applied
- `x-gateway-request-id`: unique request ID

**Streaming:** SSE format matching OpenAI's `text/event-stream` response with `data: [DONE]` termination.

**OpenAI SDK compatibility:** Users can point any OpenAI SDK at `https://<host>/api/gateway/v1` by setting `base_url`.

### F4. API Key Management
Virtual API keys for gateway access control.

- Generate gateway API keys (shown once, stored as SHA-256 hash)
- Fields: name, key hash, associated project ID (optional), tags (list), rate limit (requests/min), enabled, created date, last used date, expires date (optional)
- Authentication: `Authorization: Bearer <gateway-api-key>` on all `/api/llm-gateway/v1/**` endpoints
- Rate limiting: in-memory sliding window counter per key, reject with `429 Too Many Requests` when exceeded
- Key context (project, tags) carried through to request logs for spend tracking

### F5. Routing Policies & Strategies
Configurable routing logic for model selection.

**Routing Policy entity:**
- name (unique identifier, used in requests)
- strategy (enum)
- list of model deployments
- fallback model (optional)
- config JSON (strategy-specific parameters)

**Model Deployment (within a policy):**
- model ID reference
- weight (for weighted strategies, default 1)
- priority/order (for fallback, lower = higher priority)
- max RPM / max TPM (rate limits for this deployment)
- enabled flag

**Strategies:**

| Strategy | Behavior |
|----------|----------|
| `SIMPLE` | Route directly to the specified model. No load balancing. |
| `WEIGHTED_RANDOM` | Distribute requests across deployments proportional to weight. |
| `COST_OPTIMIZED` | Select the cheapest available deployment based on model pricing. |
| `LATENCY_OPTIMIZED` | Select the deployment with lowest recent average latency (sliding window). |
| `PRIORITY_FALLBACK` | Try deployments in priority order. Failover to next on error. |

**Resolution order:**
1. Request-level `routing_policy` field → named policy
2. Model-level default policy (if configured)
3. Direct routing to the specified `model` (no policy)

### F6. Reliability & Failover

- **Retries:** Configurable max retries (default 2) with exponential backoff (base 1s, max 8s). Retry on 5xx, timeout, connection error. Do not retry on 4xx (except 429).
- **Failover:** When a deployment fails and a routing policy has multiple deployments, automatically try the next one per strategy ordering.
- **Cooldown:** Track consecutive failure count per deployment. After N failures (default 3), mark cooled down for configurable duration (default 60s). Skip cooled-down deployments in routing. Reset on success.
- **Timeout:** Configurable per-provider request timeout (default 30s). Cancel request and failover on timeout.
- **Rate limit awareness:** Track RPM/TPM per deployment. Skip deployments at capacity during routing.

### F7. Request Logging & Observability

Log every gateway request asynchronously.

**Log entry fields:**
- request ID (UUID)
- API key ID
- requested model (what user asked for)
- routed model + provider (what was actually used)
- routing policy ID + strategy
- latency (ms)
- input tokens, output tokens
- calculated cost
- HTTP status
- error message (if any)
- cache hit (boolean)
- created date

**Storage:** PostgreSQL table with indexes on (created_date, api_key_id, model, provider, status).

**Retention:** Configurable retention period (default 30 days). Scheduled cleanup job.

**Query API:** Filterable by date range, model, provider, status, API key. Paginated. Exposed via GraphQL for UI and REST for programmatic access.

### F8. Spend Tracking & Limits

- **Cost calculation:** `(input_tokens / 1M * input_cost) + (output_tokens / 1M * output_cost)` per request. Pricing from model registry.
- **Aggregation:** Periodic aggregation (hourly) into summary table by API key, model, provider, date.
- **Budgets:** Per API key or per tag. Soft limit (log warning, include `x-gateway-budget-warning` header). Hard limit (reject with `402 Payment Required`).
- **Spend query:** Totals, breakdowns by dimension, time series. GraphQL API for UI.

### F9. Response Caching

- **Cache key:** SHA-256 hash of `(model + messages + temperature + max_tokens + top_p)`. Ignores streaming flag.
- **Storage:** Spring Cache abstraction — backed by Redis in production, simple/caffeine in development.
- **TTL:** Configurable per model (default 1 hour). `0` = no caching for that model.
- **Bypass:** Set `"cache": false` in request body or `X-Gateway-Cache: false` header.
- **Metrics:** Track hit/miss counts per model. Expose in monitoring.
- **Limitations:** Only cache non-streaming, deterministic requests (temperature=0 cached by default, temperature>0 only if explicitly enabled).

### F10. UI — Configuration Pages

New "AI Gateway" page as last item in automation sidebar.

**Sub-pages (left sidebar navigation):**

| Page | Content |
|------|---------|
| **Providers** | Table of configured providers. Add/edit dialog with fields: name, type (dropdown), API key (masked), base URL. Test connection button. Enable/disable toggle. |
| **Models** | Table of models grouped by provider. Columns: name, alias, context window, pricing, capabilities, enabled. Edit dialog for alias and pricing. |
| **API Keys** | Table of keys (masked). Create dialog (shows key once). Columns: name, last used, rate limit, project, status. Revoke action. |
| **Routing** | Table of routing policies. Create/edit dialog: name, strategy dropdown, model deployment list (add/remove/reorder with weights and priorities). |
| **Settings** | Form for global settings: default retry count, default timeout, caching enabled, cache TTL, log retention days, budget alert threshold. |

**UI patterns:** Follow existing ByteChef patterns — `LayoutContainer`, `Header`, `LeftSidebarNav`, `LeftSidebarNavItem`. Dialogs using existing dialog components. Tables with existing table components.

### F11. UI — Monitoring Dashboard

**Dashboard page** (separate sub-page or tab within AI Gateway):

| Widget | Type | Description |
|--------|------|-------------|
| Summary cards | Stats | Total requests (24h), avg latency, error rate, total spend (period) |
| Request volume | Line chart | Requests over time, grouped by model or provider |
| Latency | Line chart | P50, P95, P99 latency over time |
| Error rate | Bar chart | Error rate by provider |
| Cost breakdown | Pie/bar chart | Spend by model, provider, or API key |
| Budget tracker | Progress bar | Spend vs. budget per key/project |
| Request log | Table | Recent requests with search, filters (model, status, date), expandable rows |

**Time range selector:** Last 1h, 6h, 24h, 7d, 30d, custom range.

## 3. Module Architecture

```
server/libs/automation/automation-ai/automation-ai-llm-gateway/
├── automation-ai-llm-gateway-api/
│   └── src/main/java/com/bytechef/automation/ai/llmgateway/
│       ├── domain/
│       │   ├── AiLlmGatewayProvider.java
│       │   ├── AiLlmGatewayModel.java
│       │   ├── AiLlmGatewayApiKey.java
│       │   ├── AiLlmGatewayRoutingPolicy.java
│       │   ├── AiLlmGatewayModelDeployment.java
│       │   ├── AiLlmGatewayRequestLog.java
│       │   └── AiLlmGatewaySpendSummary.java
│       ├── repository/
│       │   ├── AiLlmGatewayProviderRepository.java
│       │   ├── AiLlmGatewayModelRepository.java
│       │   ├── AiLlmGatewayApiKeyRepository.java
│       │   ├── AiLlmGatewayRoutingPolicyRepository.java
│       │   ├── AiLlmGatewayRequestLogRepository.java
│       │   └── AiLlmGatewaySpendSummaryRepository.java
│       └── service/
│           ├── AiLlmGatewayProviderService.java
│           ├── AiLlmGatewayModelService.java
│           ├── AiLlmGatewayApiKeyService.java
│           ├── AiLlmGatewayRoutingPolicyService.java
│           ├── AiLlmGatewayRequestLogService.java
│           └── AiLlmGatewaySpendService.java
│
├── automation-ai-llm-gateway-service/
│   └── src/main/java/com/bytechef/automation/ai/llmgateway/
│       ├── config/
│       │   └── AiLlmGatewayConfiguration.java
│       ├── facade/
│       │   ├── AiLlmGatewayFacade.java          (orchestrates chat completion flow)
│       │   └── AiLlmGatewayProviderFacade.java
│       ├── routing/
│       │   ├── AiLlmGatewayRouter.java
│       │   ├── AiLlmGatewayRoutingStrategy.java  (interface)
│       │   ├── SimpleRoutingStrategy.java
│       │   ├── WeightedRandomRoutingStrategy.java
│       │   ├── CostOptimizedRoutingStrategy.java
│       │   ├── LatencyOptimizedRoutingStrategy.java
│       │   └── PriorityFallbackRoutingStrategy.java
│       ├── provider/
│       │   └── AiLlmGatewayChatModelFactory.java  (creates ChatModel per provider type)
│       ├── cache/
│       │   └── AiLlmGatewayResponseCache.java
│       ├── reliability/
│       │   ├── AiLlmGatewayRetryHandler.java
│       │   └── AiLlmGatewayCooldownTracker.java
│       ├── cost/
│       │   └── AiLlmGatewayCostCalculator.java
│       └── service/
│           ├── AiLlmGatewayProviderServiceImpl.java
│           ├── AiLlmGatewayModelServiceImpl.java
│           ├── AiLlmGatewayApiKeyServiceImpl.java
│           ├── AiLlmGatewayRoutingPolicyServiceImpl.java
│           ├── AiLlmGatewayRequestLogServiceImpl.java
│           └── AiLlmGatewaySpendServiceImpl.java
│
├── automation-ai-llm-gateway-rest/
│   ├── automation-ai-llm-gateway-rest-api/
│   │   └── (OpenAPI-generated interfaces)
│   └── automation-ai-llm-gateway-rest-impl/
│       └── src/main/java/com/bytechef/automation/ai/llmgateway/web/rest/
│           ├── AiLlmGatewayChatCompletionController.java
│           ├── AiLlmGatewayEmbeddingController.java
│           ├── AiLlmGatewayModelController.java
│           ├── AiLlmGatewayProviderApiController.java
│           ├── AiLlmGatewayApiKeyApiController.java
│           └── AiLlmGatewayRoutingPolicyApiController.java
│
└── automation-ai-llm-gateway-graphql/
    └── src/main/java/com/bytechef/automation/ai/llmgateway/web/graphql/
        ├── AiLlmGatewayProviderGraphQlController.java
        ├── AiLlmGatewayModelGraphQlController.java
        ├── AiLlmGatewayApiKeyGraphQlController.java
        ├── AiLlmGatewayRoutingPolicyGraphQlController.java
        ├── AiLlmGatewayRequestLogGraphQlController.java
        └── AiLlmGatewaySpendGraphQlController.java
```

### Client-Side Structure

```
client/src/pages/automation/ai-gateway/
├── AiGateway.tsx                    (main page with left sidebar nav)
├── components/
│   ├── providers/
│   │   ├── AiGatewayProviders.tsx
│   │   └── AiGatewayProviderDialog.tsx
│   ├── models/
│   │   ├── AiGatewayModels.tsx
│   │   └── AiGatewayModelDialog.tsx
│   ├── api-keys/
│   │   ├── AiGatewayApiKeys.tsx
│   │   └── AiGatewayApiKeyDialog.tsx
│   ├── routing/
│   │   ├── AiGatewayRoutingPolicies.tsx
│   │   └── AiGatewayRoutingPolicyDialog.tsx
│   ├── settings/
│   │   └── AiGatewaySettings.tsx
│   └── monitoring/
│       ├── AiGatewayDashboard.tsx
│       ├── AiGatewayRequestLog.tsx
│       └── charts/
│           ├── RequestVolumeChart.tsx
│           ├── LatencyChart.tsx
│           ├── ErrorRateChart.tsx
│           └── CostBreakdownChart.tsx
```

## 4. Database Schema

### Tables

```sql
-- F1: Provider Management
CREATE TABLE ai_llm_gateway_provider (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    name         VARCHAR(256) NOT NULL,
    type         VARCHAR(64)  NOT NULL,  -- enum: OPENAI, ANTHROPIC, etc.
    api_key      VARCHAR(1024) NOT NULL, -- encrypted
    base_url     VARCHAR(512),
    enabled      BOOLEAN NOT NULL DEFAULT TRUE,
    config       TEXT,                   -- JSON for provider-specific settings
    created_by   VARCHAR(256),
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_by   VARCHAR(256),
    last_modified_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version      INT NOT NULL DEFAULT 0
);

-- F2: Model Registry
CREATE TABLE ai_llm_gateway_model (
    id                     BIGINT PRIMARY KEY AUTO_INCREMENT,
    provider_id            BIGINT NOT NULL REFERENCES ai_llm_gateway_provider(id),
    name                   VARCHAR(256) NOT NULL,
    alias                  VARCHAR(256),
    context_window         INT,
    input_cost_per_m_tokens  DECIMAL(10,4),
    output_cost_per_m_tokens DECIMAL(10,4),
    capabilities           VARCHAR(256),  -- comma-separated: CHAT,EMBEDDINGS,VISION
    enabled                BOOLEAN NOT NULL DEFAULT TRUE,
    created_date           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_date     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version                INT NOT NULL DEFAULT 0
);

-- F4: API Key Management
CREATE TABLE ai_llm_gateway_api_key (
    id             BIGINT PRIMARY KEY AUTO_INCREMENT,
    name           VARCHAR(256) NOT NULL,
    key_hash       VARCHAR(128) NOT NULL UNIQUE,  -- SHA-256 hex
    key_prefix     VARCHAR(8),                     -- first 8 chars for display
    project_id     BIGINT,
    tags           VARCHAR(1024),                  -- JSON array
    rate_limit_rpm INT DEFAULT 60,
    enabled        BOOLEAN NOT NULL DEFAULT TRUE,
    created_date   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_used_date TIMESTAMP,
    expires_date   TIMESTAMP,
    version        INT NOT NULL DEFAULT 0
);

-- F5: Routing Policies
CREATE TABLE ai_llm_gateway_routing_policy (
    id             BIGINT PRIMARY KEY AUTO_INCREMENT,
    name           VARCHAR(256) NOT NULL UNIQUE,
    strategy       VARCHAR(64) NOT NULL,  -- enum: SIMPLE, WEIGHTED_RANDOM, etc.
    fallback_model VARCHAR(256),
    config         TEXT,                   -- JSON for strategy-specific params
    enabled        BOOLEAN NOT NULL DEFAULT TRUE,
    created_date   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version        INT NOT NULL DEFAULT 0
);

CREATE TABLE ai_llm_gateway_model_deployment (
    id                BIGINT PRIMARY KEY AUTO_INCREMENT,
    routing_policy_id BIGINT NOT NULL REFERENCES ai_llm_gateway_routing_policy(id),
    model_id          BIGINT NOT NULL REFERENCES ai_llm_gateway_model(id),
    weight            INT NOT NULL DEFAULT 1,
    priority_order    INT NOT NULL DEFAULT 0,
    max_rpm           INT,
    max_tpm           INT,
    enabled           BOOLEAN NOT NULL DEFAULT TRUE
);

-- F7: Request Logging
CREATE TABLE ai_llm_gateway_request_log (
    id                 BIGINT PRIMARY KEY AUTO_INCREMENT,
    request_id         VARCHAR(36) NOT NULL,  -- UUID
    api_key_id         BIGINT,
    requested_model    VARCHAR(256),
    routed_model       VARCHAR(256),
    routed_provider    VARCHAR(64),
    routing_policy_id  BIGINT,
    routing_strategy   VARCHAR(64),
    latency_ms         INT,
    input_tokens       INT,
    output_tokens      INT,
    cost               DECIMAL(10,6),
    status             INT,                  -- HTTP status code
    error_message      TEXT,
    cache_hit          BOOLEAN DEFAULT FALSE,
    created_date       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_request_log_created ON ai_llm_gateway_request_log(created_date);
CREATE INDEX idx_request_log_api_key ON ai_llm_gateway_request_log(api_key_id);
CREATE INDEX idx_request_log_model   ON ai_llm_gateway_request_log(routed_model);

-- F8: Spend Summaries (hourly aggregation)
CREATE TABLE ai_llm_gateway_spend_summary (
    id             BIGINT PRIMARY KEY AUTO_INCREMENT,
    api_key_id     BIGINT,
    model          VARCHAR(256),
    provider       VARCHAR(64),
    period_start   TIMESTAMP NOT NULL,
    period_end     TIMESTAMP NOT NULL,
    request_count  INT NOT NULL DEFAULT 0,
    total_input_tokens   INT NOT NULL DEFAULT 0,
    total_output_tokens  INT NOT NULL DEFAULT 0,
    total_cost     DECIMAL(12,6) NOT NULL DEFAULT 0,
    created_date   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

## 5. Key Technical Decisions

### Spring AI Integration
- Use `ChatModel` interface from Spring AI for provider abstraction
- `AiLlmGatewayChatModelFactory` dynamically creates `ChatModel` instances using Spring AI's provider-specific implementations (OpenAiChatModel, AnthropicChatModel, etc.)
- ChatModel instances cached via Spring Cache (`@Cacheable`/`@CacheEvict`) per provider, invalidated on provider config change
- Response caching also uses Spring Cache with configurable TTL
- Streaming uses Spring AI's `StreamingChatModel.stream()` → SSE via Spring WebMvc

### Security
- Public gateway API (`/api/llm-gateway/v1/**`) authenticated via gateway API keys (custom `SecurityConfigurerContributor`)
- Internal management API (GraphQL + REST for UI) authenticated via standard ByteChef session/JWT auth
- API keys stored as SHA-256 hashes only
- Provider API keys encrypted via `EncryptionService`

### Configuration Properties
```yaml
bytechef:
  ai:
    llm-gateway:
      enabled: true
      default-timeout-ms: 30000
      default-max-retries: 2
      cache:
        enabled: true
        default-ttl-seconds: 3600
      cooldown:
        failure-threshold: 3
        duration-seconds: 60
      log-retention-days: 30
      spend-aggregation-cron: "0 0 * * * *"  # hourly
```

### Request Flow
```
Client Request
    → API Key Auth Filter
    → Rate Limit Check
    → Budget Check (hard limit)
    → Cache Check (if enabled)
    → Routing Policy Resolution
    → Router selects deployment
    → Retry/Failover Loop:
        → ChatModel Factory gets/creates ChatModel
        → Execute request via ChatModel
        → On failure: cooldown tracking, try next deployment
    → Format response (OpenAI-compatible)
    → Async: log request, update spend
    → Return response with x-gateway-* headers
```

## 6. Phased Implementation

### Phase 1 — Foundation (F1, F2, F3 partial)
Module structure, provider CRUD, model CRUD, basic chat completions endpoint, models list endpoint. Direct routing only (no policies).

### Phase 2 — Routing & Reliability (F5, F6)
Routing policies, all 5 strategies, retry with backoff, failover, cooldown, timeout.

### Phase 3 — Access & Logging (F4, F7)
API key management, proper auth filter, request logging, log query API.

### Phase 4 — Cost & Caching (F8, F9, F3 embeddings)
Cost calculation, spend aggregation, budget limits, response caching, embeddings endpoint.

### Phase 5 — UI Configuration (F10)
Sidebar entry, all configuration sub-pages (providers, models, API keys, routing, settings).

### Phase 6 — UI Monitoring (F11)
Dashboard with charts, request log viewer, spend visualization.

## 7. Dependencies

### Server Dependencies
- `org.springframework.ai:spring-ai-openai` — OpenAI ChatModel
- `org.springframework.ai:spring-ai-anthropic` — Anthropic ChatModel
- `org.springframework.ai:spring-ai-vertex-ai-gemini` — Google Gemini ChatModel
- `org.springframework.ai:spring-ai-azure-openai` — Azure OpenAI ChatModel
- `org.springframework.ai:spring-ai-mistral-ai` — Mistral ChatModel
- `org.springframework.data:spring-data-jdbc` — Data access
- `org.springframework:spring-webmvc` — REST controllers
- `org.springframework.boot:spring-boot-autoconfigure`
- ByteChef: `encryption-api`, `commons-util`, `tenant-api`, `platform-security-web-api`

### Client Dependencies
- shadcn charts (Recharts-based) for monitoring dashboard visualizations
- Existing shadcn/ui component library already in use across the project

## 8. Deviations from this spec

The following deliberate deviations are tracked and justified by `docs/superpowers/plans/2026-04-12-phase8-gap-remediation.md`:

1. **F4 (Gateway API Key Management)** reuses the platform `ApiKey` entity and `AiGatewayApiKeyAuthenticationProvider` rather than introducing a gateway-scoped `AiGatewayApiKey` entity / `ai_gateway_api_key` table / gateway-specific UI. The existing Settings → API Keys page covers the read-only use case. Revisit only if the gateway requires key-scope filtering that Settings can't express.
2. **Routing strategies** — the implementation ships 7 strategies (Simple, TagBased, PriorityFallback, LatencyOptimized, Intelligent, WeightedRandom, CostOptimized). TagBased is an accepted extension over the design's 5 core strategies; it is documented, wired through `AiGatewayRouter`/`AiGatewayFacade`, and has unit tests (`AiGatewayRoutingStrategyTest`). Removing would break operator workflows.
3. **Budget hard limit** returns HTTP `402 Payment Required` (not `429 Too Many Requests`) so clients can distinguish billing-boundary rejection from rate-limit throttling. Response body is `{ error: { type: "budget_exceeded", message, budgetUsd, spentUsd } }`.
4. **Workspace-level settings** are persisted as a single row in the platform `property` table (scope=`WORKSPACE`, key=`ai_gateway_workspace_settings`) rather than a dedicated `ai_gateway_workspace_settings` table. Property already handles scope, audit, versioning, and encryption.
