# Workflow-chat bridge — follow-up plan

**Status:** SHIPPED — all 15 gaps complete + AssetFileFacade unification + multi-instance Spring Cache
**Date:** 2026-04-30
**Owner:** Ivica Cardic
**Branch:** `0_732`

## Status summary

All 15 gaps from the original audit landed, plus two structural improvements that surfaced during
implementation:

| Gap | Subject | Status |
|---|---|---|
| 🚩 1 | Resume-flow wiring | ✅ shipped (`ae1b52f2da9`) |
| 🚩 2 | Attachments threading | ✅ shipped (`1fc1bbf3848`) — promoted to first-class FileEntry in `fceb697d48b` |
| 🚩 3 | Workflow-chat title | ✅ shipped (pre-existing, summarized for completeness) |
| 4 | CC header workflow badge | ✅ shipped (`c08ded00965`) |
| 5 | Delete `/automation/chats` route | ✅ shipped (pre-existing) |
| 6 | Empty-state polish | ✅ shipped (`80993ceadcd`) |
| 7 | Workflow icon distinction | ✅ shipped (`17c7d3749eb`) |
| 8 | Refactor `WebhookTriggerController` | ✅ shipped (`1cc7261f22c`) — streaming endpoint only |
| 9 | Unit tests for bridge / router / agent | ✅ shipped (`b83cc6269d8`, `0dbef2dcea6`) |
| 10 | GraphQL test for `createWorkflowChatConversation` | ✅ shipped (`3a2e5c55275`) |
| 11 | Test fixture audit for `kind` column | ✅ shipped (`a989970b86d`, `6d245173c03`) |
| 12 | Liquibase ordering audit | ✅ verified — no changes needed |
| 13 | Friendly disabled-workflow message | ✅ shipped (`f93c299a420`) |
| 14 | Workflow-deletion mid-conversation | ✅ shipped (`7c61e89fe09`) |
| 15 | Archive flow for workflow chats | ✅ shipped (`e1dac74467e`) |

### Beyond the original 15

| Topic | Status |
|---|---|
| Multi-instance Spring Cache for `WebhookResumeRegistry` | ✅ shipped (`39014993c4a`) — Caffeine for single-instance dev, Redis for EE microservice topology |
| `AssetFileFacade` for attachment uploads (workflow + copilot) | ✅ shipped (`9d334775c3f`) — replaced `TempFileStorage`, dropped attachments now first-class workspace artifacts |
| Client-side `AttachmentAdapter` wiring | ✅ shipped (`38cca6a0950`) — the contract on the server side was previously dead |
| `BridgedFileEntry` SDK FileEntry adapter | ✅ shipped (`fceb697d48b`) — replaces raw Map shape that broke `ChatNewRequestTrigger`'s `instanceof FileEntry` check |
| `lastUserMessage` widened to `Role.user` match | ✅ shipped (`fceb697d48b`) — was narrowed to `instanceof UserMessage`, dropped non-UserMessage subclasses |
| `TenantContext` doc on resume HTTP loopback | ✅ shipped (`fceb697d48b`) |
| HTTP resume path test (JDK `HttpServer`) | ✅ shipped (`9491442cb85`) — 4 tests covering JSON success, non-2xx, raw-body fallback, atomic-consume |
| `cache-config` dep on ai-copilot-app | ✅ shipped (`525a9093be5`) |
| Telemetry counters | ✅ shipped (`89deed926cd`) — `bytechef_workflow_chat_turn`, `bytechef_workflow_chat_resume`, `bytechef_workflow_chat_unreachable` |

### What's next

The bridge stack is functionally complete. The next set of work is in the
**Personal Agents** feature plan (separate doc) — extending the routing layer to dispatch
per-turn invocations to user-defined agents with custom system prompts and tool sets, alongside
the existing `COPILOT` (LLM) and `WORKFLOW_CHAT` (bridge) agent kinds.

---

## Original plan (for reference)

The sections below are the original gap inventory. Kept for reference; statuses updated above.

## Context

The Option A workflow-chat bridge stack landed in six commits:

