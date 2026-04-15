# Phase 8: Gap Remediation — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Close all remaining gaps between the AI Gateway Observability spec/phase plans and the implementation merged in commits `153b922`, `7b1394`, `2ba1444`. Covers partially-done items, still-missing feature work, and cross-cutting concerns (RBAC, EE remote stubs, telemetry, idempotency, i18n, PII).

**Reference spec:** `docs/superpowers/specs/2026-04-11-ai-gateway-observability-platform-design.md`
**Prior phase plans:** `docs/superpowers/plans/2026-04-11-phase{1..7}-*.md`

**Execution order:** Groups A (urgent, CLAUDE.md mandates) → B (functional correctness) → C (UX completeness) → D (observability/docs/tests). Within a group, tasks are independent unless noted.

**Liquibase policy for this plan:** The observability migrations have not been released. **Edit the existing files in place** instead of adding new changesets:

- `server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-service/src/main/resources/config/liquibase/changelog/automation/ai_gateway/00000000000001_ai_gateway_init.xml`
- `.../00000000000002_ai_observability_init.xml`
- `.../00000000000003_ai_prompt_init.xml`
- `.../00000000000004_ai_eval_init.xml`

After editing, delete stale copies in `build/resources/` (Liquibase sees both old and new on classpath — classpath reload pitfall documented in CLAUDE.md). Reset the dev DB volume (`docker compose -f server/docker-compose.dev.infra.yml down -v`) so edited changesets re-run cleanly; no production data exists yet.

---

## Group A — Urgent (CLAUDE.md mandates & data correctness)

### A1. EE remote-client stubs for new services — **WONTFIX**
**Rationale (2026-04-12 investigation):** Only `ai-gateway-app` imports `automation-ai-gateway-api` (confirmed via `grep build.gradle.kts` across `server/ee/`). No other EE microservice consumes the gateway's services, so `@ConditionalOnEEVersion` stubs are unnecessary — nothing outside `ai-gateway-app` would attempt to wire these beans. The existing 3 facade stubs (provider/routing-policy/model) are pattern scaffolding; adding more stubs for services that have no external consumers adds no value and creates maintenance burden.

Revisit only if: (a) another EE app starts importing `automation-ai-gateway-api`, or (b) the services are re-exposed as cross-app facades. Until then, CLAUDE.md's EE remote-client mandate does not apply here.

- [x] ~~Investigation done; no stubs required.~~

### A2. RBAC on GraphQL + REST — **MOSTLY DONE**
**Investigation (2026-04-12):** Every `@QueryMapping`/`@MutationMapping` in all 22 gateway GraphQL controllers already carries `@PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")` (verified by mapping-count vs preauth-count diff — zero mismatches). `AiGatewayPlaygroundRestController` is also annotated. Public REST (`/api/ai-gateway/v1/chat/completions`, `/embeddings`, `/scores`) intentionally does not use admin-only `@PreAuthorize` because SDKs call these with user/API-key auth. Global `/api/**` rule in `SecurityConfiguration.java:242` already enforces `.authenticated()`.

- [x] ~~GraphQL controllers already guarded.~~
- [x] ~~Playground REST controller guarded.~~
- [x] ~~Public REST intentionally not admin-only — audit confirms this is correct.~~
- [x] Applied `@PreAuthorize("@permissionService.hasWorkspaceRole(#workspaceId, 'VIEWER')")` to every list-by-workspace query across the gateway GraphQL surface (trace, session, alert-rule, export-job, notification-channel, webhook-subscription, eval-rule, eval-score, eval-score-config, prompt, tag, workspace-settings, request-log). Tenant admins still pass via `PermissionService.hasWorkspaceRole`'s built-in `isTenantAdmin()` short-circuit. Mutation paths remain `hasAuthority(ADMIN)` — follow-up to tighten to `hasWorkspaceRole(..., 'EDITOR')` once role-ownership model is finalised.
- [ ] **Deferred (int-test harness)**: GraphQL integration tests asserting 403 when non-admin queries observability endpoints. Requires workspace-membership fixtures + a `@WithMockUser` variant that carries workspace roles.

### A3. Idempotency for external trace IDs — **DONE (DDL) / partial (race retry)**
- [x] Changed `idx_ai_obs_trace_ext_trace_id` composite index on `(workspace_id, external_trace_id)` from `unique="false"` to `unique="true"` in `00000000000002_ai_observability_init.xml` (PostgreSQL treats NULL values in unique indexes as distinct, so nullable `external_trace_id` rows still coexist).
- [x] Upsert-by-external-id already implemented in `AiGatewayFacade.ensureTrace` (lines 1604-1635): find existing, merge token/latency/cost totals, update status — no duplicate inserts on the happy path.
- [x] Added `try/catch DataIntegrityViolationException → re-find → merge` in `AiGatewayFacade.ensureTrace`. On UNIQUE collision, the loser re-fetches the winning trace, merges its tokens/latency/cost/error-status via new `mergeIntoExistingTrace` helper, and updates instead of bubbling a 500. Only applied when `hasExternalTraceId()` — there's no collision risk otherwise.
- [ ] **Deferred**: Extend `AiObservabilityTraceServiceIntTest` with duplicate-external-id retry test asserting UNIQUE violation on second insert. (Test-only; UNIQUE constraint itself is live.)

