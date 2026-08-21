# Drop `JobSyncExecutor`: distributed-coordinator synchronous execution

- **Date:** 2026-06-19
- **Status:** Draft (design)
- **Author:** Ivica Cardic
- **Branch:** off `master`

## 1. Background

ByteChef has two ways to run a workflow job:

1. **Distributed (production) path** — `principalJobFacade.createJob(...)` →
   `jobFacade.createJob` publishes a `StartJobEvent` onto the message broker. The
   coordinator and worker fleet pick it up; tasks are dispatched over the broker
   and can run on any worker instance. Job/task state is persisted in the shared
   database and durable `TaskFileStorage`. This is how triggers and async webhooks
   execute, and it scales horizontally.

2. **Embedded synchronous path — `JobSyncExecutor`** — a *second, in-process copy*
   of the entire coordinator + worker + task-dispatcher + completion-handler stack,
   constructed inline (see `WebhookConfiguration.webhookExecutor`,
   `AutomationMcpServerConfiguration`, `EmbeddedMcpServerConfiguration`). It runs
   over an in-JVM `AsyncMessageBroker`, stores task outputs in
   `InMemoryTaskFileStorage`, and blocks the calling thread on a `CountDownLatch`
   (`waitForJobCompletion`, 300s timeout) until the job terminates.

There are **three** independent `JobSyncExecutor` instances today:

| Consumer | Module | Result collection |
|---|---|---|
| `WebhookWorkflowExecutorImpl.executeSync` | `platform-webhook-impl` | live `TaskExecutionCompleteEvent` callback (`collectWebhookResponse`) reads `WEBHOOK_RESPONSE`-tagged task output, stores into job outputs, returns JSON |
| `AutomationMcpToolFacade` | `automation-ai-mcp-server` | **no-op** callback; post-hoc `getCallableResponseOutput(job)` reads last task's `CALLABLE_RESPONSE` metadata, else job outputs |
| `EmbeddedMcpToolFacade` | `embedded-ai-mcp-server` | same post-hoc `CALLABLE_RESPONSE` read as automation |

Note the two MCP facades already collect their result **post-hoc** from the
completed `Job`; only the webhook path still relies on the live callback.

> **Scope correction (discovered during implementation).** `JobSyncExecutor` has
> **additional** consumers beyond these three: `TestWorkflowExecutorImpl` (the
> workflow-editor "Test" run — a production feature using the engine's rich
> `startJob`/`awaitJob`/`addSseStreamBridge` listener API) and the in-process component
> test harness (`ComponentJobTestExecutor`, `TaskDispatcherJobTestExecutor`), which
> deliberately want single-node deterministic execution. Therefore **`JobSyncExecutor`
> is retained**, not deleted. The scalability goal is met by removing it from the
> production webhook + MCP request paths (the high-volume, horizontally-scaled
> surfaces). Migrating the workflow-test feature off the engine, if desired, is a
> separate effort; the test harness should keep it.

### How the response is captured today

- The worker-side `WebhookResponseTaskExecutionPostOutputProcessor` only **tags**
  the task: `taskExecution.putMetadata(WEBHOOK_RESPONSE, true)`. The actual
  `WebhookResponse` value is the task's output, stored normally.
- `WebhookWorkflowExecutorImpl.collectWebhookResponse` reads that tagged task's
  output during the run via the `JobSyncExecutor` completion callback.
- `executeSync` then stores it into job outputs and returns it; the controller
  (`AbstractWebhookTriggerController.doProcessTrigger`) returns it as the JSON body
  (or via `processWebhookResponse` when it is a `WEBHOOK_RESPONSE` map).

### Completion signal in the distributed path

The real coordinator publishes `JobStatusApplicationEvent` on terminal status. In
`PlatformCoordinatorConfiguration`, `SseStreamApplicationEventListener` republishes
**every** such event as an `SseStreamEvent(EVENT_TYPE_JOB_STATUS)` onto the
`SSE_STREAM_EVENTS` broker route. `SseStreamBridgeRegistry.handleJobStatus`
completes a per-`jobId` `CompletableFuture<Void>` on `COMPLETED|FAILED|STOPPED`.
This is what makes the SSE streaming endpoint (`WebhookTriggerController:163`,
`stream` → `executeAsync(.., bridge)`) work — and crucially it works **across
processes** because `SSE_STREAM_EVENTS` is a real broker route in distributed mode.