| SHA | Commit |
|---|---|
| `a97fc248b4b` | `732 ai-copilot: persist workflow-chat conversations alongside copilot conversations` |
| `fcec2246557` | `732 client - ai-hub sidebar: New Conversation + Workflow Chats sections` |
| `7131be56173` | `732 platform-webhook: extract WebhookWorkflowExecutionFacade for non-HTTP transports` |
| `ce6ce58aa80` | `732 ai-copilot: WebhookBridgeAgent + AgUiStreamBridge for kind=WORKFLOW_CHAT` |
| `b0cde0a1c52` | `732 ai-copilot: routing agent dispatches kind=WORKFLOW_CHAT to bridge, kind=COPILOT to LLM` |
| `435e90064b1` | `732 client - ai-hub runtime: handle ask-workflow-question custom event` |

The end-to-end flow now functions: clicking a workflow chat in the CC sidebar creates a
`kind=WORKFLOW_CHAT` conversation, sending a message dispatches through the bridge to the webhook
executor, the response renders in the CC thread.

What's structurally complete but functionally partial: the **interactive** parts (resume,
attachments) and the **polish** parts (titles, archive semantics, distinct visual treatment).
This document tracks the 15 follow-up gaps identified in the post-implementation audit.

## Gap inventory

Gaps are grouped by category and prioritised by user impact. Top-three priorities are flagged
🚩 — without those, half of workflow chats are non-functional.

### Functional gaps (block end-to-end usability)

#### Gap 1 🚩 — Resume-flow wiring

**Symptom:** A workflow that emits `ask_user_question` shows the question text inline (already
working), but the user's next message starts a fresh workflow execution instead of resuming the
paused one. The `resumeUrl` from the question payload is delivered to the client and ignored.

**Required changes:**

- **Server (ai-copilot-service):**
  - Persist the last pending `resumeUrl` per conversation. Two options:
    - Add `pending_resume_url` VARCHAR column on `ai_hub_task` (durable across
      restarts).
    - In-memory `Map<conversationId, resumeUrl>` (lost on server restart; acceptable since
      workflow runs likely time out after restart anyway).
  - **Decision:** start with the in-memory store (simpler, no migration). Add the column in a
    follow-up if production traffic shows resume-after-restart is a real case.
  - `WebhookBridgeAgent.run()`: at the top, check the pending-resume map. If a `resumeUrl`
    exists for `conversation.id`, POST the user's text to it via a small `ResumeClient` (uses
    `org.springframework.web.client.RestClient` or similar); clear the map entry; emit a
    `TextMessageContent + RunFinished` summarising "Answer submitted." Skip the regular
    `executeStreaming` path.
  - `AgUiStreamBridge.onEvent` for `ask_user_question`: stash the incoming `resumeUrl` in the
    pending-resume map keyed by conversation id (the bridge has the threadId; need to thread
    `conversationId` into the bridge constructor).
- **Client:** none required for v1. Optional polish: surface "awaiting answer" state by
  disabling the chat composer's plus-button while a resume is pending.

**Tasks:**

1. Create `WebhookResumeRegistry` (in-memory `ConcurrentHashMap<Long, String>`) in
   `ai-copilot-service`. `@Component @ConditionalOnProperty(bytechef.ai.copilot.enabled)`.
2. Inject into `WebhookBridgeAgent` and `AgUiStreamBridge` (thread `conversationId` to the
   bridge constructor — currently it only knows `threadId`).
3. In `AgUiStreamBridge.onEvent`, when the `ask-workflow-question` branch fires, register
   `resumeUrl` in the registry: `registry.put(conversationId, resumeUrl)`.
4. In `WebhookBridgeAgent.run`, look up `registry.remove(conversation.id)` first. If non-null,
   POST the user's message there; emit a `TextMessageContent("Answer submitted.")` + `RunFinished`;
   return early.
5. Unit tests: `WebhookBridgeAgentTest` covers resume-path; `AgUiStreamBridgeTest` covers the
   register-on-ask-event side effect.

**Estimated effort:** medium — ~half-day server, no client.