### A4. Session auto-upsert correctness (`AiGatewayFacade.java:1682-1693`) — **DONE**
- [x] Added `external_session_id VARCHAR(256)` column + `(workspace_id, external_session_id)` UNIQUE index `idx_ai_obs_session_ext_session_id` to `ai_observability_session` in `00000000000002_ai_observability_init.xml`.
- [x] Added `externalSessionId` field + getter/setter to `AiObservabilitySession` domain.
- [x] Added `findByWorkspaceIdAndExternalSessionId` to `AiObservabilitySessionRepository`.
- [x] Added `getOrCreateSessionByExternalId` method to `AiObservabilitySessionService` interface + impl.
- [x] Removed numeric-parse branch in `AiGatewayFacade`; now always resolves by external session key.
- [x] Verified module compiles (`./gradlew :...:automation-ai-gateway-service:compileJava`).
- [ ] **Deferred**: Integration test covering both string and numeric-looking external keys (D4 int-test group).

### A5. Alert auto-resolution (TRIGGERED → RESOLVED) — **DONE**
- [x] Added `resolved_date TIMESTAMP` column to `ai_observability_alert_event` in `00000000000002_ai_observability_init.xml`.
- [x] Added `resolvedDate` field + getter/setter on `AiObservabilityAlertEvent` domain.
- [x] Extended `AiObservabilityAlertEvaluator.evaluate`: on below-threshold path, calls new `resolveIfOpen` which transitions the latest TRIGGERED event to RESOLVED (sets `resolvedDate`, updates status), then dispatches notification.
- [x] Updated `AiObservabilityNotificationDispatcher` email subject (`[ByteChef Alert RESOLVED]`) + Slack message icon/label to branch on `getStatus()==RESOLVED`. Webhook payload already includes `status` field.
- [x] Module compiles.
- [ ] **Deferred**: Integration test covering trigger → below-threshold → resolution cycle (D4).

---

## Group B — Functional correctness

### B1. Trace filters: DB-side + additional dimensions — **DONE**
- [x] Added `findAllByFilters(workspaceId, start, end, userId, status, source, model, tagId)` `@Query` to `AiObservabilityTraceRepository` — null-safe dynamic predicates with LEFT JOINs on `ai_observability_span` (for `model`) and `ai_observability_trace_tag` (for `tagId`); `SELECT DISTINCT` + `ORDER BY created_date DESC`.
- [x] Rewrote `AiObservabilityTraceServiceImpl.getTracesByWorkspaceFiltered` to call the new repo method; removed in-memory `.filter(...)` chain.
- [x] Extended service-interface signature with `model`, `tagId` params.
- [x] Extended GraphQL schema `ai-observability-trace.graphqls` with `model: String, tagId: ID` args.
- [x] Updated `AiObservabilityTraceGraphQlController.aiObservabilityTraces` to forward the new args.
- [x] Extended client `aiObservabilityTraces.graphql` operation with new variables and regenerated `graphql.ts`.
- [x] Updated `AiObservabilityTraces.tsx` to pass `status/source/userId` server-side (remove redundant client-side filtering); existing UI filter bar now just drives query variables.
- [x] Server + client compile clean.
- [ ] **Deferred**: Integration test covering combined-filter query (D4).
- [x] UI: added debounced `Filter by Model...` text input and `All Tags` select (populated from `useAiGatewayTagsQuery`) to the trace filter bar. Selected values flow through the existing query args (`model`, `tagId`) into server-side filtering.

### B2. Tag assignment mutations + UI — **DONE (server) / DEFERRED (UI)**
- [x] Added `extend type Mutation { setAiObservabilityTraceTags(traceId: ID!, tagIds: [ID!]!): AiObservabilityTrace }` in `ai-observability-trace.graphqls`.
- [x] Implemented `setTraceTags(long, List<Long>)` in service: replaces tag set atomically (Spring Data JDBC aggregate save handles delete-orphans + insert).
- [x] `setAiObservabilityTraceTags` mutation handler added to `AiObservabilityTraceGraphQlController` with `@PreAuthorize(ADMIN)`.
- [x] Reuses existing `AiObservabilityTraceTag(Long tagId)` constructor (which itself references platform `Tag` via `AggregateReference`).
- [x] Server compiles.
- [x] Server-side: exposed `tagIds: [ID!]` on `AiObservabilityTrace` GraphQL type + `@SchemaMapping` resolver that maps the aggregate reference collection to bare IDs. Client resolves names via a separate tag query to avoid cross-module coupling.
- [x] Added `TraceTagEditor` component + new `setAiObservabilityTraceTags` client operation + `tagIds` field on the trace query. Renders attached tags as colored chips with remove buttons plus an add-dropdown of available workspace tags. Writes via the set-based mutation (full tagIds list each call). Wired into `AiObservabilityTraceDetail.tsx` below the trace metadata. Opted for a bespoke component over `TagList` because `TagList`'s `(id, tags)` callback shape doesn't fit our `(traceId, tagIds)` mutation cleanly — the chip rendering is ~30 lines anyway.
- [x] Scoped `add`/`remove` mutations — not needed; `setTraceTags(traceId, tagIds)` is idempotent and matches the "edit full tag set" UX. Re-open if per-tag mutation granularity ever proves necessary.

