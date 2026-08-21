# Agent run Hub visibility — implementation plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or
> superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax.

**Goal:** Make a conversation that reaches an AI Agent through a channel (Slack, schedule, …) readable in
the AI Hub chats list, by creating an `ai_hub_chat` row that points at the session the agent already
writes.

**Architecture:** Nothing in the agent runtime changes. The agent's builtin chat-memory element already
persists the conversation to the Hub's session store under session id = `conversationId`, and the Hub
already reads a transcript by thread id (`AiHubChatServiceImpl:641,841` →
`sessionRepository().getEvents(threadId)`). The only missing piece is a metadata row. A CE port invoked at
the end of an agent turn asks an optional EE implementation to find-or-create that row.

**Spec:** `docs/superpowers/specs/2026-08-17-agent-run-hub-visibility-design.md`

> **EXECUTED** — 18 commits, `5267ae3bb69..d0bff3e98bc`, every task reviewed, final whole-branch review
> passed with two fixes applied. Deviations worth knowing, all decided in review and recorded in the
> ledger: the payload field is `creatorUserId` (not `ownerUserId` — it comes from `AiAgent.createdBy`, an
> immutable `@CreatedBy` audit field) and gained `workflowId`/`environmentId` so the Hub can authenticate
> the stamp without depending on `automation-ai-agent`; Task 1 ships no index and edits the init changelog
> (the table proved UNRELEASED); the recorder runs on `boundedElastic`, not the reactor thread. The one
> step NOT done is the smoke run — see Task 7.

## Sequencing against the "Scheduled" deletion — read this first

`docs/superpowers/plans/2026-08-17-delegate-scheduled-to-ai-agent.md` deletes the AI Hub task subsystem:
the `ai_hub_task*` tables, the `ai_hub_chat.ai_hub_task_id` column, and **the `TASK` chat kind**, moving
scheduled runs onto the AI Agent's `schedule` channel. It adds no replacement chat surface. **This plan is
that replacement.**

- **That work is already underway** (client-side schedule blocks are landing: `babdead4e12`,
  `3692f37941e`), following its "build first, delete second" order. As of this plan the deletion has
  **not** happened — `AiHubChatKind.TASK` and the `ee.ai.hub.task` package are both still present — so
  there is still time to land this first. **Land this plan before, or in the same release as, that one.**
  Landing that plan alone leaves a window in which a scheduled agent run produces no chat at all — the
  regression its own spec warns about.
- **The reference implementation is captured in the appendix below**, read from this worktree before
  0_732 deletes it. Read that section rather than hunting through git history.
- **Two behaviours of the deleted listener have no successor in this plan.** It calls
  `scheduleService.recordFire()` / `recordFailure()` and publishes an
  `AiHubAuditEvent.AI_HUB_TASK_SCHEDULE_FIRED` audit event with the chat id. ⚑ Decide explicitly whether
  agent schedules need equivalents — the workflow execution record covers "did it run", but the audit
  trail does not survive on its own. Raise it rather than silently dropping it.
- **Enum ordinals:** that plan renumbers `AiHubChatKind` to `STANDARD(0), WORKFLOW_CHAT(1),
  AGENT_CHAT(2)` and updates `EnumOrdinalStabilityTest`. This plan does not touch the enum — it only
  creates rows of the existing `AGENT_CHAT` kind — so the two are compatible in either order, but
  whichever lands second must not re-pin the ordinals.
- **Changelog collision:** that plan *deletes* `20260504000002_ai_hub_chat_add_task_id.xml` while Task 1
  here *adds* a changeset to the same table. Check the merged changelog order after both land, and
  re-run an ai-hub `*IntTest` (Testcontainers builds from scratch, which catches ordering breakage that a
  running dev DB hides).

## Global Constraints

