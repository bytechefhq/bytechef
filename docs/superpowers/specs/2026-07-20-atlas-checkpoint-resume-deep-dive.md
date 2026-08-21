# Atlas checkpoint-resume: technical deep dive

Date: 2026-07-20. Status: analysis + proposed design. Question: "kill the worker mid-agent-loop,
show it resume — is that already supported via checkpoints where each node saves its state?"

**Short answer: half of it.** Atlas already checkpoints **at task granularity** — every completed
node's output and the accumulated context are durably persisted, and a resume path re-dispatches
from exactly that persisted state. What is NOT supported today: (a) resuming a job that a **crash**
left in `STARTED` (resume only accepts `STOPPED`/`FAILED`), (b) any intra-task state for a task
that was mid-flight — in particular the AI agent's tool loop, which runs entirely inside one
blocking call, and (c) redelivery of the in-flight task message on two of the broker backends.
The demo as stated (kill worker mid-agent-loop → resume) fails today; the pieces to make it pass
are small and listed in §5.

## 1. What IS checkpointed today (the per-node state save exists)

Per task, in order (all durable, PostgreSQL + file storage):

1. **Before dispatch** — `JobExecutor.executeNextTask` inserts the `TaskExecution` row and pushes
   a per-task `Context` snapshot (`context` table → `FileEntry`), *then* sends the
   `TaskExecutionEvent` to the worker route.
2. **On start** — the coordinator (not the worker) flips the row to `STARTED` via
   `TaskStartedApplicationEventListener`.
3. **On completion** — the worker stores the output through `TaskFileStorage` (durable), and
   `DefaultTaskCompletionHandler` updates the row to `COMPLETED`, **pushes the merged job
   context** (`name → output` accumulated), advances `job.currentTask`, persists the job, and
   dispatches the next task.

So after every node the workflow's full variable state is on disk — this is a real checkpoint
stream, and it is what the existing resume machinery replays from:

- **`ResumeJobEvent`** (`JobFacadeImpl.resumeJob`, Quartz `OneTimeSchedulerJob`, approval/webhook
  `JobResumeFacade`): `jobService.resumeToStatusStarted` (accepts `STOPPED`/`FAILED` only), then
  either re-enters `JobExecutor.execute` — re-dispatching the task at the persisted
  `job.currentTask` against the persisted context — or re-dispatches a specific persisted
  `TaskExecution` (the suspended-task path).
- **Suspend/approval** (cooperative pause): `SuspendTaskCompletionHandler` persists the
  `Suspend` state via `TaskStateService`, stamps `JOB_RESUME_ID`/`TASK_EXECUTION_RESUME_ID` into
  job metadata, sets the job `STOPPED`; `SuspendTaskDispatcherPreSendProcessor` rehydrates it on
  resume. The AI agent participates: `SuspendableToolCallingManager` serializes the **entire
  in-flight conversation** (`ConversationState`) into the suspend's continue-parameters when a
  tool calls `context.suspend(...)`, and `ConversationResume` patches the pending tool response
  and re-runs the model call from the reconstructed conversation.

That last part matters: **serializing and resuming a mid-loop agent conversation already works** —
but only at explicit, cooperative suspend points, not on crash.

## 2. What happens on a real crash today (the gap)

| Failure | Behavior today |
|---|---|
| Worker JVM dies mid-task, broker = **memory** (default monolith) | In-flight message lost (fire-and-forget, same JVM anyway). Task stays `STARTED` forever, job wedged. |
| Worker dies mid-task, broker = **redis** | Message lands in the stream's pending list but is **never reclaimed** (`RedisListenerEndpointRegistrar` reads `lastConsumed` only; no `XAUTOCLAIM`). Wedged. |
| Worker dies mid-task, broker = **amqp** | Unacked message is **redelivered** (ack-on-return, durable queues) → task re-runs from scratch. Works, but re-executes side effects (no idempotency), and a thrown exception (vs crash) dead-letters without reprocessing. |
| Worker dies mid-task, broker = **kafka** | Uncommitted offset → reprocessed on rebalance; same re-run-from-scratch caveats. |
| Coordinator dies between completion event and next dispatch | The completion handler's update-task → push-context → advance-job → dispatch-next sequence is **not transactional** (`// TODO @Transactional`); a crash can leave the job advanced-but-undispatched. Nothing detects it. |
| Agent tool loop mid-iteration | The whole LLM ↔ tools loop runs inside one blocking `ChatClient.call()`; conversation and completed tool results exist **only in JVM memory**. Chat-memory advisors write the turn only after it completes. On crash: nothing to resume; on redelivery: the turn restarts, re-invoking already-run tools. |
| Detection | No reaper, heartbeat, watchdog, or startup hook anywhere marks orphaned `STARTED` rows. `grep -ri checkpoint` over the server source: zero hits. The only timeout is worker-local (24h `future.get`) and dies with the JVM. |

