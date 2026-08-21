# Execution reliability

Plan limits and enforcement, crash recovery for orphaned jobs, notification delivery, the workflow error handler, and public URL signing.

Extracted from `CLAUDE.md` to keep that file within its size budget;
read this before working in the areas below.

## Plan limits (placeholders)

- `server/libs/platform/platform-plan` (`-api`/`-service`, CE) holds the plan-tier policy layer:
  `PlanTier` (SELF_HOSTED default + FREE/PRO/TEAM/ENTERPRISE), `PlanLimits` record (every limit
  nullable, **null = unlimited — never zero**), and the `PlanLimitsProvider` SPI. The default
  `PropertiesPlanLimitsProvider` resolves `bytechef.plan.tier` (unset = SELF_HOSTED = unlimited,
  the pre-plan behavior) with per-field `bytechef.plan.limits.*` overrides; a billing integration
  replaces the bean (`@ConditionalOnMissingBean`). Tier tables in `DefaultPlanLimits` are
  Sim-modeled placeholders pinned by `DefaultPlanLimitsTest`.
  Design + phased plan (cost calculation, alert rules, Bucket4j rate limiting, Atlas admission
  gate): `docs/superpowers/specs/2026-07-20-plan-limits-cost-alerts-design.md`.
- **Enforcement** lives in CE `server/libs/platform/platform-rate-limit`
  (`bytechef.plan.enforcement.enabled`, default on — SELF_HOSTED's all-null limits make it a
  no-op): `Bucket4jRateLimiter` (local buckets in Caffeine; per-node default — set
  `bytechef.plan.enforcement.provider=redis` for strict global limits via `RedisRateLimiter`
  (Lua token bucket) + `RedisConcurrentExecutionGate` (bounded INCR/DECR, 24h self-healing
  TTL); both Redis impls fail open on Redis outages — pinned against a real Redis by
  `RedisPlanEnforcementIntTest` (Testcontainers)), `PlanRateLimitFilter`
  (order 0, after the security chain: login 10/min/IP, webhooks + the MCP/A2A secret-key
  endpoints (`/{secretKey}/mcp|sse|message`, `/api/automation/a2a/**`) → sync tier/tenant, public
  APIs → api tier/tenant, anonymous `/api/**` → per-IP; reject = 429 + Retry-After), and
  the two async-admission gates in `PrincipalJobFacadeImpl.createJob` plus the monthly-cost cap
  (`PlanSpendProvider` SPI, EE impl over cost rows, 60s memo, fail-open; over-cap submissions can
  be admitted under the tenant's on-demand overage terms via the stub `PlanOveragePolicyProvider`
  SPI — `PlanOveragePolicy(enabled, unbilledLimitUsd)`, Sim's opt-in overage model — no default
  bean, so the cap hard-stops until the billing integration contributes one) (async only; sync
  `createJobWithoutDispatch` is deliberately ungated to avoid slot leaks): the
  `async:<tenant>` submissions-per-minute bucket (checked FIRST so a rate reject never
  leaks a slot) then `ConcurrentExecutionGate` slots, released by platform-coordinator's
  `ConcurrencySlotReleaseApplicationEventListener`
  on terminal job status (floors at zero; restart over-admits, never wrongly blocks). Never
  gate inside `server/libs/atlas/` — admission and release both live outside the engine. Every
  rejection increments
  `bytechef_plan_limit_rejection{limit=login|sync|api|preauth|async|concurrency|cost|timeout|workspace|member|storage}`
  (`PlanLimitRejectionCounter`, no-op without a MeterRegistry).