- **Dependency direction is the hard rule.** The AI Agent must never reference an AI Hub implementation —
  only a CE SPI. And the Hub must not gain a dependency on `automation-ai-agent` either (it has none
  today: no `automation-ai-agent` entry in any `server/ee/libs/ai/ai-hub/*/build.gradle.kts`). Both are
  satisfied by making the port's payload **self-contained**: it carries `aiAgentId` and an
  already-resolved `creatorUserId`, so the Hub persists what it is handed and resolves nothing. If any task
  finds itself adding either dependency, the payload is missing a field — fix the payload, not the
  build file.
- **No change to memory or session handling**: not `ChatMemory.java`, not `buildChatMemoryElement`, not
  the session user ids. Three earlier spec drafts proposed one; all were unnecessary. A diff touching
  `chat-memory-builtin`, or touching `AiAgentWorkflowGenerator` for any reason other than Task 3's
  identity stamp, means the plan has gone wrong.
- **Fail-open**: a recorder failure must never fail an agent turn (same posture as auto-memory and
  agent-loop checkpoints).
- **Chat memory off ⇒ do nothing.** No element ⇒ no session ⇒ no row. Not an error path; the agent's runs
  stay visible as workflow executions.
- CE must not import EE. The port lives in a CE module; the Hub implements it.
- Commit convention: server `732 <description>`, client `732 client - <description>`; never amend on
  `0_732`. `./gradlew spotlessApply` before every commit. Never judge a Gradle run through a pipe —
  redirect to a file, `echo $?`, grep `^> Task .* FAILED`.
- EE code carries the ByteChef Enterprise licence header and a `@version ee` Javadoc tag.

---

### Task 1: Schema — `ai_agent_id` on `ai_hub_chat`

Two agents reachable on the same Slack channel share a `conversationId`; they must not share a chat.

- Add a **nullable** `ai_agent_id BIGINT` column to `ai_hub_chat` (nullable because composer-created
  chats have no agent id, and per the workspace-scoping convention: nullable column, `Long` field, never
  primitive).
- **No index.** An earlier draft of this task added a partial unique index on
  `(workspace_id, ai_agent_id, thread_id)`. It was removed in review: `ai_hub_chat.thread_id` already
  carries a GLOBAL UNIQUE constraint (see `AiHubChatServiceImpl.create(...)` and its comment), so that
  index can never be violated independently. `ai_agent_id` is an **attribution** column, not a uniqueness
  scope — two agents sharing one Slack channel share one `conversationId`, therefore one session, one
  transcript and one chat row.
- **Edit the init changelog directly.** Verified during execution with CLAUDE.md's prescribed test:
  `ai_hub_chat` arrived in `51f5cb89c26` (2026-08-13) and `git merge-base --is-ancestor 51f5cb89c26 v0.31.3`
  is false, as is the same check against `master` — so the table is UNRELEASED and the rule is "edit init
  directly", not "add a new changeset". Note this breaks local dev DBs two ways (schema drift + stale
  md5sums); `scripts/dev/sync-local-schema-after-collapse.sh` repairs both, idempotently.
- Field on `AiHubChat` + getter/setter; include in `equals`/`hashCode` only if the class already includes
  comparable fields.
- [x] Verify with an existing `*IntTest` in the ai-hub module (Testcontainers builds the schema from
  scratch, which is stronger evidence than `bootRun`).

```bash
git commit -m "732 Add ai_agent_id to ai_hub_chat"
```

---

### Task 2: CE port `AgentConversationRecorder`

**Files:** `platform-ai-api`, package `com.bytechef.platform.ai.conversation` — chosen and reviewed in
execution. The two same-shape CE seams the agent component already wires live in DIFFERENT modules:
`AiGuardrailsAdvisorProvider` in `platform-ai-api`, `ToolExecutionRecorder` in
`platform-tool-execution-api`. This is a conversation-reporting seam like the former, not execution
telemetry like the latter, and `platform-ai-api` is already on the agent component's classpath.