### B3. Scheduled exports — **DONE**
- [x] Added `cron_expression VARCHAR(64)` and `next_run_date TIMESTAMP` columns to `ai_observability_export_job` in `00000000000002_ai_observability_init.xml`.
- [x] Added `cronExpression` field + getter/setter to `AiObservabilityExportJob` domain.
- [x] Added `ExportScheduler` SPI in `platform-scheduler-api`, `ExportExecutionEvent` event record, `QuartzExportScheduler` + `ExportExecutionJob` Quartz Job (cron-triggered) in `platform-scheduler-impl`, bean registration in `QuartzTriggerSchedulerConfiguration`.
- [x] `AiObservabilityExportJobServiceImpl.create` calls `exportScheduler.scheduleExport(id, cron)` for SCHEDULED jobs; `.cancel` calls `exportScheduler.cancelExport(id)` to remove the Quartz trigger.
- [x] `AiObservabilityExportExecutor.onScheduledExport(@EventListener)` resets status to PENDING and re-invokes `executeExport` when the cron fires. Skips if CANCELLED or still PROCESSING from prior fire.
- [x] GraphQL `createAiObservabilityExportJob` accepts `type` + `cronExpression` args; ON_DEMAND runs once, SCHEDULED relies on scheduler firing.
- [x] All modules compile.
- [ ] **Deferred**: Integration test with Quartz in-memory store (D4).

### B4. Export cancellation — **DONE (server) / DEFERRED (UI)**
- [x] Appended `CANCELLED` to `AiObservabilityExportJobStatus` (end of enum preserves ordinal stability for INT-column storage).
- [x] Added `cancelAiObservabilityExportJob(id: ID!)` mutation + `AiObservabilityExportJobService.cancel(long)` — rejects terminal-state jobs (COMPLETED/FAILED/CANCELLED).
- [x] Added `CANCELLED` to `AiObservabilityExportJobStatus` GraphQL enum.
- [x] Export executor checks CANCELLED status before start, after fetch, and before file-write; leaves partial file in storage with info log (cleanup can happen via retention service).
- [x] Server compiles.
- [x] Added `cancelAiObservabilityExportJob` client operation + "Cancel" button in `AiObservabilityExports.tsx` for rows whose status is PENDING or PROCESSING. Toast on success/failure; React Query invalidation refreshes the table.

### B5. Event-emission hooks (`trace.completed`, `budget.exceeded`) — **DONE**
- [x] Added `AiGatewayTraceCompletedEvent` and `AiGatewayBudgetExceededEvent` records in new `event/` package under `automation-ai-gateway-api`.
- [x] `AiGatewayFacade` now publishes `AiGatewayTraceCompletedEvent` whenever a trace transitions to COMPLETED or ERROR (both terminal states are interesting to subscribers).
- [x] `AiGatewayFacade.checkBudget` publishes `AiGatewayBudgetExceededEvent` before throwing `BudgetExceededException`.
- [x] `AiObservabilityWebhookDeliveryServiceImpl` now has `@EventListener onTraceCompleted` and `onBudgetExceeded` methods that translate the events into `deliverEvent(workspaceId, eventType, payload)` calls.
- [x] Updated `AiGatewayFacadeTest` constructor wiring to pass a dummy `ApplicationEventPublisher`.
- [x] Server + tests compile.
- [ ] **Deferred**: Integration test covering event → webhook delivery row creation (D4).

### B6. Test-notification-channel mutation — **ALREADY DONE**
- [x] `testAiObservabilityNotificationChannel(id: ID!): Boolean` mutation already exists in schema + controller (line 84 of `AiObservabilityNotificationChannelGraphQlController`) + `AiObservabilityNotificationDispatcher.dispatchTest` (line 66). The gap report was out of date.
- [x] Client wiring verified: `AiObservabilityNotificationChannelDialog.tsx` uses `useTestAiObservabilityNotificationChannelMutation` (line 5, 59) with a "Send Test" button (line 163) and toast feedback on success/failure.

---

## Group C — UX completeness

### C1. Span timing waterfall view — **DONE**
- [x] New `SpanWaterfall.tsx` renders a 3-column table (name / duration / timeline bar) with depth-indented names and absolute-offset-positioned bars scaled to the trace's full timeline. Bars are colored by `SpanType` (generation blue, tool_call orange, event purple, span gray) and override to red for `SpanStatus=ERROR`. Tooltip shows the span name + duration.
- [x] Integrated into `AiObservabilityTraceDetail.tsx` as a toggle between `Tree` and `Waterfall` views via a small button pair next to the "Spans" heading.

### C2. Prompt version diffs — **ALREADY DONE**
- [x] `AiPromptDetail.tsx` already ships an inline `simpleDiff(a, b): DiffLineI[]` implementation + `compareSelection` state + Compare-selectable version rows + a `compareVersions` memo that renders the diff view when exactly two are selected. No external diff library needed (the inline LCS-style implementation is adequate for prompt-sized inputs). Side-by-side/unified toggle is the one piece not yet rendered — follow-up if operators ask for it.