**Dependencies:** none (Gap 9 unit-test scaffolding helps but isn't blocking).

---

#### Gap 2 🚩 — Attachments not threaded through the bridge

**Symptom:** `WebhookBridgeAgent.buildWebhookRequest` only forwards the user's text. Workflow
chats that depend on file uploads (CSV-analysis workflows, image-processing workflows) silently
drop attachments.

**Required changes:**

- **Client:** the AG-UI `state-to-send` already carries some context (currentTabs, activeFileId,
  referencedResources). Need to also include attachments. Investigate whether the existing
  `aiHubComposerStore.attachments` slot can flow through `buildStateToSend` in
  `AiHubRuntimeProvider`. The state-to-send schema needs an `attachments` field that
  carries file metadata (id, name, mimeType) — actual bytes are uploaded separately.
- **Server (asset-file path):** the existing chat upload flow uses `CreateAssetFileToolCallback`
  to persist files. For workflow chats, we'd want the same upload path to record the asset file
  before the bridge runs. Then the bridge fetches the asset file by id and includes it in the
  `WebhookRequest.body` as a `WebhookBodyImpl` with content type `multipart/form-data`.
- **Bridge:** rebuild `WebhookRequest` to carry attachments. The webhook trigger already knows
  how to read attachments from the multipart body — that path is unchanged.

**Tasks:**

1. Audit `AiHubRuntimeProvider.buildStateToSend` to confirm attachment-id presence in
   the AG-UI state.
2. Server-side: thread attachment ids through `state` to `WebhookBridgeAgent.run`.
3. In `buildWebhookRequest`, fetch each asset file via `AssetFileFacade`, build a multipart-style
   `WebhookBodyImpl` populated with the file bytes + filename.
4. Verify existing webhook triggers correctly read the multipart body the bridge constructs.
5. Add an integration test that posts a message with an attachment to a workflow-chat
   conversation and asserts the workflow received the file.

**Estimated effort:** medium-large — full day, mostly server-side wiring; depends on how state
is currently structured.

**Dependencies:** none, but easier after Gap 1's `conversationId` threading is in place.

---

#### Gap 3 🚩 — Workflow-chat conversation title from workflow label

**Symptom:** Workflow-chat conversations stay "New Conversation" forever because
`generateConversationTitle` only fires for copilot conversations (the bridge bypasses the LLM
agent that triggers it).

**Required changes:**

- At creation time, derive the title from `workflowLabel` (already known on the
  `useWorkspaceChatWorkflowsQuery` row that produced the workflow chat) and pass it to
  `createWorkflowChatConversation`. The mutation already returns the conversation row — the
  title flows through.
- Server-side: `ConversationService.createWorkflowChat` accepts an optional `title` parameter;
  if non-null, sets it on the new row. Existing rows (no title) update at next access via a
  small repair path: when a workflow-chat conversation is loaded with `title == null`, fall back
  to `<projectName> — <workflowLabel>` for display, but don't write it back (keep the row's
  immutable fields immutable post-create).

**Tasks:**

1. Extend `createWorkflowChatConversation` GraphQL mutation to accept an optional `title`
   argument.
2. Update `ConversationService.createWorkflowChat` signature to accept and persist the title.
3. Update `WorkflowChatsList.tsx` to pass `${projectName} — ${workflowLabel}` as the title.
4. Update conversation-display rendering: when `kind=WORKFLOW_CHAT && title==null`, display
   `${projectName} — ${workflowLabel}` derived from the row's project deployment + workflow
   metadata. (Avoids needing a backfill migration.)
5. Add unit test for the find-or-create idempotency: verify a second call with a different
   title doesn't overwrite the first.

**Estimated effort:** small — couple hours, mostly mechanical wiring.

**Dependencies:** none.

---

### UX gaps (polish + clarity)

#### Gap 4 — Workflow context not shown in the conversation view

**Symptom:** A user opening a workflow-chat conversation sees the title (after Gap 3) but no
indication of which workflow it's bound to. The CC chat header shows just the title — fine for
copilot conversations but lossy for workflow chats where users need to know "this is talking to
*Slack notifier* in *Engineering project*" before they fire off a message.