```java
public interface AgentConversationRecorder {
    void recordTurn(AgentConversation agentConversation);

    /**
     * Self-contained by design: it carries the resolved owner so the Hub never looks an agent up, which
     * is what keeps ai-hub free of a dependency on automation-ai-agent (see Global Constraints).
     */
    record AgentConversation(
        long workspaceId, long aiAgentId, long creatorUserId, String conversationId,
        @Nullable String channelType, @Nullable String title) {}
}
```

- Place it beside the existing CE SPI seams the agent component already consumes —
  `AiGuardrailsAdvisorProvider` lives in `platform-ai-api` (verified), `ToolExecutionRecorder` in
  `platform-tool-execution-api`. Follow whichever of the two the agent action already wires.
- Optional bean, resolved via `ObjectProvider`; absent on CE ⇒ no-op, exactly like the guardrails
  provider seam. The AI Agent never names an AI Hub type.
- **`channelType` is nullable by ruling.** The agent chat action receives the agent's prompt, not the
  `branch_in` envelope that carries `channel`, and nothing verified says it can reach it. Task 3's
  research step decides whether to supply it; when absent, Task 6 labels the row generically. Do **not**
  plumb the channel through the generator to fill it — that is a generator change the Global Constraints
  forbid for anything but the identity stamp.
- [x] **Failing test first:** a test proving the agent chat action completes normally with **no** recorder
  bean present.

```bash
git commit -m "732 Add agent conversation recorder port"
```

---

### Task 3: Invoke the port from the agent chat action

**Where:** `AbstractAiAgentChatAction`, at the end of a completed turn — **not** a job-status listener,
which would have to recover the turn text from node output and would double-report for subflow children.

- **The component must not resolve the agent's identity — it is stamped for it.** A component module
  cannot depend on `automation-ai-agent`, so `AiAgentWorkflowGenerator` writes `aiAgentId` and
  `creatorUserId` into the `aiAgent_1` node when it generates the workflow, and the action forwards them
  as opaque values. This is the one sanctioned generator change in this plan (Global Constraints).
  `creatorUserId` is resolved there from `AiAgent.createdBy` — an auditing **username string**, not a user
  id — which is also the only place that knows how.
  - ⚑ Consequence: the stamp is frozen at generation. Every save regenerates the definition, so it
    refreshes on the next publish; a creator deleted in between leaves a row owned by a missing user
    until then. Accepted — the alternative is a Hub→agent dependency this plan forbids.
- [x] **Research step, do this first and record the answer in the commit message:** confirm the action can
  read those stamped values plus `workspaceId` and the channel type at turn completion. `conversationId`
  is already resolved at `AbstractAiAgentChatAction:213-227`; `ActionContextAware` carries `environmentId`
  and job context. If the stamp cannot be read there, **stop and report** rather than inventing a lookup —
  an agent id guessed from the workflow id would be wrong for sub-agents.
- Skip entirely when `conversationId` is null (chat memory off — Task 5 pins this).
- Wrap the call so any exception is logged and swallowed.
- [x] **Failing test first:** the recorder is called once per completed turn with the resolved
  `conversationId`; a throwing recorder does not fail the turn.

```bash
git commit -m "732 Invoke the conversation recorder on agent turn completion"
```

---

### Task 4: EE implementation — find-or-create the chat row

**Files:** `ai-hub-service`, alongside `AiHubChatServiceImpl`.

- Find-or-create by `(workspaceId, aiAgentId, threadId)` where `threadId = conversationId`; kind
  `AGENT_CHAT`; owner = the agent's creator.
- **Owner comes from the payload, not a lookup.** `creatorUserId` arrives on the record (Task 3 stamps it).
  The Hub must not read `AiAgent` — doing so would add the `automation-ai-agent` dependency the Global
  Constraints forbid. Verify after this task that no `ai-hub` `build.gradle.kts` gained that entry.
- Keep `message_count` and preview current the way the Hub's own paths do. Write **no transcript** — the
  session store already has it.