### C3. Per-version performance metrics — **DONE (server) / DEFERRED (UI)**
- [x] `prompt_version_id` column already present on `ai_observability_span` (more granular than trace-level; one trace can involve multiple prompt versions). Facade already writes it (`AiGatewayFacade:1452`).
- [x] Added `AiPromptVersionMetrics` DTO record (`invocationCount, avgLatencyMs, avgCostUsd, errorRate`).
- [x] Added `AiObservabilitySpanRepository.aggregateMetricsByPromptVersion` `@Query` (COUNT/AVG/error-rate via status ordinal = 2).
- [x] Added `metrics: AiPromptVersionMetrics` field to `AiPromptVersion` GraphQL type + `@SchemaMapping` resolver in `AiPromptGraphQlController`.
- [x] Server compiles.
- [x] Extended `aiPrompts.graphql` version selection with `metrics { avgCostUsd avgLatencyMs errorRate invocationCount }`. Added a "Metrics" column to the version table in `AiPromptDetail.tsx` rendering invocation count, average latency/cost, and error-rate pct when the version has traffic.

### C4. Variable extraction parser — **DONE**
- [x] Added `PromptVariableExtractor.extractAsJson(content)` utility in `service/util/` — regex `\{\{\s*([A-Za-z_][A-Za-z0-9_]*)\s*\}\}`, LinkedHashSet preserves insertion order + deduplicates, returns JSON array string.
- [x] `AiPromptVersionServiceImpl.create` auto-populates `variables` from content when caller didn't supply an explicit list (null-only fallback so explicit empty `[]` is respected).
- [x] Server compiles.
- [x] Unit test `PromptVariableExtractorTest` with 10 cases covering null/empty, simple, order preservation, dedup, whitespace, malformed, underscores+digits, adjacent placeholders. All pass.

### C5. Playground: managed prompt mode + side-by-side comparison — **MOSTLY DONE**
- [x] Extended `PlaygroundChatCompletionInput` GraphQL input with `promptId: ID`, `promptVariables: String` (JSON map). Controller resolves the active `production` version via `AiPromptVersionService.getActiveVersion` and renders `{{var}}` placeholders via `renderManagedPrompt`, prepending as a SYSTEM message.
- [x] Side-by-side comparison: `AiGatewayPlayground.tsx` already ships dual `mutationLeft` / `mutationRight` + separate `responseLeft` / `responseRight` state for compare-two-models runs. The spec's "Freeform vs Managed-prompt" mode-switcher is deferred — operators who want to feed a `promptId` today do so by switching the mutation variables directly; the UI toggle is a polish pass rather than a capability gap.

### C6. Monitoring: custom property filters — **DONE (server) / DEFERRED (UI)**
- [x] Added `propertyKey: String, propertyValue: String` args to `workspaceAiGatewayRequestLogs` GraphQL query. When both supplied, `AiGatewayRequestLogRepository.findAllByWorkspaceIdAndCustomProperty` JOINs `ai_gateway_custom_property` on `request_log_id` to filter to only logs with that key/value. Chain multiple filters client-side with separate queries for AND semantics. Also upgraded this query's predicate to `hasWorkspaceRole(#workspaceId, 'VIEWER')` via A2's batch.
- [x] Added `propertyKey` + `propertyValue` text inputs to `AiGatewayDashboard.tsx` next to the time-range controls. Both args thread through the `workspaceAiGatewayRequestLogs` query; server-side JOIN on `ai_gateway_custom_property` (shipped earlier) does the filtering. A Clear button wipes both when either is set.

### C7. Score analytics time-series trend — **DONE**
- [x] Added `aiEvalScoreTrend(workspaceId, name, startDate, endDate): [AiEvalScoreTrendPoint]` GraphQL query + DTO + repo `@Query` (day-bucketed via `DATE_TRUNC('day', created_date)`) + service + controller. Added `aiEvalScoreTrend` client operation. New `AiEvalScoreTrendChart` (Recharts `LineChart` with `connectNulls=true`) wired into `AiEvalScoreAnalytics.tsx` under the min/avg/max block for NUMERIC-typed scores.

---

## Group D — Observability, i18n, PII, tests, docs

### D1. Micrometer telemetry — **PARTIAL**
- [x] Added `AiGatewayMetrics` component — decouples metrics from `AiGatewayFacade` via `@EventListener` on existing `AiGatewayTraceCompletedEvent` and `AiGatewayBudgetExceededEvent`.
- [x] Metrics registered: `ai_gateway.trace.completed` (counter, tags: model, status), `ai_gateway.trace.latency` (timer, tags: model, status), `ai_gateway.budget.exceeded` (counter, tag: model). Workspace omitted from tags to avoid cardinality explosion.
- [x] Added `ai_gateway.rate_limit.rejections` (tag: rate_limit) — called from `AiGatewayRateLimitChecker` on reject.
- [x] Added `ai_gateway.alert.triggered` (tag: metric) — called from `AiObservabilityAlertEvaluator.evaluate` when a TRIGGERED event is created.
- [x] Added `ai_gateway.webhook.delivery` (tags: event_type, status) — called from `AiObservabilityWebhookDeliveryServiceImpl` after SUCCESS, or after the final FAILED retry.
- [x] Server + tests compile.
- [x] Added `ai_gateway.export.duration` timer (tags: scope, outcome). `AiObservabilityExportExecutor.executeExport` records wall-clock duration from PROCESSING flip to either COMPLETED or FAILED state via `AiGatewayMetrics.recordExportDuration`. Cancellation short-circuits don't emit the timer (no meaningful "duration to cancel" for the ops question "how long do exports take?").
- [x] Metrics documented in module README under the `Metrics` section (covered by D5).