**Required changes:**

- In `AiHub.tsx` header (where the conversation title renders), conditionally show a
  small badge under the title for `kind=WORKFLOW_CHAT`: "Workflow: \<workflowLabel\> · Project:
  \<projectName\>". Reads from the existing conversation row + a project-deployment lookup.

**Tasks:**

1. Extend `useAiHubTasksQuery` to include the workflow label and project name
   for workflow-chat rows. Server-side: GraphQL resolver can JOIN against `project_deployment`
   and `project_workflow` to surface the labels.
2. Render a `<Badge>` below the title in `AiHub.tsx` header when
   `currentConversation.kind === 'WORKFLOW_CHAT'`.
3. Snapshot-test the new badge rendering for both kinds.

**Estimated effort:** small — couple hours.

**Dependencies:** Gap 3 (title rendering) is independent but lands together cleanly.

---

#### Gap 5 — Standalone /automation/chats page redundant — delete + redirect

**Decision:** delete the standalone route, add a redirect from
`/automation/chats/<workflowExecutionId>` to the matching CC conversation.

**Required changes:**

- **Client:**
  - Remove the `<Route>` entries for `/automation/chats` and `/automation/chats/:workflowExecutionId`
    from `routes.tsx`.
  - Add a redirect-only component at the same paths that:
    1. Reads `workflowExecutionId` from the URL.
    2. Calls `createWorkflowChatConversation` (idempotent — returns existing row if the user
       already has one for this workflow).
    3. Navigates to `/automation/ai-hub/conversations/<id>`.
  - Delete dead files: `Chats.tsx`, `Chat.tsx`, `ChatsSidebar.tsx`, `ChatRuntimeProvider.tsx`,
    `useChatsStore.ts`, `useChat.ts`, plus their tests.
- **Server:** no changes. The `useWorkspaceChatWorkflowsQuery` data is consumed by both the old
  page and the new sidebar — the new sidebar continues to use it.

**Migration path for users with bookmarks:**

The redirect handles the common case (`/automation/chats/<id>`). The bare `/automation/chats`
landing page redirects to `/automation/ai-hub` (home view).

**Tasks:**

1. Add `WorkflowChatRedirect.tsx` — small component that performs the find-or-create + navigate.
2. Update `routes.tsx` to mount the redirect at the old paths.
3. Delete the old page + its components and tests.
4. Verify no other code paths import the deleted modules (`grep -r "from '.*chats/Chat"`).
5. Update any sidebar nav links that point to `/automation/chats`.

**Estimated effort:** small — half-day of mechanical deletion + redirect wiring.

**Dependencies:** none.

---

#### Gap 6 — Empty-state polish for the Workflow Chats sidebar section

**Symptom:** When the workspace has zero chat-enabled workflows, `WorkflowChatsList.tsx` returns
`null` — the section header doesn't render at all. Compare to the copilot conversations list's
"No conversations yet" stub. Users don't understand why the section is missing.

**Required changes:**

- Render the "Workflow Chats" header always (when the query has resolved). Show a muted
  "No chat-enabled workflows in this workspace" hint underneath when the list is empty.

**Tasks:**

1. Update `WorkflowChatsList.tsx` to render the header + an empty-state stub instead of `null`
   when `workflowsByProject.size === 0`.
2. Snapshot-test the empty-state rendering.

**Estimated effort:** trivial — 15 minutes.

**Dependencies:** none.

---

#### Gap 7 — Visual distinction for workflow-chat rows in the conversations list

**Symptom:** A workflow-chat conversation, post-creation, lands in the same conversations list
as copilot conversations. The user has to remember which is which by name — there's no badge,
icon, or other affordance.

**Required changes:**

- In the `Conversations` section of the sidebar, when a row's `kind === 'WORKFLOW_CHAT'`, render
  a small workflow icon (or use a different background tint) alongside the title.

**Tasks:**

1. Update the conversation-row renderer in `AiHubTasksSidebar.tsx` to read the
   `kind` field and conditionally render a workflow icon for workflow chats.