- [x] **Failing tests first:**
  - first turn creates a row; second turn on the same `conversationId` **reuses** it (this is the
    opposite of `createAgentChatAiHubChat`, which is deliberately always-new — see Task 6)
  - two different agents on the same `conversationId` get two rows
  - a composer-created `AGENT_CHAT` row (random UUID thread id, null `ai_agent_id`) is never matched
  - the transcript reads back through the existing `getEvents(threadId)` path with no new read code

```bash
git commit -m "732 Create AI Hub chat rows for agent channel conversations"
```

---

### Task 5: Chat memory off ⇒ no row

- [x] **Failing test:** an agent whose `CHAT_MEMORY` element has been deleted
  (`AgentSettingsCard.tsx:106-113` deletes it when the toggle is switched off) produces **no**
  `ai_hub_chat` row and no error. Assert on the absence of a row, not on a log line.

This is a resolved decision, not a gap (spec §8): no memory ⇒ no session ⇒ nothing to show, and an empty
chat would misrepresent an agent that genuinely remembers nothing.

```bash
git commit -m "732 Skip chat creation for agents without chat memory"
```

---

### Task 6: Client — agent conversations in the chats list

- The rows are kind `AGENT_CHAT`, which is already webhook-bridged, so `isWebhookBridgedChat(kind)`
  already disables the composer, model picker, artifacts, suggestion chips and attachments. **Verify no
  new client branch is needed**; if one is, the kind test is wrong, not the client.
- Phase 1 is **owner-only visibility** (spec §5): only the agent's creator sees these chats. A teammate
  sees nothing. Ship that knowingly; the `ResourceVisibility` column that lets the creator widen reach is
  a follow-up, not this plan.
- Distinguish them in the list so a busy Slack agent does not read as the user's own chats — ⚑ label by
  channel type, since the row now carries one.
- [x] Tests mocking the query must use `vi.hoisted` (module-scope refs are TDZ inside `vi.mock`
  factories).

```bash
git commit -m "732 client - Show agent channel conversations in the chats list"
```

---

### Task 7: Docs + verification

- `.agents/agents.md`: a short section on where an agent's conversations are readable.
- **`CLAUDE.md` correction (do not skip):** the AI Hub section claims `SPRING_AI_CHAT_MEMORY` "belongs to
  the AI Agent component's chat-memory cluster elements". That is **false** for the builtin element —
  `ChatMemory.java` is session-backed (`SessionRepository` + `DefaultSessionService` +
  `SessionMemoryAdvisor`). This wrong line cost three spec drafts; fix it.
- [x] Full verification:

```bash
./gradlew spotlessApply
./gradlew compileJava compileTestJava --continue > /tmp/hub-compile.log 2>&1; echo $?; grep '^> Task .* FAILED' /tmp/hub-compile.log
./gradlew :server:ee:libs:ai:ai-hub:ai-hub-service:test :server:libs:modules:components:ai:agent:test --continue > /tmp/hub-test.log 2>&1; echo $?; grep '^> Task .* FAILED' /tmp/hub-test.log
cd client && npm run check; cd ..
```

- [ ] **NOT DONE** — Smoke run: create an agent, add a Slack channel, publish, send two Slack messages, confirm **one**
  chat appears with both turns; toggle Chat memory off, send a third, confirm no new chat and no error.

```bash
git commit -m "732 Document agent conversation visibility and correct the chat-memory note"
```

---

## Self-review notes

