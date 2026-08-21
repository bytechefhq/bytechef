# Plan limits, cost calculation, and alert rules — connected design

Date: 2026-07-20
Status: Design + phase 1 (plan-limit placeholders) implemented; phases 2–3 planned
References: docs.sim.ai/platform/costs, sim.ai/pricing, docs.sim.ai/logs-debugging/alerts
(all three proxy-403'd; content verified against the open-source `simstudioai/sim` repo that
powers Sim cloud — `apps/docs/content/docs/en/platform/costs.mdx`, `.../logs-debugging/alerts.mdx`,
`apps/sim/lib/billing/constants.ts`, `apps/sim/lib/core/rate-limiter/types.ts`)

## 1. How the three tasks connect

They are one vertical, built bottom-up:

```
┌─────────────────────────────────────────────────────────────┐
│ 3. ALERTS (feedback layer)                                  │
│    rule engine over execution outcomes + per-run cost +     │
│    usage-vs-limit thresholds; delivery via notifications    │
├─────────────────────────────────────────────────────────────┤
│ 2. COST CALCULATION (metering layer)                        │
│    per-execution cost = base run charge + Σ AI usage;       │
│    period rollups feed usage meters and plan ceilings       │
├─────────────────────────────────────────────────────────────┤
│ 1. PLAN LIMITS (policy layer)                               │
│    per-tenant tier → numeric limits: cost ceiling, sync/    │
│    async rpm, concurrency, timeouts, storage, retention     │
└─────────────────────────────────────────────────────────────┘
```

- **Plan limits** define the numbers (what is a tenant allowed to do).
- **Cost calculation** produces the consumption facts those numbers are compared against
  (how much has the tenant done), and per-run costs the alert rules evaluate.
- **Alerts** watch both streams — execution outcomes (failures, latency) and the
  metering/limits pair (expensive run, budget threshold crossed, rate-limit pressure) —
  and deliver through the notification system.

Enforcement (rate limiters, concurrency slots) reads layer 1; alert rules read layers 1+2.
Building order is therefore: placeholders (1) → metering (2) → rules (3) → enforcement.

## 2. What Sim's model is (verified numbers)

**Cost.** 1 credit = $0.005. Every run pays a **base run charge of 1 credit**; AI blocks add
`(inputTokens × inputPrice + outputTokens × outputPrice) / 1M` (prices per 1M tokens),
with a **1.1× multiplier** when using Sim-hosted model keys (BYOK = no markup). Cost is
tracked per block and summed per run; nested workflow runs fold into the parent.

**Plans** (repo main, July 2026): Free $0 / 1,000 one-time credits; Pro $25/mo / 6,000
credits; Max $100/mo / 25,000 credits; Enterprise custom. Teams pool per-seat credits.
Default behavior at the ceiling: **runs stop**; opt-in on-demand billing allows overage at
$0.005/credit with auto-invoicing at $100 unbilled.

**Per-plan limits:**

| Limit | Free | Pro | Max | Enterprise |
|---|---|---|---|---|
| Sync API req/min | 50 | 150 | 300 | 600 |
| Async API req/min | 200 | 1,000 | 2,500 | 5,000 |
| General API req/min | 30 | 100 | 200 | 500 |
| Burst | 2× sustained (token bucket) | | | |
| Concurrent executions | 10 | 50 | 200 | 1,000 (custom) |
| Sync run timeout | 5 min | 50 min | 50 min | 50 min |
| Async run timeout | 90 min | 90 min | 90 min | 90 min |
| Personal workspaces | 1 | 3 | 10 | unlimited shared |
| File storage | 5 GB | 50 GB | 500 GB | 500 GB (custom) |
| Log retention | 7 days | indefinite | indefinite | configurable + drains |

Concurrency slots are held while **queued+running** for async, **running** for sync.

**Alerts.** Workspace-scoped, each with exactly one rule out of seven: consecutive
failures (default 3), failure rate (% + window h), error count, latency threshold
(default 30 s), latency spike (% over recent average), **cost threshold (per-run)**, and
no-activity (background poll). Rate rules need ≥5 runs in window; fixed 1 h cooldown per
alert. Scope: all or specific workflows, plus level/trigger-type filters. Channels:
webhook (HMAC-signed `t.body` payload, 5 retries at 5s/15s/60s/3m/10m), email (≤10
recipients), Slack. Max 10 alerts per channel type.

## 3. What ByteChef already has (recon results)

