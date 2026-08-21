# AI Hub Phase 17 — Usage Tracking (Layer 1)

**Status**: Draft
**Date**: 2026-04-24
**Pattern source**: existing `platform-audit` retention pattern + Spring AI observation API.

## Goal

Capture, per-row, every LLM call and every expensive external-tool call that the AI Hub makes — so future phases (rate limiting, cost caps, observability dashboards) have a real data foundation. Layer 1 is **measurement only**: no enforcement, no UI, no admin actions. Just durable rows you can `SUM()`.

## Why a separate phase

After Phase 12 (audit/undo) and Phase 6.1 (artifacts), it's tempting to fold usage into one of those. Don't:

| Dimension | Audit / artifacts | Usage |
|---|---|---|
| Row volume per turn | 0–3 (mutations only) | 5–50 (every LLM call + every Firecrawl + every image gen) |
| Mutability | Status transitions (APPLIED → REVERSED) | Insert-only |
| Retention | Reversal TTL 30 min; rows live forever for audit | Tiered (raw 90 days → monthly rollups forever) |
| Query pattern | Filter + paginate recent | `SUM(cost), SUM(tokens) GROUP BY day/user/agent` |
| Schema needs | `metadata_json` grab-bag for pre-images | Typed columns (tokens, model, USD) for fast aggregation |
| Audience | Workspace user (sees own mutations) + admin | Admin / finance only |

Same conclusion for `persistent_audit_event` (platform-audit module): wrong shape — that's a security/compliance log keyed by principal+event_type, not an analytical metering store. New tables, same admin-shell pattern as Phase 12.

## Architecture

### Two new tables

```sql
CREATE TABLE ai_hub_usage (
    id              BIGSERIAL PRIMARY KEY,
    workspace_id    BIGINT       NOT NULL,
    user_id         BIGINT       NOT NULL,
    conversation_id BIGINT       NULL REFERENCES ai_hub_task(id) ON DELETE SET NULL,
    agent_name      VARCHAR(64)  NOT NULL,   -- ai_hub_ask | ai_hub_build | research | workflow_builder | data_analyst | image_generator | slide_builder
    parent_agent    VARCHAR(64)  NULL,       -- non-null on subagent rows; references the agent that dispatched it
    model           VARCHAR(128) NOT NULL,   -- e.g. claude-3-5-sonnet-20241022, gpt-4o-mini
    input_tokens    INT          NOT NULL,
    output_tokens   INT          NOT NULL,
    estimated_cost_usd  DECIMAL(12,6)  NOT NULL,
    duration_ms     INT          NOT NULL,
    created_at      TIMESTAMP    NOT NULL
);
CREATE INDEX idx_cc_usage_workspace_created
    ON ai_hub_usage (workspace_id, created_at DESC);
CREATE INDEX idx_cc_usage_user_created
    ON ai_hub_usage (user_id, created_at DESC);
CREATE INDEX idx_cc_usage_agent_created
    ON ai_hub_usage (agent_name, created_at DESC);

CREATE TABLE ai_hub_tool_usage (
    id              BIGSERIAL PRIMARY KEY,
    workspace_id    BIGINT       NOT NULL,
    user_id         BIGINT       NOT NULL,
    conversation_id BIGINT       NULL REFERENCES ai_hub_task(id) ON DELETE SET NULL,
    tool_name       VARCHAR(64)  NOT NULL,   -- firecrawl_search | firecrawl_scrape | openai_image | pptx_generate | etc.
    unit_count      INT          NOT NULL,   -- 1 per call, except Firecrawl batch (could be N URLs)
    estimated_cost_usd  DECIMAL(12,6)  NOT NULL,
    duration_ms     INT          NOT NULL,
    metadata_json   TEXT         NULL,       -- e.g. {urls: [...], imageSize: "1024x1024"}
    created_at      TIMESTAMP    NOT NULL
);
CREATE INDEX idx_cc_tool_usage_workspace_created
    ON ai_hub_tool_usage (workspace_id, created_at DESC);
CREATE INDEX idx_cc_tool_usage_tool_created
    ON ai_hub_tool_usage (tool_name, created_at DESC);
```