2. Snapshot-test both kind variants.

**Estimated effort:** trivial — 15 minutes.

**Dependencies:** none.

---

### Server hardening

#### Gap 8 — Refactor WebhookTriggerController to use the facade

**Symptom:** The controller still has its own inline orchestration (disabled-check + flag-fetch
+ executor calls). The facade and controller maintain parallel copies; future changes to the
orchestration logic risk drifting between paths.

**Required changes:**

- Refactor `WebhookTriggerController.executeWorkflow` and `sseStreamWorkflow` to delegate to
  `WebhookWorkflowExecutionFacade`. The controller becomes a thin transport adapter.
- `executeWorkflow`: call `facade.executeSync` (returning `WebhookExecutionResult`), map the
  result envelope to `ResponseEntity` (Disabled → 410 GONE, Ok → 200 OK with the outputs).
- `sseStreamWorkflow`: call `facade.executeStreaming(workflowExecutionId, webhookRequest, sseStreamBridge)`
  — the existing `WebhookSseStreamBridge` already implements `SseStreamBridge`, so wiring is direct.
- Pure refactor: existing webhook integration tests must pass unchanged.

**Tasks:**

1. Inject `WebhookWorkflowExecutionFacade` into `WebhookTriggerController`.
2. Replace inline disabled-check + flag-fetch + executor calls with facade calls.
3. Move the `WebhookSseStreamBridge` inner class out to a top-level class in the same package
   so the facade can also use it (currently scoped to the controller).
4. Run existing webhook integration tests; assert all pass.
5. Delete the now-dead inline orchestration code from the controller.

**Estimated effort:** medium — ~half-day, mostly cautious mechanical refactoring with test
verification at each step.

**Dependencies:** none, but lower-priority than Gaps 1-3 since it's purely a code-quality fix
with no user-visible benefit.

---

#### Gap 9 — Unit tests for the new agent + bridge classes

**Symptom:** `WebhookBridgeAgent`, `AgUiStreamBridge`, `AiHubRoutingAgent` shipped with
zero tests. The existing 25 service-unit tests + 69 client-runtime tests pass but don't exercise
the new code paths. The bridge has subtle behavior (idempotent onComplete/onError,
ask-user-question detection, sync-vs-streaming branch) that warrants tests.

**Required changes:**

- **`AgUiStreamBridgeTest`:**
  - Mock `AgentSubscriber`; verify each event-shape input produces the expected subscriber call.
  - Verify idempotency: double-onComplete fires events once; double-onError fires once.
  - Verify text-chunk path emits `TextMessageStartEvent` once + `TextMessageContentEvent` per
    chunk + `TextMessageEndEvent` on complete.
  - Verify ask-user-question detection (both `ask_user_question` and `questions` keys).
  - Verify AI Agent SSE event-type pass-through.
- **`WebhookBridgeAgentTest`:**
  - Mock `WebhookWorkflowExecutionFacade` + `ConversationService`.
  - Verify dispatch to `executeSync` for `workflowSyncExecution=true`.
  - Verify dispatch to `executeStreaming` for `workflowSyncExecution=false`.
  - Verify error paths: kind mismatch, missing `workflowExecutionId`, parse failure, missing
    user message.
  - Verify resume-path (after Gap 1 lands).
- **`AiHubRoutingAgentTest`:**
  - Mock both sub-agents + `ConversationService`.
  - Verify routing per conversation kind.
  - Verify null-bridge fallback (logs WARN, dispatches to copilot agent).
  - Verify threadId-null fallback.

**Tasks:**

1. Write the three test classes alongside the existing tests in `ai-copilot-service` test
   sources.
2. Achieve >80% line coverage on the new agent/bridge classes.
3. Verify tests run as part of the existing `gradle test` pipeline.

**Estimated effort:** medium — ~full day for proper coverage; could ship in two commits (bridge
tests + agent tests).

**Dependencies:** Gap 1 + Gap 2 ideally land first so the tests cover the final shapes.

---

#### Gap 10 — GraphQL test for createWorkflowChatConversation