- **Metering**: EE `platform-ai-llm-usage` — `ai_llm_usage` is the canonical per-LLM-call
  row (tokens, cost DECIMAL(10,6), model/provider, workspace join table); `ai_tool_usage`
  mirrors it per tool call (already wired for research/image/slide subagents via
  `MeteredToolCallback`). `AiGatewayModel` stores per-model input/output cost per 1M
  tokens. Two cost contracts exist: `AiGatewayCostCalculator` (throws on missing rates)
  and `LlmCostEstimator`/`ToolCostEstimator` (zero on unknown; `DefaultCostEstimator`
  reads `bytechef.ai-hub.cost-estimation` rates). CE captures tokens via
  `TokenUsageHolder` + `ModelUtils.captureTokenUsage`.
  **Gaps**: `ai_gateway_spend_summary` has a full read stack but **no writer**;
  `LlmUsageRecorder` has no callers outside its module (AI Hub LLM usage not yet
  recorded); **no per-job execution cost exists** — nothing ties `ai_llm_usage` rows to
  an Atlas job, and there is no base-run-charge concept.
- **Quota/limits**: `LicenceJobUsageService` (monthly job-count quota, atomic
  `incrementIfBelow`, `JobLimitExceededException`) is the working consume-or-throw
  template. AI Gateway has DB-configured request rate limits (fixed-window, in-memory or
  Redis INCR+EXPIRE, fail-closed, 429 + Retry-After) — scoped to gateway endpoints only.
  Asset files have per-file/per-workspace byte quotas. **No plan/tier domain exists.**
- **Notifications**: CE `platform-notification` — `Notification` (EMAIL|WEBHOOK +
  settings map) subscribed to `NotificationEvent.Type` JOB_* events, fired from
  `NotificationJobStatusApplicationEventListener` on job status transitions. Email works
  end-to-end (`JobStatusEmailNotificationHandler` → `MailService`). **Correction to
  "webhook exists, needs UI": it is the reverse — the UI already offers the WEBHOOK
  channel with a URL field, but `WebhookNotificationSender.send()` is an empty stub and
  no `WebhookNotificationHandler` implementation exists.** A production-grade webhook
  dispatcher (SSRF-guarded HttpClient, Slack, lastError bookkeeping) already exists in
  the EE AI-observability stack to borrow from. Known bugs: JOB_CANCELLED can never fire
  (no such Job.Status); JOB_STOPPED fires but has no handler/message keys (NPE risk).
- **Atlas async seams**: submissions funnel through `PrincipalJobFacadeImpl.createJob`
  (tenant on thread, licence gate already there) → `JobFacadeImpl` → Spring event →
  `MessageEventListener` (stamps `CURRENT_TENANT_ID` metadata) → broker. Consumer-side
  concurrency is honored by AMQP/JMS only; **Kafka and Redis registrars ignore it**, and
  the memory broker is effectively unbounded — the real bound today is the shared
  `taskExecutor` pool (max 50). `JobSyncExecutor` bypasses the broker entirely.
  Interception seams: `MessageEventPreSendProcessor`, `TaskDispatcherPreSendProcessor`,
  consumer delegate wrappers, `calculateQueueName`.

## 4. Answer: can Spring rate-limit for us instead of reinventing the wheel?

**Spring Framework/Boot itself: no.** Neither Framework 7 nor Boot 4 ships a request
rate limiter. The new `org.springframework.resilience` package provides `@Retryable` and
`@ConcurrencyLimit` — the latter is a per-JVM, per-method semaphore on *concurrent*
invocations with **no key dimension** and blocking semantics; it cannot express "N
requests/min per workspace" (10,000 fast sequential calls sail through
`@ConcurrencyLimit(2)`). It remains useful as a coarse in-process bulkhead (e.g. polyglot
script execution), never as a tenant quota.

**Spring Cloud Gateway's `RequestRateLimiter`** (Redis Lua token bucket) is real, but it
only exists as an edge filter: the CE monolith serves traffic directly, so the quota
logic must live in shared server code regardless. It is a legitimate *complement* at the
EE `api-gateway-app` for coarse per-IP anti-abuse — not the implementation.

**The ecosystem answer is Bucket4j** (`bucket4j-core`, Apache 2.0, ~0.3 MB, zero
transitive deps): true token bucket (sustained rate + burst capacity — exactly Sim's
"burst = 2× sustained" semantics), lazy thread-free refill, and per-key distributed
state via `ProxyManager` with first-party **Redis/Lettuce**, JCache/Hazelcast, and
**PostgreSQL** backends. Decisive signal: Spring Cloud Gateway's own WebMVC variant
implements its rate-limit filter *on Bucket4j* — this is what Spring itself reaches for
in servlet land. (The `bucket4j-spring-boot-starter` still targets Boot 3.x — use
`bucket4j-core` directly behind our own thin filter; ~100 LOC.)