- **Quota fields** are enforced at their natural creation points, each via an optional
  `ObjectProvider<PlanLimitsProvider>` (null limit / no bean = unlimited): `maxWorkspaces` in EE
  `WorkspaceServiceImpl.create`, `maxMembers` in `UserServiceImpl.create`/`registerUser` (counts ALL
  user rows — pending invites hold a seat; checked after the non-activated-user cleanup),
  `maxStorageBytes` in `AssetFileFacadeImpl` (tenant-wide `sumSizeBytes()` alongside the existing
  per-workspace property quota), `syncRunTimeout` caps the `JobCompletionAwaiter` wait on ALL
  three sync surfaces — `WebhookWorkflowExecutorImpl`, `AutomationMcpToolFacade`, and
  `AutomationA2AServerFacade` (plan can only tighten the configured default, never extend), and
  `logRetentionDays` drives `JobRetentionMonitor` (platform-coordinator, 6h per-tenant sweep,
  `getEndedJobs(endDateBefore)` finder — endDate exists only on terminal jobs — deleting through
  `JobFacade.deleteJob`'s cascade and skipping subflow children; works distributed via the remote
  job service/facade endpoints; operator fallback
  `bytechef.workflow.execution.retention.default-retention-days`, disable with
  `bytechef.workflow.execution.retention.enabled=false`). `JobFacadeImpl.deleteJob` also releases
  file-storage blobs (task outputs, job outputs, context values via `TaskFileStorage.delete*`) and
  context rows (`ContextService.getStackFileEntries`/`deleteStackContexts`) best-effort — a storage
  failure never blocks the row delete; in-memory repos throw `UnsupportedOperationException` for
  context enumeration and the facade skips that portion. The retention monitor additionally drops
  the purged job's `data_storage` CURRENT_EXECUTION rows via `DataStorage.deleteScopeData(scope,
  scopeId)` (jdbc provider + remote client implement it; the file-storage provider throws and the
  monitor skips). Quota rejections throw
  `QuotaLimitExceededException` (core exception-api) → HTTP 403 without Retry-After — a capacity
  ceiling, not a retryable rate limit (`RateLimitExceededException` stays 429) — and count into the
  rejection metric with tags `workspace`/`member`/`storage`.

## Crash recovery (orphaned jobs)

- Workers publish `TaskHeartbeatApplicationEvent` every 30s per in-flight task (scheduler inside
  `TaskWorker`, tenant captured at task receipt); the coordinator-side
  `TaskHeartbeatApplicationEventListener` re-saves the STARTED row, bumping `lastModifiedDate`.
  `OrphanedJobRecoveryMonitor` (platform-coordinator, every minute) then treats a job as orphaned
  only when the job row AND all its non-terminal task executions are stale
  (`bytechef.workflow.execution.recovery.staleness-threshold`, default PT5M) — children's
  heartbeats keep control-flow parent tasks alive transitively. Recovery marks tasks + job FAILED
  (normal job-status fan-out fires) making the job resumable via the existing
  `resumeToStatusStarted` path; `bytechef.workflow.execution.recovery.auto-resume=true` (default
  false) also publishes `ResumeJobEvent` — at-least-once semantics, the interrupted task re-runs
  from the last completed node — capped by `max-auto-resume-attempts` (default 3) tracked in job
  metadata. Disable the whole monitor with `bytechef.workflow.execution.recovery.enabled=false`.
  Stale-row finders are `getStaleTaskExecutions`/`getStaleJobs`, and they work in distributed EE
  too: `RemoteJobServiceClient` (`getStaleJobs`/`getLongRunningJobs`/`getEndedJobs`) and
  `RemoteTaskExecutionServiceClient.getStaleTaskExecutions` make real REST calls, against matching
  endpoints on `RemoteJobServiceController` and `RemoteTaskExecutionServiceController`. The monitor's
  `UnsupportedOperationException` warn-skip is a fallback for remote clients that lack them, not the
  normal path. Detection lives OUTSIDE `server/libs/atlas/` except the engine-owned heartbeat
  primitives; semantics pinned by `OrphanedJobRecoveryMonitorTest`, and the underlying
  stale/long-running SQL finders by `StaleExecutionFinderIntTest` (Testcontainers PG).
- **Per-run timeouts**: `JobTimeoutMonitor` (platform-coordinator, every minute,
  `bytechef.workflow.execution.timeout.enabled` default on) fails STARTED jobs whose runtime
  exceeds the plan's `asyncRunTimeout` (per tenant) or the operator fallback
  `bytechef.workflow.execution.timeout.default-timeout`; with neither set it is a no-op. Uses the
  startDate-based finder `getLongRunningJobs` (remote clients throw, monitor skips). No
  auto-resume — a timed-out run would immediately exceed again. Pinned by `JobTimeoutMonitorTest`.
- **Mockito gotcha**: unstubbed wrapper-returning methods (Long/Integer) return 0, NOT null — stub
  `thenReturn(null)` explicitly when a null-means-absent field (e.g. `Job.getParentTaskExecutionId`)
  drives branching.
- **Redis broker redelivery**: `RedisListenerEndpointRegistrar` reclaims consumer-group pending
  entries left by crashed consumers (XPENDING + XCLAIM sweep every 10s, min idle 60s) and
  redelivers them through the normal invoke-then-ack path — at-least-once semantics like amqp.
- **Transactional completion**: `DefaultTaskCompletionHandler` takes an optional
  `TransactionTemplate` (coordinator config wires it from `ObjectProvider<PlatformTransactionManager>`)
  and runs update-task + push-context + advance-job (+ next-task create/dispatch) in ONE
  transaction — closing the half-advanced coordinator-crash window. Dispatch stays correct because
  `TaskExecutionEvent`/`JobStatusApplicationEvent` are `MessageEvent`s and `MessageEventListener`
  is `@TransactionalEventListener(AFTER_COMMIT, fallbackExecution = true)` — deferred under a
  transaction, immediate without one. `JobSyncExecutor` passes null (in-memory sync path,
  unchanged). Pinned by `DefaultTaskCompletionHandlerTest`.
- **Agent-loop checkpoints**: `SuspendableToolCallingManager` takes an optional per-tool-round
  checkpointer; the AI Agent writes `AiAgentConversationCheckpoint` (SHA-256 input-parameter
  fingerprint + `ConversationState`) to `Data.Scope.CURRENT_EXECUTION` after each completed
  round, `AiAgentChatAction.perform` restores it on a crash-resumed job (fingerprint must match
  — protects against a different agent node in the same job) and clears it on success. Editor /
  job-less runs skip it; all checkpoint I/O is fail-open (a storage failure never fails the
  turn). Fingerprint computation is deliberately lazy (inside the write lambda).

## Notification delivery (central point)

- **`platform-notification` is THE central registry for notifications AND channels.** All channel
  types are first-class on `Notification.Type` — `EMAIL, WEBHOOK, SLACK` (INT ordinal, append-only) —
  with settings keys `email` / `webhook` + `webhookSecret` / `slackWebhookUrl` and a sender + handler
  pair per type (`Email|Webhook|SlackNotificationSender`, `JobStatus*NotificationHandler`). New
  notification surfaces and alert rules must reference `Notification` rows for delivery targets
  instead of defining their own channel entities. Workspace scoping is a **nullable
  `notification.workspace_id` column**, where `NULL` means global — i.e. the notification applies to
  every workspace. (It was previously the `workspace_notification` membership table, where the ABSENCE
  of a row meant global; that table was collapsed into the column, so "no row" became "null".) Writes go
  through `NotificationWorkspaceRepository` in `platform-notification-workspace`. The former EE
  `AiObservabilityNotificationChannel` table is GONE, along with its `workspace_*` relation table.
  There is no migration to run: the whole `platform-ai-observability` module is unreleased (absent
  from `master` and every release tag), so no database ever held a channel row. The observability init
  changelog therefore creates `ai_observability_alert_rule_channel` with a `notification_id` column
  FK'd straight to `notification(id)` — the conversion migration (`20260720000004`) that used to
  create, convert, then drop the channel tables was deleted rather than kept as a permanent no-op.
  That FK sits in its own changeset behind a `tableExists` precondition, because module-scoped test
  contexts load only a subset of changelogs and may not have `notification`.

- Webhook + Slack transports live in CE `server/libs/platform/platform-notification/platform-notification-delivery`:
  `WebhookNotificationClient` is THE single outbound-webhook transport (one `RestTemplate`, one Spring
  core `RetryTemplate`/`ExponentialBackOff` retry mechanism). Two entry points: `deliver(request[, retry])`
  for admin-configured notification webhooks — SSRF-validated via commons-util `UrlValidator`
  (loopback/private hosts rejected, so tests can't use a local HTTP server), standard
  `X-ByteChef-Event/Timestamp/Delivery` headers, optional HMAC
  `X-ByteChef-Signature: t=<ts>,v1=hex(HMAC-SHA256(secret, "<ts>.<body>"))` — and
  `deliverEvent(url, payload, retry)` for Atlas per-job callback webhooks (`Job.getWebhooks()`), which
  keeps the pre-existing contract: NO SSRF validation (authenticated API callers may target internal
  hosts) and message-converter payload serialization. Non-2xx / exhausted retries →
  `WebhookDeliveryException`. `SlackNotificationClient` (incoming-webhook transport) owns the
  `{"text": ...}` payload shape and delegates to the webhook client.
- Email: there is NO separate email transport — `MailService` (platform-mail, `@Async`, warn-skips when
  no mail host configured) is the single email path for everything, user-account mail and notification
  email alike. `EmailNotificationSender` and the EE
  `AiObservabilityNotificationDispatcher` both call `mailService.sendEmail(...)` — no inline
  `JavaMailSender` remains anywhere in notification delivery.
- Consumers: all three CE senders (`Email|Webhook|SlackNotificationSender`) live in
  platform-notification-delivery (so coordinator-app carries them). `EmailNotificationSender`
  reaches mail through the `NotificationEmailGateway` port (platform-notification-api):
  monolith/configuration-app bind it to MailService (`MailServiceNotificationEmailGateway`),
  coordinator/webhook apps bind it to `RemoteNotificationEmailGatewayClient` which proxies to
  configuration-app's `/remote/notification-email-gateway/send-email` — SMTP credentials stay in
  one app; no gateway bean at all = the EMAIL channel warn-skips. In the distributed deployment
  the coordinator resolves delivery targets through `configuration-app`'s
  `/remote/notification-service` read endpoints (platform-notification-remote-rest + the
  implemented `RemoteNotificationServiceClient` reads).
  `WebhookNotificationSender` (job-status webhook channel; settings keys `webhook` +
  optional `webhookSecret`, `@Async`), payload shaped by `JobStatusWebhookNotificationHandler` in
  platform-coordinator; platform-coordinator's `WebhookJobStatusApplicationEventListener` delegates the
  Atlas job-callback delivery to `deliverEvent` with the `Job.Retry` schedule (defaults: 5 attempts,
  2s initial interval, 2.0 multiplier); EE `AiObservabilityNotificationDispatcher` (post-migration: reads `Notification` rows,
  delivers via MailService + the shared clients; per-channel lastError bookkeeping is gone with the
  channel entity).
- The job-status trigger path is unchanged: `JobStatusApplicationEvent` → platform-coordinator
  `NotificationJobStatusApplicationEventListener` → sender/handler registries. Never add notification
  logic under `server/libs/atlas/` — the engine stays notification-agnostic (hard requirement).
- The listener warn-skips event/channel combos with no sender or handler (don't NPE the fan-out).
  JOB_CANCELLED fires when a job is stopped while still CREATED (never started) — `Job.Status.CANCELLED`
  is appended at the enum end (INT-ordinal persisted); STOPPED remains the mid-run interruption status.
- **Workflow alert rules (EE, Sim model)**: `server/ee/libs/automation/automation-workflow-alert` —
  workspace-scoped `workflow_alert_rule` rows (7 `WorkflowAlertRuleType`s, INT ordinal append-only)
  whose delivery targets are `Notification` ids (join table, FK CASCADE — rules own WHEN, the
  notification registry owns WHERE/HOW). Evaluation state lives ON the rule row (consecutive counter,
  tumbling-window counters, EWMA latency, lastActivity) — updated per terminal job event by
  `WorkflowAlertApplicationEventListener` (`@Order(200)`, after the cost listener's `@Order(100)` so
  COST_THRESHOLD sees the cost row); NO_ACTIVITY fires from a 5-min scheduled monitor; fixed cooldown
  (default 60 min). `WorkflowAlertDispatcher` delivers via MailService / WebhookNotificationClient
  (`workflow.alert` eventType) / SlackNotificationClient. Semantics pinned by
  `WorkflowAlertEvaluatorTest`.

### Workflow error handler

When an automation run ends `FAILED`, `ErrorWorkflowJobStatusApplicationEventListener`
(platform-coordinator, `@Order(300)`, after cost and workflow alerts) dispatches the configured
error workflow through `PrincipalJobFacade.createJob`. Config is a nullable
`project.error_project_workflow_id` (set via the `updateProjectErrorWorkflow` GraphQL mutation, and
in the client via the project header's Settings menu → Project tab → Error Workflow dialog) with a
per-workflow override + a separate `error_workflow_disabled` flag on `project_workflow` (null
already means inherit) — set together via `updateProjectWorkflowErrorWorkflow` (client: Settings
menu → Workflow tab → Error Handling dialog's three-state Inherit/Override/Disabled radio). The
handler must live in the same project and carry a `workflow/newWorkflowError` trigger; both are
validated when configured
(`ErrorWorkflowConfigurationValidator`), not at failure time. `errorHandlerFor` job metadata caps
recursion at depth 1 — a failing handler does not spawn another; a subflow child job is also
skipped (only the top-level failed run dispatches). Admission gates are deliberately not bypassed,
so a failure storm is bounded by plan limits, not deduped — N failures can produce up to N handler
runs. This layers on the `on-error` task dispatcher rather than competing with it: `on-error` is an
intra-workflow catch (a handled error ends the job `COMPLETED`), so an error workflow only fires on
a genuinely uncaught, inter-workflow failure. **Monolith only** — resolution needs
`ProjectWorkflowService` lookups, and `RemoteProjectWorkflowServiceClient` is all
`UnsupportedOperationException` stubs, so distributed EE can't resolve the handler at all (same
root cause as orphaned-job recovery); the listener detects this, logs once, and records the
`skipped_unsupported` outcome instead of warning on every failed job. The payload carries
`execution.autoRecoveryAttempts` rather than n8n's `retryOf`: ByteChef resumes a job IN PLACE
(`resumeToStatusStarted` reuses the same id), so there is no prior job to point at — what a handler
can use is how many times this run was already auto-recovered. Metric:
`bytechef_error_workflow_dispatch{outcome=dispatched|rejected|
skipped_recursion|skipped_subflow_child|skipped_no_config|skipped_unsupported|failed}`.

## Public URL Signing

- `/file-entries/{id}/content` is intentionally unauthenticated (serves webhook outputs to anonymous callers). As of the 2026-05-18 signing rollout, the preferred form is an HMAC-SHA256 signed token (`v1.<exp>.<payload>.<sig>`) minted via `FileEntryTokens.toSignedToken`. Legacy unsigned `FileEntry.toId()` IDs are still accepted while `bytechef.file-storage.signed-url.required=false` (default).
- **Use `FileEntry.toId()` for**: DB persistence, intra-process passing. No security claim, deterministic forever.
- **Use `FileEntryTokens.toSignedToken(fileEntry)` for**: anything that leaves the server as part of a URL (webhook response body, etc.). TTL applies.
- The signer lives in `file-storage-token-service` (not in `file-storage-api`, which stays interface-only). Consumers depend on `file-storage-api` for the `FileEntryTokens` interface and pull `file-storage-token-service` at runtime so the autoconfig fires.
- **Signing key resolution order**: (1) explicit `bytechef.file-storage.signed-url.secret` property — power-user override for independent key rotation; (2) `EncryptionKey` bean present (standard ByteChef setup) — derived automatically via `HMAC-SHA256(decode(encryptionKey), "bytechef-file-storage-signed-url-v1")`; (3) neither present — unconfigured mode (mint throws, verify accepts legacy only). In practice, signed URLs work out of the box on every deployment because `EncryptionKey` is always configured. Setting `bytechef.file-storage.signed-url.secret` explicitly is not required for normal deployments.
- The domain-separation label `"bytechef-file-storage-signed-url-v1"` ensures the derived signing key is mathematically independent from the AES master key (key-separation principle). The `-v1` suffix allows rolling forward to a new derivation scheme without rotating the encryption key.
- Spec: `docs/superpowers/specs/2026-05-18-hmac-signed-file-entry-tokens-design.md`. Plan: `docs/superpowers/plans/2026-05-18-hmac-signed-file-entry-tokens.md`.