## 3. Honest scoreboard

- Per-node state save: **exists** (context push per completed task).
- Resume from last completed node: **exists**, but only for `STOPPED`/`FAILED` jobs — a crash
  leaves `STARTED`, which no path accepts.
- In-flight message durability: broker-dependent (amqp/kafka at-least-once; memory/redis
  effectively at-most-once).
- Intra-task (agent-loop) checkpointing: **exists only cooperatively** (suspend); not continuous.
- Orphan detection: **does not exist**.

## 4. Proposed design: make the crash demo real

> **Status 2026-07-21:** steps 1 and 3 are IMPLEMENTED (worker heartbeats via
> `TaskHeartbeatApplicationEvent` + `OrphanedJobRecoveryMonitor` in platform-coordinator;
> recovery marks orphans FAILED — resumable via the existing path — with opt-in auto-resume
> capped by `max-auto-resume-attempts`). Detection requires the job row AND every non-terminal
> task to be stale, so children's heartbeats keep control-flow parents alive. Step 2
> (transactional completion) is IMPLEMENTED (2026-07-21): `DefaultTaskCompletionHandler` takes an
> optional `TransactionTemplate` (wired from the coordinator config's
> `ObjectProvider<PlatformTransactionManager>`) and wraps update-task + push-context + advance-job
> (+ next-task create) in one transaction; message publications inside are `MessageEvent`s that
> `MessageEventListener` delivers AFTER_COMMIT (`fallbackExecution = true` keeps
> transaction-less contexts working), so nothing is dispatched for uncommitted state.
> `JobSyncExecutor`'s in-memory path passes null and keeps its pre-transactional behavior.
> Semantics pinned by `DefaultTaskCompletionHandlerTest`. Step 4 is
> IMPLEMENTED: `SuspendableToolCallingManager` invokes a checkpointer after every completed
> non-suspend tool round, writing `AiAgentConversationCheckpoint` (input-parameter fingerprint +
> `ConversationState`) to CURRENT_EXECUTION-scoped data storage; on a crash-resumed job,
> `AiAgentChatAction.perform` restores the conversation (fingerprint-matched) and continues with
> a fresh model call over the reconstructed history, clearing the checkpoint on success. Editor
> and job-less runs skip checkpointing entirely; all checkpoint I/O is fail-open. Step 5 (redis
> pending-reclaim) is IMPLEMENTED (2026-07-21): `RedisListenerEndpointRegistrar` runs an
> XPENDING+XCLAIM sweep every 10s (min idle 60s, batch 100) inside its poll loop, redelivering
> entries left unacknowledged by a crashed consumer through the normal invoke-then-ack path —
> redis now has amqp-like at-least-once redelivery. Pinned by
> `RedisListenerEndpointRegistrarTest`. All five steps of §4 are now implemented. The agentic AI
> component got the step-4 treatment too (2026-07-21): produced blackboard bindings are
> checkpointed to CURRENT_EXECUTION data storage after every completed GOAP action and reseeded
> on a crash-resumed job, so the planner continues from the last completed action instead of
> re-running the whole plan.

The differentiator claim is legitimate — the persistence spine is already there; what is missing
is detection + permission to resume + one incremental-persistence hook. In dependency order:

1. **Orphan reaper (platform-coordinator, CE)** — a scheduled monitor (out of `server/libs/atlas/`,
   same placement discipline as notifications/enforcement): find `TaskExecution` rows `STARTED`
   with `lastModifiedDate` older than the task's timeout (default worker heartbeat interval × N),
   plus `STARTED` jobs with no live task. Action per policy: mark task `FAILED` +
   job `FAILED` (making the existing `resumeToStatusStarted` path immediately usable), or
   auto-resume (step 3). A worker heartbeat column (`task_execution.heartbeat_date`, touched by a
   worker-local ticker) makes detection sharp; `lastModifiedDate` is the no-migration fallback.