**Resilience4j RateLimiter is not suitable** for per-tenant quotas: state is per-JVM, so
N replicas silently grant N× the quota and every deploy resets counters.

**Recommended shape** (mirrors our existing provider patterns):

- A small CE `RateLimiter` SPI (`tryConsume(key, PlanRate) → allowed/remaining/resetAt`)
  in a platform module, implemented on `bucket4j-core` with the backend chosen by
  deployment config: Caffeine-local (single node, default) → Redis/Lettuce
  (`spring-data-redis` already on the classpath) → PostgreSQL ProxyManager for
  distributed-without-Redis and low-RPS keys (job submission). This can later absorb the
  hand-rolled AI-gateway fixed-window limiters.
- Sync/API HTTP limits: `OncePerRequestFilter` ordered after Spring Security, key =
  `tenantId[:routeClass]`, limits from `PlanLimitsProvider` (§6), reply 429 +
  `Retry-After` + `X-RateLimit-*` (the AI gateway's `AiGatewayExceptionHandler` already
  models this response shape).
- Metrics: outcome-tagged counters following the `bytechef_workflow_chat_turn{outcome}`
  convention.

## 5. Answer: async Atlas execution limits in a multitenant deployment

Two different knobs protect two different resources; we need both, and both must live in
**app code, not broker config**, because our broker abstraction spans six backends of
which only AMQP/JMS honor consumer concurrency, and `JobSyncExecutor` bypasses the
broker entirely.

**A. Admission control at enqueue — the quota (primary).**
`PrincipalJobFacadeImpl.createJob` is the single funnel for principal-attributed
submissions (webhook async, schedules, MCP/A2A, deployments): the tenant id is on the
thread (`TenantContext`), and the licence job-count gate already lives there — the new
per-tenant **async rpm token bucket** (Bucket4j, Postgres backend suffices at submission
RPS) and the **concurrent-execution slot check** go right next to it. Reject = typed
exception → HTTP 429 + `Retry-After` while the caller is still on the line. This bounds
queue growth (the broker never becomes an unbounded buffer for a runaway tenant) and is
broker-agnostic by construction.

**B. Per-tenant concurrency slots — execution fairness (secondary).**
Sim semantics: an async run holds a slot while **queued+running**; sync while running.
Implementation: a per-tenant in-flight counter (Redis INCR/DECR when available, else a
small DB table with atomic `incrementIfBelow` — the `LicenceJobUsageService` pattern):
- acquire at admission (async) or at `JobSyncExecutor` start (sync);
- release on terminal job status — the existing `JobStatusApplicationEvent` stream
  already fires on every transition (it is what drives notifications today), so the
  slot-release listener rides the same event;
- crash-safety: TTL on Redis slots / a sweeper that reconciles the counter against
  actual non-terminal jobs (the `WorkflowChatGuard` in-flight cache uses the same
  self-healing-TTL idea).
Over-cap behavior: reject at admission (simplest, Sim-like "runs stop") in phase 1;
optional queue-and-delay via `TaskDispatcherPreSendProcessor` re-scheduling later.

**Not recommended**: per-tenant queues via `calculateQueueName` (queue explosion per
tenant, and three of six brokers ignore consumer concurrency anyway); consumer-side-only
throttling (defers noisy-neighbor damage into queue ordering and gives the tenant a
false 202).

## 6. Phase 1 — plan-limit placeholders (implemented)

New CE module `server/libs/platform/platform-plan` (`-api`/`-service`), modeled on the
licence stack (CE interface + defaults, EE/SaaS billing can override the provider bean):

- `PlanTier` enum: `FREE, PRO, TEAM, ENTERPRISE` (Sim's Free/Pro/Max/Enterprise mapped
  to ByteChef naming).
- `PlanLimits` record — nullable = unlimited: `includedMonthlyCostUsd`,
  `syncRequestsPerMinute`, `asyncRequestsPerMinute`, `apiRequestsPerMinute`,
  `burstMultiplier` (default 2), `maxConcurrentExecutions`, `syncRunTimeout`,
  `asyncRunTimeout`, `maxWorkspaces`, `maxStorageBytes`, `logRetentionDays`,
  `maxMembers`; `PlanLimits.unlimited(tier)` factory.