**Symptom:** `ConversationGraphQlController.createWorkflowChatConversation` shipped without a
test. Other mutations on the same controller have tests; the new mutation should match the
pattern.

**Required changes:**

- Test in `ConversationGraphQlControllerTest`:
  - Verify ownership check (cross-user request returns 403-equivalent).
  - Verify idempotency (second call with same args returns same row id).
  - Verify the `kind=WORKFLOW_CHAT` field on the returned row.
  - Verify partial unique constraint (concurrent insert retry path).

**Tasks:**

1. Add `testCreateWorkflowChatConversationCreatesRow`,
   `testCreateWorkflowChatConversationIsIdempotent`,
   `testCreateWorkflowChatConversationDeniesCrossWorkspaceAccess` to the existing test class.
2. Verify all pass; add to the existing `gradle check` pipeline.

**Estimated effort:** small — ~hour.

**Dependencies:** none.

---

#### Gap 11 — Test fixtures for `kind` column

**Symptom:** Test fixtures that create conversations directly via SQL or via the entity (not
through `createWorkflowChat`) might bypass setting the `kind` field. Works today because of the
DB DEFAULT 0, but would silently fail if a future change makes the field's default explicit.

**Required changes:**

- Audit test fixtures (especially in `ConversationRepositoryIntTest`,
  `ConversationServiceIntTest`) for any path that creates a `Conversation` without setting kind.
- Update those fixtures to set `kind = ConversationKind.COPILOT` explicitly (matches current
  behavior, makes the intent clear).

**Tasks:**

1. Grep for `new Conversation(` in test sources.
2. Update each fixture to set kind explicitly.
3. Ensure tests still pass.

**Estimated effort:** trivial — 30 minutes.

**Dependencies:** none.

---

#### Gap 12 — Liquibase migration ordering

**Symptom:** The migration adding `kind` / `workflow_execution_id` / `project_deployment_id`
loads under the `configuration` context. Need to verify all integration test contexts that load
the conversation table also load this migration in the right order.

**Required changes:**

- Audit `application-testint.yml` configs across modules for ones that load
  `ai/copilot/changelog` but might not have run the migration. Add the migration's changeset id
  to any explicit changelog filter.
- Add a one-line comment in the migration file calling out the load-order requirement.

**Tasks:**

1. Run all integration tests across modules; identify any that fail due to missing column.
2. Update `application-testint.yml` files as needed.
3. Document the load-order in the migration file's `<changeSet>` comment.

**Estimated effort:** small — couple hours, mostly investigation.

**Dependencies:** none.

---

### Edge cases

#### Gap 13 — Friendlier error message when workflow is disabled

**Symptom:** When a workflow gets disabled mid-conversation, the bridge correctly emits
`RUN_ERROR("Workflow is disabled.")`. Users see the error but don't know what to do.

**Required changes:**

- Update `WebhookExecutionResult.Disabled` to carry a friendlier message: "This workflow has
  been disabled. Ask an admin to re-enable it before continuing the conversation."
- Surface the message through the bridge unchanged; client renders it as the assistant's reply.

**Tasks:**

1. Update the wording in `WebhookWorkflowExecutionFacadeImpl.executeSync` and
   `executeStreaming` (the `IllegalStateException` thrown at disabled-check time).
2. Verify HTTP controller's 410 GONE response also gets the new wording (covered by the
   facade refactor in Gap 8).

**Estimated effort:** trivial — 15 minutes.

**Dependencies:** Gap 8 (controller refactor) lands the change cleanly to both transports.

---

#### Gap 14 — Workflow deletion mid-conversation

**Symptom:** If the underlying workflow (or its project deployment) is deleted while a
workflow-chat conversation exists, the next user message hits
`webhookFacade.getWebhookTriggerFlags(...)` which throws (the trigger lookup walks a now-missing
workflow). The error propagates as an unhelpful generic RUN_ERROR.

**Required changes:**