2. **Transactional completion** — wrap `DefaultTaskCompletionHandler.handle`'s
   update+push+advance in one transaction (the `// TODO @Transactional` is already in the code),
   and dispatch after commit (the `MessageEventListener` AFTER_COMMIT bridge already exists for
   exactly this pattern). Closes the coordinator-crash half-advanced window.
3. **Crash-resume policy** — allow `resumeToStatusStarted` from reaper-confirmed orphaned state
   (explicit new transition, not a blanket `STARTED→STARTED`), re-dispatching the persisted
   `TaskExecution` exactly as `ResumeJobEvent(taskExecutionId)` does today. Tasks re-run from the
   last completed node — correct for idempotent tasks; document the at-least-once contract.
4. **Agent-loop checkpoints (the differentiator)** — the machinery already exists for suspend;
   make it continuous: in `SuspendableToolCallingManager`, after **each tool-execution round**,
   persist `ConversationState.from(conversation)` via `TaskStateService` keyed by the task
   execution id (same store the suspend path uses), and delete it on turn completion. On
   crash-resume, `AiAgentChatTaskHandler` checks for a stored conversation state and enters
   `resumeChat` (the `ConversationResume` path) instead of starting fresh — replaying from the
   last completed tool call instead of re-running the whole loop. Cost: one small row write per
   tool round; no new tables. This is exactly Koog's "continuous persistence per node" shape
   (see the Embabel/Koog analysis doc), implemented with parts we already have.
5. **Redis broker pending-reclaim** (optional hardening) — add `XAUTOCLAIM` on an interval in
   `RedisListenerEndpointRegistrar` so redis gains amqp-like redelivery.

Steps 1–3 make "kill the worker, job resumes from the last completed node" true on every broker.
Step 4 makes "kill the worker **mid-agent-loop**, the agent resumes mid-conversation" true —
that is the demo headline.

## 5. Live demo script (target state; today only the suspend variant passes)

Environment: monolith + amqp (or the EE coordinator-app/worker-app pair), PostgreSQL, RabbitMQ
(`server/docker-compose.dev.infra.yml` provides both).

```bash
# 1. Infra + app (broker=amqp so in-flight messages survive)
cd server && docker compose -f docker-compose.dev.infra.yml up -d && cd ..
BYTECHEF_MESSAGE_BROKER_PROVIDER=amqp ./gradlew -p server/apps/server-app bootRun &

# 2. Create a workflow: manual trigger → AI Agent (with 2-3 tools, e.g. a slow HTTP tool)
#    → a post-processing task. Run it from the editor (or POST /api/.../workflows/{id}/tests).

# 3. While the agent is mid-loop (editor shows the node blue/spinning — watch the
#    task_started SSE events), kill the JVM hard:
kill -9 $(pgrep -f server-app)     # (distributed: kill only worker-app)

# 4. Observe persisted state:
#    - task_execution row for the agent node: status STARTED, context rows present
#    - with step 4 implemented: task_state row holding the ConversationState checkpoint

# 5. Restart. Target behavior:
#    - reaper detects the orphaned STARTED task (step 1)
#    - job resumes (step 3); agent node rehydrates ConversationState and continues the
#      conversation after the last completed tool call (step 4)
#    - editor re-attaches via /workflow-tests/{jobId}/attach and shows the node running again

# What you can demo TODAY without any changes: the cooperative variant — an agent tool that
# calls context.suspend() (human-in-the-loop), server restart while STOPPED, then resume via
# the signed approval link: the conversation continues exactly where it paused. Same
# machinery, crash-triggering it is what §4 adds.
```

## 6. File/line anchors

Coordinator orchestration `TaskCoordinator.java:122-246`; dispatch + context
`JobExecutor.java:81-106`; completion `DefaultTaskCompletionHandler.java:89-170`; worker
`TaskWorker.java:110-310`; status transitions `JobServiceImpl.java:142-202`; suspend stack
`SuspendTaskCompletionHandler` / `SuspendTaskDispatcherPreSendProcessor` / `TaskStateService`;
agent loop `AbstractAiAgentChatAction.java:290-336`, `SuspendableToolCallingManager.java:62-105`,
`ConversationState`, `ConversationResume`; brokers `AsyncMessageBroker`,
`AmqpMessageBrokerListenerRegistrarConfiguration.java:114-175`,
`RedisListenerEndpointRegistrar.java:117-147`; resume surface `JobFacadeImpl.java:149-154`,
`JobResumeFacadeImpl.java:71-118`, `OneTimeSchedulerJob.java:39`.