## 2. Problem

`JobSyncExecutor` caps horizontal scalability:

- **Execution is pinned to the receiving node.** All tasks of a sync job run on
  that node's local `taskExecutor` thread pool over an in-JVM broker; the
  distributed worker fleet is never used. A burst of sync webhooks / MCP tool calls
  is bounded by one node's CPU and thread pool, regardless of fleet size.
- **It is a full duplicate engine.** Three inline copies of the coordinator/worker
  stack must be constructed and maintained (dispatchers, completion handlers,
  adapters, listeners), drifting from the production engine.
- **Thread blocking.** Each call blocks a thread on a `CountDownLatch` for the
  whole workflow duration.

## 3. Goals / non-goals

**Goals**

- Remove all three `JobSyncExecutor` instances and the class itself.
- Run synchronous workflow execution on the **distributed coordinator**
  (`createJob`), so it uses the worker fleet and scales horizontally.
- Preserve existing **caller contracts**: webhook sync returns the same JSON body;
  MCP tool calls return the same `CALLABLE_RESPONSE`/outputs.
- Convert the webhook controller to **async-servlet** (return
  `CompletableFuture<ResponseEntity<?>>`) so no servlet thread blocks while the
  distributed job runs. No client-visible change.

**Non-goals**

- No change to the SSE streaming endpoint behavior (it already uses the
  distributed path; it only benefits from a shared await primitive).
- No change to fire-and-forget async webhooks (`executeAsync`,
  `validateAndExecuteAsync`).
- No new response shape for any consumer (that was the rejected option B).

## 4. Design overview

Introduce one shared primitive — **await job completion fed by the broker signal**
— and rewrite each synchronous consumer as:

```
jobId = principalJobFacade.createJob(params, principalId, type)   // distributed
job    = jobCompletionAwaiter.await(jobId, timeout)               // non-blocking future
result = <consumer-specific post-hoc collection from the completed job>
```

Then delete `JobSyncExecutor`, the inline engine wiring in the three configs, and
the now-unused `createSyncJob` (verify no other callers — `TriggerErrorHandler`
uses it independently; see §6.6).

## 5. Detailed design

### 5.1 Shared completion-await primitive

**The completion signal must cross process boundaries**, so it must be fed by the
`SSE_STREAM_EVENTS` broker route (not the in-JVM `JobStatusApplicationEvent`).

Two viable shapes:

- **(Recommended) Generalize the existing registry.** Extract the
  completion-future half of `SseStreamBridgeRegistry` into a small, dependency-light
  `JobCompletionAwaiter` that subscribes to `SSE_STREAM_EVENTS` and exposes:

  ```java
  CompletableFuture<Job> await(long jobId, Duration timeout);
  ```

  It completes the future on `EVENT_TYPE_JOB_STATUS` terminal status (reusing the
  exact logic in `SseStreamBridgeRegistry.handleJobStatus`), loading the `Job` via
  `jobService` before completing. `SseStreamBridgeRegistry` can then delegate its
  completion-future bookkeeping to it (or they share the listener).

- (Alternative) Leave the registry as-is and have each consumer register a
  throwaway no-op `SseStreamBridge` purely to obtain `Registration.completion()`.
  Simpler diff, but leaks the SSE/bridge concept into MCP modules that have nothing
  to do with SSE. Rejected for clarity.

**Module placement.** `SseStreamEvent` and `SseStreamMessageRoute` live in
`platform-webhook-api`; the broker listener config in `platform-webhook-impl`. The
MCP server modules (`automation-ai-mcp-server`, `embedded-ai-mcp-server`) must reach
the awaiter, and they should not depend on `platform-webhook-impl`. Options:

- Move the await primitive (and the job-status broker listener) into a neutral
  platform module both already depend on — candidate:
  `platform-workflow-execution-service` (alongside `PrincipalJobFacade`), or a new
  `platform-job-sync`-replacement module. **Decision needed** (see open question
  OQ-1). The `SSE_STREAM_EVENTS` route constant can stay in `platform-webhook-api`
  and be referenced, or be promoted to a platform route module.

**Race guard (critical for fast jobs).** `createJob` dispatches asynchronously; a
short job can reach terminal status before `await(jobId, ...)` registers its future,
in which case the job-status broker event arrives with no registered future and the
fresh future never completes. The awaiter MUST, immediately after registering the
future, re-fetch the job (`jobService.fetchJob(jobId)`) and complete the future at
once if the job is already terminal. (This mirrors
`JobSyncExecutor.waitForJobCompletion`'s initial status check.)

**Timeout.** `await` arms a timeout (default 300s, matching today's
`JobSyncExecutor` timeout) and completes the future exceptionally with a
`TimeoutException`-typed failure on expiry. Callers translate it to their existing
error surface.

### 5.2 Webhook `executeSync` rewrite

`WebhookWorkflowExecutorImpl`:

- Remove the `JobSyncExecutor` field and `executeSyncJob`. Remove
  `collectWebhookResponse` as a live callback; replace with a post-hoc reader.
- New flow per trigger-output value:
  1. `long jobId = principalJobFacade.createJob(createJobParameters(...), principalId, type)`.
  2. `CompletableFuture<Job> future = jobCompletionAwaiter.await(jobId, timeout)`.
  3. `future.thenApply(job -> collectWebhookResponse(job))` — read the
     `WEBHOOK_RESPONSE`-tagged task output from **durable** `TaskFileStorage`
     (`durableTaskFileStorage`, not the in-memory wrapper), store into job outputs
     as today, and read back the outputs map.
- **Post-hoc collection (last-to-complete wins).** Replace the event-driven
  `collectWebhookResponse` with a scan of `taskExecutionService.getJobTaskExecutions(jobId)`.
  A run may execute multiple `WebhookResponse` actions — usually sequentially, but
  possibly in racing parallel branches (uncommon but valid). Today's semantic is
  *last-to-complete wins* (the `AtomicReference.set` callback fires in completion
  order). Reproduce it exactly by selecting the `WEBHOOK_RESPONSE`-tagged task with
  the **latest `endDate`**, tie-broken by `getId` (the tiebreaker also makes the
  same-instant parallel race deterministic, which it is not today). Do **not** use
  created-date order or first-match — those diverge from current behavior on a
  parallel race. Single-response and sequential-multi cases are unaffected.
- **Batch case** (collection trigger output → N jobs): create N jobs, await all via
  `CompletableFuture.allOf(...)`, collect each, return the `List`. Same external
  result as today.
- **Return type:** `executeSync` becomes
  `CompletableFuture<@Nullable Object> executeSync(...)` so the controller can stay
  non-blocking end to end. (Interface change in
  `WebhookWorkflowExecutor`; update the `WebhookBridgeAgent` caller — it currently
  consumes the value synchronously, so it can `.join()`/compose as appropriate.)

### 5.3 Async-servlet controller conversion

- `AbstractWebhookTriggerController.doProcessTrigger` returns
  `CompletableFuture<ResponseEntity<Object>>`. The `workflowSyncExecution()` branch
  composes on the `executeSync` future (`.thenApply` → `processWebhookResponse` /
  `ResponseEntity.ok`). Other branches wrap their already-computed
  `ResponseEntity` in `CompletableFuture.completedFuture(...)`.
- `WebhookTriggerController.executeWorkflow` returns
  `CompletableFuture<ResponseEntity<?>>` (Spring MVC async support releases the
  servlet container thread while the future is pending). Preserve the
  `TenantContext.callWithTenantId` wrapping; ensure tenant context is captured into
  the async continuation (the broker listener already restores tenant id from
  `CURRENT_TENANT_ID` metadata, but the collection `.thenApply` runs on a
  completion thread and must re-establish `TenantContext` — wrap the continuation).
- The SSE endpoint (`sseStreamWorkflow`) is unchanged.

### 5.4 MCP facades rewrite

Both `AutomationMcpToolFacade` and `EmbeddedMcpToolFacade`:

- Replace `jobSyncExecutor.execute(params, factory, true, noop)` with
  `createJob` + `jobCompletionAwaiter.await(jobId, timeout).join()` (these are tool
  calls invoked from agent worker threads, not servlet threads — blocking `.join()`
  is acceptable here, or expose an async variant if the MCP server supports it).
- Keep `getCallableResponseOutput(job)` and the job-outputs fallback unchanged —
  they already operate post-hoc on the completed `Job`.
- Drop the `JobSyncExecutor` construction from `AutomationMcpServerConfiguration`
  and `EmbeddedMcpServerConfiguration`; inject the shared awaiter instead.

### 5.5 Error handling (replacing `checkForError`)

`JobSyncExecutor.checkForError` threw `ExecutionException` on a failed
job/last-task. The awaiter returns the completed `Job`; each caller must reproduce
the check:

- A shared helper `checkForError(Job)` (move the logic out of `JobSyncExecutor`
  into a static util in the new module) so webhook + MCP behave identically.
- `FAILED` job or `FAILED` last task → throw `ExecutionException` with the task/job
  error message and the existing `TaskExecutionErrorType.TASK_EXECUTION_FAILED` /
  `JobErrorType.JOB_FAILED`. In async-servlet form this becomes an exceptionally
  completed future the controller's error handling renders as today.
- Timeout → mark the job FAILED (as today) and surface a timeout error.

### 5.6 Removal & cleanup

- Delete `JobSyncExecutor`, `JobServiceWrapper`, the `platform-job-sync`
  `SseStreamTaskExecutionPostOutputProcessor` copy, `WebSocketEmitterRegistry`
  usages that exist only for the embedded engine, and the inline stack builders in
  the three configs (`getTaskCompletionHandlerFactories`,
  `getTaskDispatcherResolverFactories`, `getTaskDispatcherAdapterFactories`,
  `getAdditionalApplicationEventListeners`, the `AsyncMessageBroker` +
  `InMemoryTaskFileStorage` wiring).
- `createSyncJob`: after the three consumers move off it, `TriggerErrorHandler`
  remains the only caller. Confirm it still needs a directly-created job row (it
  creates a job purely to attach trigger-error state, not to run it). If so, keep
  `createSyncJob` but consider renaming to reflect "create job row without
  dispatch." Otherwise remove it too. **Decision needed (OQ-2).**
- Remove the now-stale `executeSyncJob` Javadoc about bridge registration.

## 6. Edge cases & risks

- **Suspend/resume (RESOLVED — no special handling needed).** A suspending
  workflow does *not* stay non-terminal: `SuspendTaskCompletionHandler` calls
  `jobService.setStatusToStopped(jobId)` and publishes
  `JobStatusApplicationEvent(STOPPED)` (lines 133–135). **STOPPED is a terminal
  status**, and both the embedded `JobSyncExecutor` and
  `SseStreamBridgeRegistry.handleJobStatus` already treat COMPLETED/FAILED/STOPPED
  as terminal — so today's sync webhook already returns on suspend (it does *not*
  hang), and the broker-fed awaiter behaves identically. The `STOPPED` event
  republishes onto `SSE_STREAM_EVENTS`, reaching the awaiting node. Resume continues
  through the distributed `JobResumeFacade`, unchanged. (The embedded engine
  actually wired `new SuspendTaskExecutionPostOutputProcessor(null)` — a null
  `triggerScheduler` — so it could not schedule timed resume at all; the
  distributed path is strictly more correct here.) **Requirement:** the awaiter must
  treat STOPPED as terminal (it does), and `checkForError` must NOT error on STOPPED
  (today it only throws on FAILED — preserved).