- Spec §3 (transcript already in the Hub's store, read by thread id) → the entire plan; no task touches
  the session store.
- Spec §4 (no migration) → no backfill task. Existing conversations appear on their next turn.
- Spec §5 keying → Task 1 (column + partial index), Task 4 (find-or-create).
- Spec §5 ownership → Task 4 (creator resolution), Task 6 (owner-only phase 1).
- Spec §8 chat-memory-off → Task 5.
- Spec §6 always-new-vs-found-or-create → Task 4's third test, the one most likely to be got wrong.
- Deliberately absent: any change to `ChatMemory.java`, `buildChatMemoryElement`, or session user ids.
  Their absence is the point (Global Constraints).

---

## Appendix: the deleted implementation, captured before removal

Read from this worktree on 2026-08-17, before 0_732 deletes it. Source files:
`ee/ai/hub/task/AgentScheduleFiredEventListener.java`, `ee/ai/hub/agent/AiHubScheduledChatDispatcher.java`,
`AiHubChatServiceImpl#createAiHubTaskChat`.

**The old path — the Hub owned the run:**

```
Quartz → AgentScheduleFiredEvent
      → AgentScheduleFiredEventListener.onFired(event)
          schedule = scheduleService.findEnabled(scheduleId)        // null/disabled ⇒ debug-log, return
          chat = chatService.createAiHubTaskChat(                   // ALWAYS-NEW row, random UUID threadId,
                     workspaceId, userId, environment,              //   kind = TASK, tools copied from the task
                     aiHubTaskId, title)
          dispatcher.dispatch(userId, workspaceId, environment,
                     aiHubTaskId, chat.getThreadId(), prompt)       // runs the "ai_hub_ask" LocalAgent via
                                                                    //   chatStreamer.runAgent(agent, params, threadId);
                                                                    //   SseEmitter discarded — nobody is listening
          scheduleService.recordFire(scheduleId)
          auditPublisher.publish(AI_HUB_TASK_SCHEDULE_FIRED, {workspaceId, agentId, scheduleId, chatId})
      catch (Exception) → warn + scheduleService.recordFailure(scheduleId)
```

**The new path — the agent owns the run.** A schedule channel row fires the generated workflow, the
`aiAgent_1` node runs, the builtin chat-memory element writes the session under
`conversationId`, and this plan's recorder creates the chat row. The Hub never runs an agent.

Three things this contrast tells us:

1. **The Hub ran its own `ai_hub_ask` agent, not the user's AI Agent.** So the migration is not merely a
   scheduling move — a scheduled run now executes the *user's configured agent* (its model, tools, skills,
   instructions) instead of the Hub's generic ASK agent. That is an improvement, and it means output will
   legitimately differ after the migration. Worth a release note.
2. **The dispatcher's fire-and-forget shape is the precedent for "no HTTP client is listening."** Its
   comment — the agent "runs to completion and writes to the session store regardless" — is exactly why
   this plan can rely on the transcript existing without anyone watching a stream.
3. **Chat identity changes — see the decision below.**

### ⚑ Decision needed: one chat per fire, or one growing chat?

`createAiHubTaskChat` is explicitly **always-new**: *"every click on a task row in the sidebar starts a
fresh chat rather than restoring a prior thread… The threadId is a plain UUID."* Since the scheduled
listener calls it on every fire, **today every scheduled run produces its own chat row** — a daily task
yields a chat per day.

Under this plan the schedule channel's `conversationId` is a **stable per-row UUID** (companion spec §3,
preserving the generator's existing `scheduleConversationId`), and `thread_id = conversationId`, so all
fires **append to one ever-growing chat**.

The two cannot be mixed: the transcript is stored per thread id, so per-fire chats require a fresh
`conversationId` per fire — which also means the agent stops remembering previous fires. The choice is
therefore genuinely between:

- **One growing chat** (this plan as written): the agent accumulates context across fires — a daily
  summariser can say "unchanged since yesterday". Cost: a chat that never ends, and a visible change from
  today's behaviour.
- **One chat per fire** (today's behaviour): each run is a clean, readable unit. Cost: the agent starts
  cold every time, and the companion spec's stable `conversationId` must change.

⚑ Assumed: one growing chat, because memory continuity is the reason the stable id exists. **Confirm
before implementing** — this is the most user-visible difference in the whole migration, and it is easy to
ship by accident.