`ON DELETE SET NULL` on `conversation_id`: deleting a conversation does not destroy its usage history (cost data outlives the conversation for billing accuracy).

### Cost estimation

`CostEstimator` interface with two methods:

```java
public interface CostEstimator {
    BigDecimal estimateLlmCost(String model, int inputTokens, int outputTokens);
    BigDecimal estimateToolCost(String toolName, int unitCount);
}
```

Default impl `DefaultCostEstimator` reads rates from `application.yml`:

```yaml
bytechef:
  ai-hub:
    cost-estimation:
      llm-rates:
        # USD per 1M tokens; rates as of 2026-04-24, configurable
        claude-3-5-sonnet-20241022: { input: 3.00, output: 15.00 }
        claude-3-5-haiku-20241022:  { input: 0.80, output: 4.00 }
        gpt-4o:                     { input: 2.50, output: 10.00 }
        gpt-4o-mini:                { input: 0.15, output: 0.60 }
      tool-rates:
        # USD per unit; unit is 1 call unless otherwise noted
        firecrawl_search:  0.005
        firecrawl_scrape:  0.005
        openai_image:      0.04   # DALL-E 3 standard 1024x1024
        pptx_generate:     0.0    # local POI; no upstream cost
```

Unknown model / tool → log WARN, charge `0.0` (don't fail the request). The rates table will need periodic updates by hand; a future phase could add an admin-config UI.

### Capture strategy

Two approaches; **start with A**, fall back to B if observation events don't carry enough context.

**Approach A — Spring AI ObservationHandler (preferred)**

Spring AI 1.0+ emits `ChatClientObservation` events with `gen_ai.usage.input_tokens` / `gen_ai.usage.output_tokens` / `gen_ai.request.model`. Register a `UsageObservationHandler implements ObservationHandler<ChatClientObservationContext>` that:

1. Pulls the workspace+user+conversation+agent from a `WorkspaceContextProvider` (already populated by the `WorkspaceInvocationContext` machinery from earlier phases).
2. On observation `onStop`, builds a `AiHubUsage` row and inserts via `UsageRecorder`.

Wired globally in `CopilotConfiguration` so every ChatClient call — parent agent and all subagents — is captured automatically with no per-call instrumentation.

**Approach B — Manual instrumentation (fallback)**

If observation context loses the workspace/user/conversation across the TaskTool-pattern thread boundaries (subagents run on a separate ChatClient inside a synchronous tool call), wrap each subagent's ChatClient with a `MeteringChatClient` decorator that captures input + output tokens around `.call()` and writes the row directly. More verbose; survives any observation context loss.

The first commit prototypes A; the second commit decides B if A turns out unreliable for subagents.

### Tool-call capture

The expensive non-LLM tools each get an explicit recording call inside their `ToolCallback.call(...)`:

| Tool callback | Records |
|---|---|
| `FirecrawlSearchToolCallback` (in research subagent) | tool_name=`firecrawl_search`, unit_count=1 |
| `FirecrawlScrapeToolCallback` | tool_name=`firecrawl_scrape`, unit_count=N URLs |
| `OpenAiImageToolCallback` (image_generator subagent) | tool_name=`openai_image`, unit_count=1 |
| `PptxGenerateToolCallback` (slide_builder subagent) | tool_name=`pptx_generate`, unit_count=1 |

Each of these subagent-internal callbacks already has access to the `WorkspaceInvocationContext` (passed by the wrapping ToolCallback). Inject `UsageRecorder` and call it at the bottom of the success path.

Cheap tools (workspace listing/reading/etc.) are NOT recorded — too noisy and free-of-cost.

### Service layer

`ai-copilot-api`:

```java
public interface UsageRecorder {
    void recordLlm(long workspaceId, long userId, Long conversationId,
                   String agentName, String parentAgent,
                   String model, int inputTokens, int outputTokens, long durationMs);

    void recordTool(long workspaceId, long userId, Long conversationId,
                    String toolName, int unitCount, long durationMs, Map<String, Object> metadata);
}

public interface UsageService {
    UsageSummary summarize(long workspaceId, Instant from, Instant to);
    List<DailyUsage> dailyByWorkspace(long workspaceId, int days);
    List<TopConsumer> topUsersByCost(long workspaceId, Instant from, Instant to, int limit);
    // ... aggregation helpers — read-side, used by future Layers 2/3 enforcement
}
```

`ai-copilot-service`: `UsageRecorderImpl` writes via repositories; `UsageServiceImpl` runs the read aggregations.

### REST (read-only, admin-only — minimal)

Layer 1 ships **no UI**. We don't even need REST endpoints in Layer 1 strictly — but a single read endpoint is useful for a quick admin sanity check and as a stub for Layer 4's dashboard:

- `GET /api/platform/internal/ai-hub/usage/summary?workspaceId=N&from=...&to=...`

Returns `{totalCostUsd, totalInputTokens, totalOutputTokens, llmCallCount, toolCallCount}`. Admin-gated.

If even that feels like scope creep for Layer 1, defer it — Layer 4 (admin UI) lands the full read surface.

## Non-goals (Layer 1 / Phase 17)

- **No enforcement** — rate limits + cost caps are Layers 2 + 3 (separate phases).
- **No admin UI** — Layer 4. Phase 17 ships data only.
- **No retention / rollup job** — raw rows live forever in Phase 17. A future commit adds tiered retention (90-day raw, monthly rollups) once volume justifies it.
- **No per-user surfacing** — usage is back-office in Layer 1.
- **No cross-workspace aggregation** — every query is workspace-scoped.
- **No accounting-grade fidelity** — rates drift; rounding loses precision; Approach A may miss cancelled/errored requests. Good enough for trend analysis and pre-cap warnings, not for invoicing.

## Known gaps carried forward

- **Rate freshness**: model rates are hand-maintained config; will drift. Layer 4 admin UI eventually edits them; until then, expect quarterly manual updates.
- **Subagent attribution under Approach A**: if observation context loses parent-agent identity across the TaskTool boundary, the `parent_agent` column may be null on subagent rows. Acceptable for v1.
- **Errored requests**: if the LLM errors mid-stream, Spring AI may emit no usage observation. Token cost is capped at our request size, so this under-counts but never over-counts. Acceptable.
- **Local pptx**: `pptx_generate` cost is 0.0 (just CPU/disk). Recording it lets us track subagent reach even though it's free.

## Testing

### Server
- `AiHubUsageRepositoryIntTest` (Testcontainers) — insert/query/aggregation smoke.
- `AiHubToolUsageRepositoryIntTest` — same.
- `DefaultCostEstimatorTest` — known model/tool rates → expected USD; unknown → 0.0 + WARN log.
- `UsageObservationHandlerTest` — synthesize observation events, assert recorder called with right args.
- `UsageRecorderImplTest` — happy path + null conversation_id.
- `UsageServiceImplTest` — summarize / daily / topUsers aggregations.
- Each affected expensive tool callback test (`FirecrawlSearch...`, `OpenAiImage...`, `PptxGenerate...`) asserts the recorder is invoked once per success.

### Client
- None in Phase 17 (no UI).

## Task sequence (~6–8 commits)

1. `CC17 Add ai_hub_usage + ai_hub_tool_usage Liquibase tables`.
2. `CC17 Add AiHubUsage / AiHubToolUsage domain + repositories`.
3. `CC17 Add CostEstimator + DefaultCostEstimator with config-driven rates`.
4. `CC17 Add UsageRecorder / UsageService (interface + impl)`.
5. `CC17 Add UsageObservationHandler and wire globally for LLM calls (Approach A)`.
6. `CC17 Instrument Firecrawl + OpenAI image + pptx tool callbacks with UsageRecorder`.
7. `CC17 Add admin GET /usage/summary endpoint` (optional — defer to Layer 4 if scope tight).
8. `CC17 Final formatting + lint fixes` (if needed).

If Approach A turns out unreliable in step 5, split commit 5 into "5a — observation handler" (kept) + "5b — MeteringChatClient decorator (Approach B fallback) for subagents".

## Commit convention

`CC17 …` (server-only phase; no `client - ` prefix needed since there's no client work).

## What lands after Phase 17

A populated `ai_hub_usage` table. Run a query, see real numbers. That's the deliverable.

Layers 2 (rate limiting), 3 (cost caps), 4 (admin dashboard) become straightforward follow-up phases on top of this data foundation — they were impossible before.