- **Streaming tasks inside a sync (JSON) job.** With option A the response is JSON,
  so per-token SSE chunks are simply not consumed (no bridge registered) — same as
  not registering a bridge today. The job still completes normally. Acceptable.
- **Cross-process completion delivery.** Depends entirely on `SSE_STREAM_EVENTS`
  reaching the awaiting node. Verified that `SseStreamApplicationEventListener` is
  registered in `PlatformCoordinatorConfiguration` and publishes for every job.
  Must confirm the broker route is configured in all deployment topologies
  (monolith, microservices) — in monolith it is in-memory but same-process; in
  microservices it is the real broker. **Verify in testing.**
- **Tenant context across the async continuation.** The collection `.thenApply` and
  the controller's future composition run on completion threads; tenant id must be
  restored (capture at request time, re-establish in the continuation).
- **Durable vs in-memory storage.** Collection must read durable `TaskFileStorage`;
  the webhook executor currently holds the `InMemoryTaskFileStorage` wrapper — fix
  the injected instance for reads.
- **Future leak / cleanup.** The awaiter must invalidate per-jobId futures on
  completion/timeout (Caffeine TTL as `SseStreamBridgeRegistry` already does).

## 7. Open questions

- ~~**OQ-1:** Which module hosts the shared `JobCompletionAwaiter`?~~ **RESOLVED**
  — `platform-workflow-execution`: interface in `-api`, impl + broker job-status
  listener in `-service` (next to `PrincipalJobFacade`); reference
  `SseStreamEvent`/`SseStreamMessageRoute` from `platform-webhook-api`. All three
  consumers already depend on `platform-workflow-execution`, so none needs
  `platform-webhook-impl`. Verify no `-api ← -service` dependency cycle is
  introduced by the `platform-webhook-api` reference (webhook-api depends on
  workflow-execution-*api*, so service→webhook-api should be acyclic).