- `PlanLimitsProvider` SPI: `getPlanLimits(tenantId)` +
  `getCurrentPlanLimits()` (TenantContext convenience).
- `DefaultPlanLimits`: the per-tier placeholder table with the Sim-derived numbers above
  (credits converted at $0.005: Free $5 one-time → modeled as monthly for now, Pro $30,
  Team $125, Enterprise unlimited).
- `PropertiesPlanLimitsProvider` (`@ConditionalOnMissingBean(PlanLimitsProvider.class)`):
  resolves `bytechef.plan.tier` — **unset (default) = self-hosted = `unlimited()`**, so
  no existing deployment changes behavior; setting a tier activates the placeholder
  table; any individual number is overridable via `bytechef.plan.limits.*` properties.

Nothing enforces these yet — enforcement points are §4/§5; consumers just inject
`PlanLimitsProvider`.

## 7. Phase 2 — cost calculation (partially implemented)

Sim-equivalent formula, ByteChef terms (USD-denominated to match `Money`/`ai_llm_usage`;
credits stay a UI presentation choice at $0.005 if we adopt them):

```
executionCost = baseRunCharge                      // bytechef.plan.cost.base-run-charge-usd, default 0.005, 0 = disabled
              + Σ ai_llm_usage.cost  (rows attributed to the job)
              + Σ ai_tool_usage.estimated_cost_usd (rows attributed to the job)
```

Work items:
1. **Job attribution**: add `jobId` to `LlmUsageContext`/`ToolUsageContext` and thread it
   from execution surfaces (AI agent component via `TokenUsageHolder` capture already
   reaches `AiAgentTestFacadeImpl`; the workflow path needs the job id from
   `TaskExecution`). Wire `AiHubModelUsageLoggingAdvisor` → `LlmUsageRecorder.recordLlm`
   (the advisor is already on every ASK/BUILD agent; the recorder contract exists with
   zero callers).
2. **`workflow_execution_cost` row per job** (CE api + EE persistence, following the
   tool-invocation-log split): jobId, tenant/workspace, baseCharge, aiCost, toolCost,
   totalCost, computed on terminal `JobStatusApplicationEvent` (COMPLETED/FAILED/STOPPED)
   by summing attributed usage rows. Exposed on the execution detail UI.
3. **Rollup writer**: scheduled aggregation into `ai_gateway_spend_summary` (its read
   stack + budget checker are waiting for exactly this) and a per-tenant month-to-date
   usage meter that plan-ceiling enforcement (§4) and threshold alerts (§8) read.
4. Model pricing source: reuse `AiGatewayModel` rates where the gateway is enabled;
   fall back to the `bytechef.ai-hub.cost-estimation` rate sheet (`DefaultCostEstimator`)
   elsewhere — both already exist; no third pricing store.

## 8. Phase 3 — notification alert rules (core implemented)

Status: the rule engine is BUILT — EE module `automation-workflow-alert` (`-api`/`-service`/`-graphql`):
`workflow_alert_rule` (workspace-scoped, optional workflowId scope, INT-ordinal `WorkflowAlertRuleType`
with all 7 types, per-rule rolling state columns so evaluation never scans job history) +
`workflow_alert_rule_notification` (delivery targets = `Notification` rows, FK CASCADE) +
`workflow_alert_event` history (100 newest per workspace via GraphQL). Evaluation:
`WorkflowAlertApplicationEventListener` joins the coordinator fan-out `@Order(200)` — after the cost
listener `@Order(100)` so COST_THRESHOLD reads the fresh cost row; pure core in
`WorkflowAlertEvaluator` (tumbling windows, >= 5 runs floor for FAILURE_RATE, pre-update EWMA baseline
alpha 0.2 for LATENCY_SPIKE, fixed cooldown default 60 min); NO_ACTIVITY via
`WorkflowAlertNoActivityMonitor` 5-min poll (re-alerts once per cooldown while silent). Delivery:
`WorkflowAlertDispatcher` (@Async) through the central transports — MailService / WebhookNotificationClient
(`workflow.alert` event, signed when `webhookSecret` set) / SlackNotificationClient. GraphQL CRUD:
queries `isAuthenticated()`, mutations `ROLE_ADMIN`. Workspace scoping on `Notification` is DONE (`workspace_notification` membership table in
`platform-notification-workspace`; no membership row = global) and `AiObservabilityNotificationChannel`
is MIGRATED onto `Notification` (Liquibase `20260720000004`: type remap, config->settings mapping —
lossy: first email recipient only, custom webhook headers dropped — rule join repointed to
`notification_id`, channel tables dropped; dispatcher now reads Notification rows and delivers via
MailService + the shared clients). The client alerts UI exists (Settings -> Alerts). Still deferred:
USAGE_THRESHOLD (needs billing-period spend vs plan ceiling) and the send-test affordance.