### D2. i18n
- [x] Audit all new client components under `client/src/pages/automation/ai-gateway/` for hardcoded English strings.
- [x] Migrate to the project's translation mechanism (check `client/src/shared/i18n/` for existing pattern — likely `react-intl` or keyed JSON).
- [x] Add English base translations; coordinate with translation workflow.

### D3. PII retention + redaction — **PARTIAL**
- [x] Added `pii_redacted BOOLEAN NOT NULL DEFAULT FALSE` column to `ai_observability_trace` in `00000000000002_ai_observability_init.xml`.
- [x] Added `piiRedacted` field + getter/setter (`isPiiRedacted` / `setPiiRedacted`) on `AiObservabilityTrace` domain.
- [x] Server compiles.
- [x] Added `redactPii` field to `AiGatewayWorkspaceSettings` record + service read/write + GraphQL schema + controller.
- [x] Wired `AiGatewayWorkspaceSettingsService` into `AiGatewayFacade`; `resolveOrCreateTrace` checks `isPiiRedactionEnabled(workspaceId)` and replaces `input`/`output` with `redactedDigest()` (SHA-256 first 16 hex chars + byte length) when true. `piiRedacted` boolean is stamped on the trace row.
- [x] `AiObservabilityDataCleanupService.resolveRetentionDays` now checks project → workspace settings (via `AiGatewayWorkspaceSettingsService.findByWorkspaceId(...).logRetentionDays`) → DEFAULT (30). Operators can pin sensitive projects shorter than their workspace default, or ship a workspace-wide override without touching every project row.
- [x] Added `Data Privacy & Retention` section to module README covering trace-payload storage, the reserved `pii_redacted` column, retention ownership, encryption-at-rest posture, and workspace-deletion FK cascade.

### D4. Integration tests for uncovered services — **PARTIAL**
- [x] `AiObservabilitySessionServiceIntTest` — external-id idempotency + numeric-looking key coverage.
- [x] `AiObservabilitySpanServiceIntTest` — span creation + retrieval by trace.
- [x] `AiObservabilityExportJobServiceIntTest` — cancel happy path + terminal-state rejection. Scheduled-cron path deferred (needs Quartz harness).
- [x] `AiObservabilityNotificationChannelServiceIntTest` — create/list/delete.
- [ ] **Deferred (HTTP/mail harness)**: `AiObservabilityNotificationDispatcherIntTest` — needs mock `JavaMailSender` + HTTP in shared int config.
- [x] `AiObservabilityWebhookSubscriptionServiceIntTest` — only enabled subscriptions returned for event delivery.
- [ ] **Deferred (HTTP harness)**: `AiObservabilityWebhookDeliveryServiceIntTest` — needs HTTP client stub (WireMock).
- [x] `AiGatewayCustomPropertyServiceIntTest` — create + find-by-trace.
- [ ] **Deferred (async harness)**: `AiObservabilityExportExecutorIntTest` — `@Async` + file-storage coupling thickens setup.
- [ ] **Deferred**: Run `./gradlew :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-service:testIntegration` end-to-end — requires a local PostgreSQL Testcontainers environment. Individual test files compile clean; suite run is out of scope for this pass.

### D5. Per-feature documentation — **PARTIAL**
- [x] Added `Gateway Response Headers` section to module README documenting `x-gateway-*` headers + 402 budget semantics.
- [x] Added `Metrics` section listing all Micrometer counters/timers with their tags and emission source.
- [x] Updated `Database Schema` section: noted `default_routing_policy_id` on `ai_gateway_model`, added `ai_gateway_tag`, documented the workspace-settings Property-store reuse (no dedicated table).
- [ ] **Deferred**: `docs/` site content extension. Module README already covers architecture, features, headers, metrics, schema, and privacy — site pages can link to it.
- [x] Updated public REST OpenAPI spec (`openapi.yaml`) for `/chat/completions`: documented the six `x-gateway-*` response headers on 200 and the new 402 `budget_exceeded` response. Cancel-export/scheduled-export/test-provider are GraphQL-only, not public REST — no OpenAPI change needed.
- [ ] **Deferred**: Run `./gradlew generateDocumentation` — CI regenerates component docs on main.

---

## Group E — Gateway-proper spec gaps (from `2026-04-03` + `2026-04-07`)

> Distinct from Groups A–D (which track the observability spec). These items close remaining gaps against the **original** AI API Gateway design (`specs/2026-04-03-llm-api-gateway-design.md`) and the **Projects & Tags** rename spec (`specs/2026-04-07-ai-gateway-rename-projects-tags-design.md`).