- ~~**OQ-2:** Does `TriggerErrorHandler` still need `createSyncJob`?~~ **RESOLVED**
  — `TriggerErrorHandler.createFailedJob` uses it to insert an undispatched job row
  and immediately mark it `FAILED` (it never runs the job). Keep the method, but
  **rename** it to express intent (e.g. `createJobWithoutDispatch`) since "sync"
  no longer means anything once the embedded engine is gone.
- ~~**OQ-3:** Required behavior for suspendable sync workflows on the distributed
  path.~~ **RESOLVED** — suspend maps to terminal `STOPPED`; the awaiter completes
  rather than hanging, matching current behavior. See §6.

## 8. Testing strategy

- Unit: awaiter race guard (job terminal before/after registration), timeout,
  failed-job error translation, batch await.
- Integration (`*IntTest`, Testcontainers): sync webhook end to end on the
  distributed coordinator — success, `WEBHOOK_RESPONSE` body, failed job, batch
  trigger output, and (if in scope) a suspend/resume workflow. Verify against the
  real broker that completion crosses the listener.
- Regression: existing `WebhookBridgeAgentTest`, MCP tool-call tests, and the SSE
  streaming endpoint must stay green.

## 9. Phasing

1. **Phase 1 — shared awaiter.** Add `JobCompletionAwaiter` (+ move
   `checkForError`) in the chosen module; unit-test it. No consumer changes yet.
2. **Phase 2 — webhook slice.** Rewrite `executeSync` + async-servlet controller;
   update `WebhookBridgeAgent`. Delete the webhook `JobSyncExecutor` wiring.
3. **Phase 3 — MCP facades.** Move both onto the awaiter; delete their
   `JobSyncExecutor` wiring.
4. **Phase 4 — cleanup (revised).** `JobSyncExecutor` is **retained** (still used by
   `TestWorkflowExecutorImpl` + the component/task-dispatcher test harnesses — see the
   scope-correction note in §1). The only Phase 4 action taken is renaming
   `createSyncJob` → `createJobWithoutDispatch` (OQ-2): now that sync execution runs on
   the distributed coordinator, its sole caller is `TriggerErrorHandler`, which uses it
   to insert an undispatched failed-job row, so the old name is misleading. Deleting the
   engine is out of scope.

Each phase is independently shippable and leaves the build green.