Original design sketch (rule model reuses the existing trigger path):

- **`notification_alert_rule` table**: ruleType (CONSECUTIVE_FAILURES, FAILURE_RATE,
  ERROR_COUNT, LATENCY_THRESHOLD, LATENCY_SPIKE, COST_THRESHOLD, NO_ACTIVITY,
  USAGE_THRESHOLD*), numeric threshold, window, optional workflow scope, cooldown state
  (fixed 1 h to start). *USAGE_THRESHOLD (period spend ≥ N% of plan ceiling) is our
  addition — Sim exposes it via budget emails; we fold it into the same rule engine.
- **Evaluation**: on the existing `JobStatusApplicationEvent` terminal transitions
  (same listener family as `NotificationJobStatusApplicationEventListener`), reading
  duration from the job row and cost from phase 2's `workflow_execution_cost`;
  NO_ACTIVITY via a scheduled poll. Rate rules need ≥5 runs in window (Sim semantics).
- **Delivery — one central point (requirement)**: webhook + Slack transports live in
  the CE `platform-notification-delivery` module. `WebhookNotificationClient` is the
  single outbound-webhook transport — one `RestTemplate`, one Spring core
  `RetryTemplate`/`ExponentialBackOff` retry mechanism (the mechanics that previously
  lived inline in the Atlas job-callback listener). Entry points: `deliver(request[,
  retry])` for admin-configured notification webhooks (SSRF validation via commons-util
  `UrlValidator`, standard `X-ByteChef-Event/Timestamp/Delivery` headers, optional
  Sim-compatible HMAC signature
  `X-ByteChef-Signature: t=<ts>,v1=hex(HMAC-SHA256(secret, "<ts>.<body>"))`, non-2xx →
  typed exception) and `deliverEvent(url, payload, retry)` for the Atlas per-job
  callback webhooks (`Job.getWebhooks()` — no SSRF, converter-serialized payload,
  pre-existing contract preserved); `WebhookJobStatusApplicationEventListener`
  (platform-coordinator) delegates here with the job's `Job.Retry` schedule (defaults 5
  attempts / 2 s initial / 2.0 multiplier) instead of owning its own RestTemplate.
  `SlackNotificationClient` (incoming-webhook transport owning the payload shape;
  callers pass message text only) delegates to the webhook client. **Email is NOT a
  separate transport**: the async templated `MailService` (platform-mail) is the single
  email path for user-account mail and notification email alike —
  `EmailNotificationSender` calls `mailService.sendEmail(...)` directly. The EE
  `AiObservabilityNotificationDispatcher` keeps its inline optional-`JavaMailSender`
  alert email (sync throw semantics feed per-channel `lastError`) until its channels
  migrate onto `Notification`. **`platform-notification` is the central registry for
  notifications AND channels**: `Notification.Type` carries every channel first-class
  (`EMAIL`, `WEBHOOK`, `SLACK`, ordinal append-only) with a sender+handler pair per
  type, so phase-3 alert rules attach to `Notification` rows as their delivery targets
  rather than defining channel entities of their own; the EE
  `AiObservabilityNotificationChannel` table migrates onto `Notification` in that
  build (prerequisite: workspace scoping on `Notification`, which the alert-rules
  schema needs anyway). Other consumers: the CE `WebhookNotificationSender` (job-status
  webhook channel — previously a no-op stub, now real, with `webhookSecret` in settings
  + UI) and the EE `AiObservabilityNotificationDispatcher` (delegates webhook/Slack
  mechanics to the clients while keeping its channel config + lastError bookkeeping).
  Phase-3 alert rules deliver through the same clients. **No atlas change**: the trigger
  path stays `JobStatusApplicationEvent` → platform-coordinator listener → sender
  registry; nothing under `server/libs/atlas/` is touched. Retry schedule (Sim's
  5s/15s/60s/3m/10m) maps onto the client's `WebhookRetry` record.
- **Fixes rolled in**: JOB_STOPPED now has email message keys and both handlers cover
  it; the listener skips (with a warning) event/channel combinations that lack a
  sender or handler instead of NPE-ing. JOB_CANCELLED now fires: Job.Status
  gained an append-only CANCELLED value, set when a job is stopped while still CREATED
  (never started); STOPPED remains the mid-run interruption status.