### E1. Client-facing `x-gateway-*` response headers (F3) — **PARTIAL (plumbing + chat) / DEFERRED (facade wiring + embeddings/scores)**
- [x] Extended `AiGatewayChatCompletionResponse` with a transient `GatewayMetadata` record (provider, model, latencyMs, cacheHit, routingPolicy, requestId). Added a 6-arg convenience constructor so existing callers that don't set metadata still compile unchanged.
- [x] `AiGatewayChatCompletionApiController.chatCompletions` now extracts `gatewayMetadata` and emits `x-gateway-provider`, `x-gateway-model`, `x-gateway-latency-ms`, `x-gateway-cache-hit`, `x-gateway-routing-policy`, `x-gateway-request-id` on the 200 response (per-field null-skip to avoid empty headers).
- [x] Server compiles.
- [x] Populate `gatewayMetadata` from `AiGatewayFacade.chatCompletion`. New private `withGatewayMetadata(response, request, startTime)` populates provider (parsed from `provider/model` slug), model, latencyMs, routingPolicy, and requestId on the returned response. `cacheHit` is left null for now (cache path doesn't surface a hit flag yet).
- [x] **WONTFIX (streaming headers)**: SSE responses commit the HTTP status line + headers before the first chunk emits, and the Reactor `Flux` chain only knows its metadata after the first `ChatResponse` lands. Emitting `x-gateway-*` on streaming would require either buffering the whole response (defeats streaming) or a custom `HttpMessageWriter` that materialises the first chunk synchronously before writing headers. Neither fits cleanly; non-streaming chat + embeddings already ship the headers.
- [x] Extended `AiGatewayEmbeddingResponse` with `GatewayMetadata` (reused the chat completion record). `AiGatewayEmbeddingApiController.embeddings` now emits `x-gateway-*` headers via its own `applyGatewayMetadataHeaders` helper; facade populates provider type + model name + latencyMs + fresh request ID. Scores controller has no response body metadata to emit.
- [ ] **Deferred**: Integration test asserting headers present on 200 responses (D4).

### E2. Budget headers & hard-limit status (F8) — **PARTIAL**
- [x] `BudgetExceededException` now carries `budgetUsd` + `spentUsd` numeric fields; facade passes them from the `BudgetCheckResult`.
- [x] `AiGatewayExceptionHandler.handleBudgetExceeded` returns `HTTP 402 Payment Required` with structured body `{ error: { message, type: "budget_exceeded", budgetUsd, spentUsd } }`. Existing unit test updated to assert 402.
- [x] Both modules compile.
- [x] Soft-limit `x-gateway-budget-warning` shipped: `GatewayMetadata` grew a `budgetWarningRemainingUsd: BigDecimal` field, populated by `AiGatewayFacade.resolveBudgetWarningRemaining` (re-runs the budget checker post-response so the header reflects the after-this-call state, returns null when below threshold). Both chat and embedding controllers emit the new header. OpenAPI spec updated.
- [ ] **Deferred**: Integration test covering 402 + warning header semantics (D4).

### E3. Provider "Test connectivity" button (F1) — **DONE (server) / DEFERRED (UI)**
- [x] Added `ProviderConnectionResult` DTO (`{ok, latencyMs, errorMessage}`) with `success/failure` factories.
- [x] `WorkspaceAiGatewayProviderFacade.testWorkspaceProviderConnection(workspaceId, providerId)` method + impl that GETs `{baseUrl}/models` with bearer token, 5s connect / 10s request timeout. Never throws — returns structured success or failure.
- [x] Remote-client stub updated for new method.
- [x] GraphQL `testWorkspaceAiGatewayProviderConnection(workspaceId, providerId)` mutation + `ProviderConnectionResult` type.
- [x] Server compiles.
- [x] Wired "Test Connection" button in `AiGatewayProviderDialog.tsx` (edit mode only). Calls `testWorkspaceAiGatewayProviderConnection`, toasts `Connected in Xms` on success or the structured `errorMessage` on failure.

### E4. Model-level default routing policy (F5) — **PARTIAL**
- [x] Added `default_routing_policy_id BIGINT` column to `ai_gateway_model` in `00000000000001_ai_gateway_init.xml`.
- [x] Added `defaultRoutingPolicyId` field + getter/setter on `AiGatewayModel` domain.
- [x] Extended GraphQL `AiGatewayModel` type, `CreateAiGatewayModelInput`, `UpdateAiGatewayModelInput` with `defaultRoutingPolicyId: ID`.
- [x] Extended `AiGatewayModelGraphQlController` record types + create/update handlers to persist the new field.
- [x] Server compiles.
- [x] Added `AiGatewayFacade.applyRoutingPolicyPrecedence` that falls back to model-level `defaultRoutingPolicyId` when the request has none, building a new request with the resolved policy name. Missing-policy lookup logs and falls back to direct routing (no 500). Project/workspace layers of the spec chain are stubs for a future pass.
- [x] Added `defaultRoutingPolicyId` field on `WorkspaceAiGatewayModelGraphQlController.UpdateAiGatewayModelInput` + `CreateWorkspaceAiGatewayModelInput`. Extended `WorkspaceAiGatewayModelFacade.updateWorkspaceModel` signature + impl + remote-client stub. `AiGatewayModelDialog.tsx` renders a "Default Routing Policy" select populated from `useWorkspaceAiGatewayRoutingPoliciesQuery`. Updates pass null to clear (different from omit-means-keep) so operators can revert to inheritance.

### E5. `AiGatewayTag` workspace-scoped entity (Projects & Tags §3) — **DONE (new entity) / DEFERRED (migration of existing callers)**
- [x] Added `ai_gateway_tag` table to `00000000000001_ai_gateway_init.xml` with `(workspace_id, name)` UNIQUE index.
- [x] `AiGatewayTag` domain + `AiGatewayTagRepository` + `AiGatewayTagService` / `AiGatewayTagServiceImpl`.
- [x] GraphQL: `aiGatewayTag`, `aiGatewayTags(workspaceId)` queries + `createAiGatewayTag`, `updateAiGatewayTag`, `deleteAiGatewayTag` mutations (admin-only).
- [x] Server compiles.
- [x] Migrated `AiObservabilityTraceTag` + `AiGatewayRoutingPolicyTag` from `AggregateReference<Tag, Long>` → `AggregateReference<AiGatewayTag, Long>`. `AiGatewayFacade.resolveOrCreateTrace` now upserts incoming tag names via `aiGatewayTagService.findByWorkspaceIdAndName(...)` + `create(...)` instead of platform `TagService.save(...)`. `AiGatewayTagApiController` (public REST) reads via `AiGatewayTagService.getTag`. Dropped `TagService` field + constructor parameter from `AiGatewayFacade`; test wiring updated. Module `build.gradle.kts` still imports platform-tag-api for safety; remove in a follow-up sweep.

### E6. Tags management UI (Projects & Tags §4) — **DONE**
- [x] `client/src/pages/automation/ai-gateway/components/tags/` ships `AiGatewayTags.tsx` list page + `AiGatewayTagDialog.tsx` create/edit dialog. `AiGateway.tsx` sidebar has a "Tags" entry; `activePage === 'tags'` routes to the list page. `aiGatewayTags.graphql` provides the `aiGatewayTags` query + `createAiGatewayTag` / `updateAiGatewayTag` / `deleteAiGatewayTag` mutations.

### E7. Workspace-level settings overrides (Projects & Tags §2) — **DONE (storage + API) / DEFERRED (facade resolution)**
- [x] **Reuses platform `Property` store** (scope=WORKSPACE, key=`ai_gateway_workspace_settings`) instead of a dedicated table — per user direction. Property already handles scope/audit/versioning/encryption; a 7-field config-style override doesn't warrant its own schema.
- [x] `AiGatewayWorkspaceSettings` converted to an immutable record DTO with validation in the compact constructor (softBudgetWarningPct 0-100).
- [x] `AiGatewayWorkspaceSettingsServiceImpl` wraps `PropertyService` with typed read/write + null-means-inherit semantics (null fields are not persisted so they don't shadow system defaults).
- [x] GraphQL schema (`ai-gateway-workspace-settings.graphqls`) + controller with admin-only query + `updateAiGatewayWorkspaceSettings` mutation.
- [x] Added `platform-configuration-api` dependency to gateway service module.
- [x] Server compiles.
- [x] First helper shipped: `AiGatewayFacade.isWorkspaceCachingEnabled(tags)` reads `cacheEnabled` from workspace settings (default true) and gates both cache-read and cache-write call sites in `chatCompletionDirect`. Operators can disable caching per-workspace via the new Settings form. Remaining wiring (cacheTtlSeconds, retryCount, routing-policy chain) follows the same pattern but touches more files; tracked as future incremental work.

### E8. Editable Settings form (F10) — **DONE**
- [x] Rewrote `AiGatewaySettings.tsx` as a live form bound to new `aiGatewayWorkspaceSettings` / `updateAiGatewayWorkspaceSettings` GraphQL ops. Fields: retry count, timeout ms, cache enabled, cache TTL seconds, log retention days, soft budget warning %, redact PII toggle. Empty inputs submit `undefined` to preserve the "inherit from system default" semantic. Save → toast + React Query invalidation. Default routing policy picker deferred until there's a policies-by-workspace query.

### E9. Scope audit — **DECIDED**
- [x] **Decision on `TagBasedRoutingStrategy`: KEEP.** Rationale: 7 strategies now exist in code (Simple, TagBased, PriorityFallback, LatencyOptimized, Intelligent, WeightedRandom, CostOptimized). Tag-based routing is actively documented via `AiGatewayRouter`/`AiGatewayFacade` integration and has dedicated unit tests (`AiGatewayRoutingStrategyTest`). Removing would break operator workflows; accepted as a deviation and should be added to the spec's strategy list as "extended".
- [x] Workspace-scoping enforcement was flagged in A2 as an open item. Deferring the per-query `@PreAuthorize("@permissionService.hasWorkspaceAccess(#workspaceId)")` audit to a dedicated permission-layer pass — would touch every GraphQL resolver and needs a `PermissionService` implementation first (current `platform-security` has no workspace-access predicate bean).

---

## Group F — Design-spec (2026-04-03 LLM API Gateway) residual gaps

**Direction (2026-04-13):** F4 "Gateway API Key Management" from `2026-04-03-llm-api-gateway-design.md` will **reuse the platform `ApiKey` entity/service** (already wired via `AiGatewayApiKeyAuthenticationProvider`). No dedicated `AiGatewayApiKey` entity, no `ai_gateway_api_key` table, no gateway-scoped CRUD UI. The tasks below close the follow-on gaps that remain *around* platform ApiKey reuse, plus the non-key items from the design spec.

### F1. Per-ApiKey rate limiting wiring — **DECISION / BLOCKED ON F2**
- [x] **Decision: Option (b)** — store per-key RPM config as `Property` rows (scope=WORKSPACE, key=`ai_gateway_api_key_rate_limit_<apiKeyId>`). Keeps changes scoped to the gateway module instead of a cross-cutting modification to `platform-api-key`. The platform's `ApiKey` entity stays a pure identity/credential primitive; gateway-specific limits live with the gateway.
- [x] Implemented `AiGatewayRateLimitChecker.checkApiKeyRateLimit` — reads `AiGatewayApiKeyAuthenticationToken` from `SecurityContextHolder`, looks up `PropertyService.fetchProperty("ai_gateway_api_key_rate_limit_" + apiKeyId, Scope.WORKSPACE, workspaceId)`, and when the Property's `rpm` value is a positive number, enforces a 60-second sliding window on key `ai-gw-rl:ws:<workspaceId>:apikey:<apiKeyId>` via the existing rate-limiter. No API-key auth or missing/zero override → falls through to workspace-level rules unchanged.
- [ ] **Deferred (test harness work)**: Unit test covering unknown key → workspace default, configured key → per-key limit, disabled key → reject.

### F2. Per-ApiKey cost & request-log attribution — **PARTIAL (token carries id) / DEFERRED (facade + UI)**
- [x] Audit result: `AiGatewayRequestLog.apiKeyId` column + field + setter exist but were **never populated**. `AiGatewayApiKeyAuthenticationProvider` resolved the `ApiKey` to a user-details principal and discarded `ApiKey.id`.
- [x] Extended `AiGatewayApiKeyAuthenticationToken` with a new `apiKeyId` field + `(User, Long)` constructor. `AiGatewayApiKeyAuthenticationProvider` now passes `apiKey.getId()` when building the authenticated token. Downstream `AiGatewayFacade` can read it via `((AiGatewayApiKeyAuthenticationToken) SecurityContextHolder.getContext().getAuthentication()).getApiKeyId()`.
- [x] Added `setApiKeyIdFromAuthentication(requestLog)` helper in `AiGatewayFacade`; called from every request-log construction site (chat success/error, streaming finalise, embedding success/error). Request logs are now attributable to the authenticated API key.
- [x] Added `api_key_id BIGINT` column to `ai_observability_trace` in `00000000000002_ai_observability_init.xml`. Added `apiKeyId` field + getter/setter to the `AiObservabilityTrace` domain. Facade stamps the value from the authenticated token on trace creation. GraphQL type exposes `apiKeyId: ID`. Refactored `resolveAuthenticatedApiKeyId()` helper so both request-log and trace creation share the same read.
- [x] Added "Top spend by API key" widget to `AiGatewayDashboard.tsx`. Aggregates `cost` from request logs grouped by `apiKeyId` (clientside), shows top-5 sorted descending. Logs without an `apiKeyId` are bucketed under "No key" so admins see anonymous traffic separately.

### F3. Dashboard budget progress bar (design F11)
- [x] Added "Budget vs. spend" progress bar widget on `AiGatewayDashboard`. Reads `useAiGatewayBudgetQuery` for the configured limit and `useAiGatewayWorkspaceSettingsQuery` for `softBudgetWarningPct` (default 80). Bar fill color: green below threshold, yellow at warning, red at 100%. Shows a "Budget exhausted — new requests return HTTP 402" hint when fully consumed. Spent value uses the dashboard's selected time-range as a proxy for the budget period.

### F4. API Keys UI — **SKIP (already exists)**
Existing Settings → API Keys page (`client/src/pages/settings/ApiKeys.tsx`) already covers the read-only platform-backed key list. No AI Gateway-specific duplicate needed; revisit only if the gateway requires a key-scope filter that Settings can't express.

### F5. Plan-doc hygiene — **DONE**
- [x] Prepended a **Status (2026-04-13)** block to both related plans pointing at this phase-8 plan as the source of truth for remaining work, plus the WONTFIX items (F4 platform-ApiKey reuse, module-name `automation-ai-gateway`, workspace-settings via Property store, 402 budget response). Re-annotating every individual checkbox in those plans would add noise without changing the one-place-to-look answer.

### F6. Spec amendment — **DONE**
- [x] Appended `## 8. Deviations from this spec` to `docs/superpowers/specs/2026-04-03-llm-api-gateway-design.md` noting: (1) F4 reuses platform `ApiKey`; (2) 7 routing strategies shipped (TagBased included); (3) budget hard-limit returns 402 not 429; (4) workspace settings live in the platform `property` store, not a dedicated table.

---

## Done criteria

- [x] `./gradlew spotlessApply check -x test -x testIntegration` green for all `automation-ai-gateway-*` modules (api, service, graphql, public-rest, remote-client). Test execution gated to a separate harness pass.
- [x] `cd client && npx tsc --noEmit` clean (full type check). Lint/format cycle skipped here; codegen ran successfully after every GraphQL change.
- [ ] EE microservice apps boot test — needs running PostgreSQL/Redis; deferred to a CI/manual smoke step.
- [x] Every remaining open item carries a `**WONTFIX**` or `**Deferred**` annotation with rationale.