- `WebhookBridgeAgent.run`: wrap the `getWebhookTriggerFlags` call in a try-catch. On failure,
  emit a clear RUN_ERROR ("This workflow no longer exists; the conversation cannot continue. You
  can delete it from the sidebar.").
- Optional: archive the conversation automatically when the workflow disappears. Defer until
  there's evidence users actually delete workflows mid-chat.

**Tasks:**

1. Add the try-catch + RUN_ERROR emission in `WebhookBridgeAgent.run`.
2. Unit test for the failure path in `WebhookBridgeAgentTest`.

**Estimated effort:** trivial — 30 minutes.

**Dependencies:** Gap 9 (test scaffolding).

---

#### Gap 15 — Archive flow for workflow-chat conversations

**Symptom:** Copilot conversations support archive/unarchive. Workflow chats inherit the same
UI. Archiving a workflow chat is semantically odd — the conversation is bound to a still-active
workflow, just hidden.

**Decision needed:** does archiving a workflow chat:

a. Hide it from the active list (matches copilot behavior, no special-case)?
b. Get prevented via UI (workflow chats don't have an archive button)?
c. Trigger different behavior (e.g., disconnect from the workflow)?

**Recommendation:** Option (a). Workflow chats hide the same way copilot ones do.
Re-clicking the workflow in the sidebar's Workflow Chats section restores the archived
conversation (the find-or-create in `createWorkflowChat` will find the archived row and reuse
it). This is the lowest-friction behavior; users don't need to learn a new pattern.

**Tasks:**

1. Verify `createWorkflowChat` find-or-create returns archived rows (currently it does, since
   the unique index doesn't filter by status).
2. Add a side effect: when a workflow chat is unarchived via re-click, the row's
   `status` flips from `ARCHIVED` to `ACTIVE`. Mirror what copilot conversations do.
3. Snapshot-test the round-trip: archive → re-click sidebar → row reappears in conversations
   list as ACTIVE.

**Estimated effort:** small — couple hours.

**Dependencies:** none.

---

## Suggested commit ordering

Each commit is independently mergeable; the order optimises for user impact + minimal cross-
commit churn.

### Tier 1 — block-feature gaps

1. **Gap 3** (workflow-label title) — small, highest UX impact per LoC.
2. **Gap 5** (delete `/chats` + redirect) — clean-up + simpler mental model.
3. **Gap 1** (resume-flow) — half the workflow-chat UX depends on this.
4. **Gap 2** (attachments) — substantial, but unlocks file-driven workflow chats.

### Tier 2 — polish + correctness

5. **Gap 6** (empty-state polish) — trivial.
6. **Gap 7** (visual distinction in conversations list) — trivial.
7. **Gap 4** (workflow context badge) — small, complements Gap 3.
8. **Gap 13** (disabled-workflow message) — trivial.
9. **Gap 14** (workflow-deletion handling) — trivial.
10. **Gap 15** (archive semantics) — small, decision already made.

### Tier 3 — code quality

11. **Gap 9** (unit tests) — should land alongside Gaps 1-2 ideally; trail batch otherwise.
12. **Gap 10** (GraphQL test) — small.
13. **Gap 11** (test fixture audit) — trivial.
14. **Gap 12** (Liquibase ordering audit) — small, non-urgent.
15. **Gap 8** (controller refactor) — pure refactor, no user impact; lowest priority.

## Open questions

1. **Resume-flow durability:** should the pending-resume map persist across server restarts?
   v1 says no (in-memory). Promote to a column if production traffic shows resume-after-restart
   is common.
2. **Attachment shape:** does the AG-UI state-to-send already carry attachments in a usable
   form, or do we need a new field? Investigation needed before sizing Gap 2.
3. **Conversation title format for workflow chats:** "\<projectName\> — \<workflowLabel\>" or
   just "\<workflowLabel\>"? The longer form is unambiguous when multiple projects share a
   workflow name; the shorter is cleaner when projects are obviously distinct. Lean toward the
   longer form for safety.

## Definition of done

This document is "done" when all 15 gaps are either:
- Landed as commits on `0_732`, OR
- Promoted to standalone follow-up issues with their own scope, OR
- Explicitly de-scoped with a written rationale.