## 9. Build order and effort

1. ✅ Placeholders (`platform-plan`).
1b. ✅ Cost calculation core: `TokenUsageHolder` carries the model and is bracketed
   around `ActionDefinitionFacadeImpl.executePerform` (reentrancy-safe for agent tool
   sub-actions), emitting the CE `WorkflowLlmUsageEvent`; the EE
   `automation-workflow-execution-cost` module records `ai_llm_usage` rows
   (`source=AI_AGENT`, `ownerId=jobId`) and writes one `workflow_execution_cost` row per
   terminal job (`base run charge (bytechef.workflow.execution-cost.*, default $0.005)
   + Σ AI usage`), idempotent per job, workspace-resolved via deployment → project.
   The hourly `AiGatewaySpendRollupJob` (gateway-service, :05 past each hour) now
   aggregates `ai_llm_usage` into `ai_gateway_spend_summary` per workspace grouped by
   (provider, model, apiKeyId, projectId) — the budget checker finally has a producer.
   Remaining: streaming-path token capture, AI Hub advisor → recorder wiring, cost
   display in the execution UI.
2. ✅ Cost calculation (phase 2): attribution + per-job cost row + rollup (~2 slices:
   CE seam, EE persistence — mirrors the tool-invocation-log build). AI Hub metering
   advisor, cost GraphQL surface and execution-sheet cost display are in; workspace
   attribution goes through the `workspace_workflow_execution_cost` membership table.
3. ✅ Alert rules (phase 3): `automation-workflow-alert` (8 rule types incl.
   USAGE_THRESHOLD), delivery via `Notification` ids, Alerts UI + send-test.
4. ✅ Enforcement (phase 4) — implemented in CE `platform-rate-limit`:
   - `RateLimiter` SPI + `Bucket4jRateLimiter` (`com.bucket4j:bucket4j_jdk17-core`,
     local buckets in a Caffeine cache, capacity = rate × burst multiplier, greedy
     per-minute refill). Per-node by default; `bytechef.plan.enforcement.provider=redis`
     switches BOTH the limiter and the concurrency gate onto shared Redis state
     (`RedisRateLimiter` — atomic Lua token bucket with the same capacity/refill
     semantics; `RedisConcurrentExecutionGate` — bounded INCR/DECR, floored release,
     24h TTL refreshed per operation so crash-orphaned slots self-heal). Both Redis
     implementations fail OPEN on Redis outages: an enforcement outage must not become
     a platform outage. Requires a `RedisConnectionFactory` bean.
   - `PlanRateLimitFilter` (`FilterRegistrationBean`, order 0 — after the Spring
     Security chain at -100 so the auth outcome is visible): login POST
     `/api/authentication` fixed 10/min/IP; `/webhooks/**` → sync tier per tenant;
     `/api/automation/v1/**` + `/api/embedded/v1/**` → api tier per tenant; anonymous
     `/api/**` → per-IP preauth using the sync tier. Null limit (SELF_HOSTED) → pass;
     reject → 429 + `Retry-After: 60`.
   - Async submission rate: `PrincipalJobFacadeImpl.createJob` consumes the per-tenant
     `async:<tenant>` bucket (`asyncRequestsPerMinute` × burst) BEFORE acquiring a
     concurrency slot (a rate-rejected submission must never leak a slot), throwing
     `JobRateLimitExceededException` when exhausted.
   - `ConcurrentExecutionGate`: per-tenant in-flight slots. Acquired in
     `PrincipalJobFacadeImpl.createJob` (async admission ONLY — the sync
     `createJobWithoutDispatch` path is deliberately ungated to avoid slot leaks, since
     sync completion events may not traverse the coordinator fan-out), throwing
     `JobConcurrencyLimitExceededException` when `maxConcurrentExecutions` is reached;
     released by platform-coordinator's `ConcurrencySlotReleaseApplicationEventListener`
     on terminal `JobStatusApplicationEvent` (floors at zero, so redelivery and
     restart-reset are safe — a node restart temporarily over-admits, never blocks).
   - Everything is gated by `bytechef.plan.enforcement.enabled` (default on;
     SELF_HOSTED's all-null limits make it a no-op) via
     `PlanRateLimitAutoConfiguration`.
   - Decision: the dormant `ai_gateway_workspace_settings.max_rpm/max_tpm` columns stay
     dormant — plan-level request limits supersede them; they remain available for a
     future per-model/per-workspace override layer.
