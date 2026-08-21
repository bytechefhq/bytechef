# HITL Approvals — Remaining Gap Backlog

*Follow-up to `docs/superpowers/plans/2026-07-21-hitl-gap-closure.md` and the 2026-07-22 gap
review. All 43 review findings were re-verified against current code (branch `0_732`); most were
fixed after the review's snapshot. This file tracks only what is genuinely still open or partial,
with why it was NOT blind-fixed and the concrete next step.*

**Status legend:** OPEN = defect still present · PARTIAL = mitigated but incomplete.

Verified fixed and out of scope of this backlog: the two author regressions (EE build-break stubs,
`pendingApprovals` authorization), all High leads, all three adversarially-Confirmed channel bugs,
and the capability-token leak on the `approvalTask(s)` reads (now `@PreAuthorize("isTenantAdmin()")`).

---

## 1. Engine concurrency — needs the atlas/coordinator integration-test harness

These touch the workflow engine's resume path. They must be validated by Testcontainers-backed
integration tests, not blind edits — a wrong change risks double-executing or losing a run.

### 1a. Concurrent double resolution both return OK (Medium)

Two racing resolutions (e.g. chat-card Approve + email-form Reject) both pass the facade's STOPPED
check, both get HTTP 204, both increment `bytechef_approval_resolution`, and both publish
`ResumeJobEvent`. Which decision wins is decided later by `resumeToStatusStarted`'s optimistic lock;
the loser is told success but their decision was discarded, and the metric double-counts.

- Evidence: `JobResumeFacadeImpl.java:128-162` (read STOPPED → publish, no atomic claim);
  `TaskCoordinator.java:127` (`resumeToStatusStarted`, Assert-only) is the sole guard;
  `JobServiceImpl.java:158-169` + `Job.java` `@Version`.
- Fix: make the claim atomic at the facade boundary — compare-and-set the job status, or
  delete-claim the `task_state` / `JOB_RESUME_ID` row inside the resume transaction — and return
  `GONE` to the loser; increment the resolution counter only for the winner.
- Test: an IntTest firing two concurrent `resumeJob` calls; assert exactly one OK, one GONE, one
  counter increment.

### 1b. Resume racing the expiry sweep resurrects a FAILED run (Medium)

The facade validates STOPPED/not-expired, but the actual resume runs later via `ResumeJobEvent`
(AFTER_COMMIT + broker hop). If `ApprovalExpiryMonitor` commits FAILED in that window,
`resumeToStatusStarted` still succeeds because it accepts FAILED jobs — the expired run flips back
to STARTED after downstream consumers already observed a terminal FAILED.

- Evidence: `JobResumeFacadeImpl.java:111-137`; `ApprovalExpiryMonitor.java:152-190`;
  `JobServiceImpl.java:206-208` (`isRestartable` accepts FAILED).
- Fix: carry a STOPPED-only flag on the token-based `ResumeJobEvent` path so
  `resumeToStatusStarted` restricts to STOPPED (keep FAILED-restart for the explicit restart API),
  or have the coordinator re-check suspend expiry before dispatching.

---

## 2. Agent internals — need a running agent to verify

Wrong fixes here corrupt production chat continuations; the streaming path is what production chat
workflows use via `WebhookBridgeAgent`.

### 2a. requestApproval invoked as an agent tool drops the editor card (Medium)

In a canvas test run, an agent that calls the `requestApproval` TOOL suspends correctly but the
`approval_request` card event is sent to `NOOP_SSE_EMITTER` and discarded — the tester sees a hung
run with no card. The gate and the standalone action both deliver the editor card; the
approval-as-tool entry point is the one path that drops it.

- Evidence: `ApprovalRequestApprovalTool.java:99-107` (`NOOP_SSE_EMITTER`); contrast
  `ApprovalGateToolCallback.java:192-233`.
- Fix: thread the agent's `SSE_EMITTER_REFERENCE` / `SSE_BUFFERED_EVENTS` from the ToolContext into
  the drained handler, or replace the NOOP emitter with one that forwards into the buffered-events
  queue.

### 2b. AiAgentStreamChatAction writes crash checkpoints but never restores or clears them (Medium)

For streaming chat runs a checkpoint is persisted after every completed tool round, but a
crash-resumed job never reads it (restore lives only in `AiAgentChatAction.perform`), and nothing
clears it on success — dead I/O per round + a stale conversation snapshot left in `data_storage`
until job purge, plus a stale-restore hazard if a non-stream Chat node in the same job shares the
inputParameters fingerprint.

- Evidence: `AiAgentStreamChatAction.java` (no `fetchCheckpointedConversation` /
  `clearConversationCheckpoint`); checkpointer wired via `AbstractAiAgentChatAction.java:245` +
  `550-566`.
- Fix (safe option): pass a null checkpointer from the stream action so the dead writes stop.
  Fix (full option): add restore-on-perform + clear on stream-completion.

---

## 3. Atlas layering — needs an event hook, not a direct call

### 3a. task_state rows leak on user/retention job deletion (Partial)

The expiry path now deletes the suspended `task_state` row, but `JobFacadeImpl.deleteJob` (the
user-initiated and retention-monitor delete path) does not — a suspended run that is deleted rather
than expired leaves its `task_state` row (which contains the rendered approval request) behind.

- Evidence: `ApprovalExpiryMonitor.java:204` + `deleteTaskState:211-228` (done);
  `JobFacadeImpl.deleteJob` has zero `TaskState` references.
- Constraint: `JobFacadeImpl` is in `atlas-execution`; `TaskStateService` is in
  `platform-workflow-execution` — atlas must not depend on platform.
- Fix: delete the row from a job-deletion application-event listener in `platform-coordinator` (or
  `platform-workflow-execution`), or fold it into `JobRetentionMonitor`. Keyed by `jobResumeId`
  from job metadata.

---

## 4. Architectural / infra decisions

### 4a. Approval-task channel + reconciliation are dead in distributed EE (Medium)

On EE worker-app the approval-task delivery channel always throws (absorbed as a per-channel
delivery failure), so approval-task rows are never created; and no EE app hosts
`ApprovalTaskReconciliationMonitor`, `ApprovalTaskCompletionListener`, or the `pendingApprovals`
query. The docs present the approval-task inbox as the channel-independent safety net, but it is
monolith-only.

- Evidence: `RemoteApprovalTaskFacadeClient.java:24-27` (`createApprovalTask` throws UOE); no
  `automation-task-remote-rest` module; `automation-task-service` wired only into `server-app`.
- Decision needed: either add an `automation-task-remote-rest` endpoint + real remote client (and
  host the reconciliation monitor in an EE app), OR document/validate the approval-task channel and
  inbox as monolith-only so users don't configure a channel that always fails.

### 4b. Expiry/reminder sweeps have no multi-instance coordination (Low, Partial)

The expiry sweep now continues past a per-job lock conflict (per-job try/catch added), but the
reminder marker is still written AFTER the send and the monitors are plain `@Scheduled` beans, so
two coordinator replicas can emit duplicate `JOB_APPROVAL_EXPIRING` notifications.

- Evidence: `ApprovalReminderMonitor.java:135-137` (mark after send); monitors are plain
  `@Scheduled`.
- Fix: write the reminder marker with a conditional/optimistic update before sending, add a
  distributed lock (e.g. ShedLock), or accept documented at-least-once reminders.

---

## 5. EE / MCP-typed — compile-in-CI only, low value

### 5a. MCP form-mode elicitation unavailable when public-url is unset (Low)

Form-mode resolution only needs the resume token, which exists in job metadata regardless of
`publicUrl`, yet on deployments without a configured public URL the descriptor carries no `formUrl`
so form-capable clients get neither elicitation mode.

- Evidence: `ApprovalFormUrls.java:38-39`; `AutomationMcpToolFacade.resolvePendingApprovalFormUrl`
  returns empty; `ApprovalElicitingToolSpecifications.java` (form path derives the token from
  `formUrl`).
- Fix: add a signed `resumeToken` field to the `approval_required` descriptor independent of
  `formUrl`; let form-mode elicitation use it, keep URL mode gated on `formUrl`.

### 5b. MCP cross-workflow job-output read (Medium, Partial)

The spoofed-`formUrl` phishing vector is closed (URL re-derived server-side), but
`awaitApprovedWorkflowRun(jobId)` is only tenant-scoped — a genuinely-paused job in another
workspace of the same tenant remains readable by id.

- Evidence: `AutomationMcpToolFacade.java` `awaitApprovedWorkflowRun` — no check the job belongs to
  a workflow exposed by this MCP server.
- Fix: verify the job's workflow is one this MCP server exposes (via the server's project/workflow
  membership) before returning outputs.

---

## 6. Test coverage — a dedicated compile-in-CI writing pass

### 6a. Zero unit-test coverage for the 14 external approval channels (Medium)

The markdown/HTML escaping functions are security-load-bearing (they stop AI-chosen tool args from
forging clickable Approve links) and are pinned by nothing.

- Fix: per-channel unit tests (mock `ClusterElementContext`/`Http`) asserting escaped output for
  hostile title/description, both message branches (with/without inputs), the Expires line, and the
  request-body shape. At minimum pin `escapeMrkdwn` (Slack), `escapeMarkdown`
  (Discord/Mattermost/Rocketchat), and the three email channels' HTML escaping.

### 6b. No test coverage for the MCP/A2A approval facades (Medium)

`AutomationMcpToolFacade.awaitApprovedWorkflowRun` / `resolveApprovalAndAwait` (poll interval,
deadline expiry, second-pause re-descriptor, non-OK → `approval_unavailable`) and
`AutomationA2AServerFacade`'s STOPPED → `input-required` branch + `pollRun` state mapping are pinned
only by prose.

- Fix: `AutomationMcpToolFacadeTest` (job leaves STOPPED then completes → outputs; deadline while
  STOPPED → descriptor; re-pause → new descriptor; GONE/INVALID → `approval_unavailable`) and A2A
  facade tests for the STOPPED execute branch and each `pollRun` state.

### 6c. ApprovalGateToolCallback best-effort fan-out has zero test coverage (Medium)

Every existing `ApprovalGateToolCallbackTest` case constructs the callback with an empty channel
list, so the best-effort fan-out (first channel throws → second still delivers; all throw →
`IllegalStateException`, no suspend) is untested — a regression would pass the suite.

- Fix: mirror `ApprovalRequestApprovalActionTest`'s two fan-out tests for the gate.

---

## 7. Client — needs a GraphQL query arg + codegen

### 7a. Pending run approvals ignores the page's environment scope (Low, Partial)

The list is now invalidated on resolution, but the `pendingApprovals` query takes no
`environmentId` while the adjacent approval-task list is environment-scoped — the banner lists runs
from all environments, reading as inconsistent counts.

- Evidence: `PendingApprovalsList.tsx` calls `usePendingApprovalsQuery(undefined, …)`;
  `pendingApprovals` GraphQL operation has no `environmentId`.
- Fix: add `environmentId` to the `pendingApprovals` query (regen `graphql.ts`) and pass the
  selected environment, OR label the section as cross-environment.

---

## Channel in-place approvals

In-place resolution lets the reviewer approve/reject inside the messenger (no browser, message
rewrites to the outcome, verified identity → `approvedBy`), instead of the default URL buttons that
open the hosted form. Field-less approvals only — approvals with form fields keep the hosted-form
link. The reusable pattern: message carries an interactive control → provider POSTs the tap to a
ByteChef endpoint → verify → `JobResumeFacade.resumeJob` → rewrite the message.

### Shipped

- **Slack** — `signingSecret` on the connection switches to `block_actions` buttons;
  `SlackInteractivityController`/`Handler` verify the `X-Slack-Signature` HMAC. Button `value` (2000
  chars) holds the whole signed token; no store needed. Includes a discard-with-comment modal.
- **WhatsApp (Meta)** — `appSecret` on the connection switches to interactive reply buttons;
  `WhatsAppInteractivityController`/`Handler` verify `X-Hub-Signature-256` (HMAC) and answer Meta's
  GET verify handshake (`bytechef.webhook.whatsapp.verify-token`). Button `id` (256 chars) holds the
  decision-prefixed signed token; no store needed. Single per-app webhook — use a dedicated
  approvals app if a WhatsApp trigger also uses that app.
- **Mattermost** — interactive attachment buttons carry `integration.url` (`/mattermost/interactivity`)
  + the token in `integration.context`; no store, no connection secret. Mattermost doesn't sign these,
  so the token in context is the capability (same as the hosted-form link) and the reviewer identity
  is not recorded.

- **Telegram** — inline-keyboard callback buttons with `callback_data = <shortId>:a|d`; the bot
  webhook (set with a secret token) points at `/telegram/interactivity`, verified by the
  `X-Telegram-Bot-Api-Secret-Token` header. Because `callback_data` caps at 64 bytes, the channel
  mints a short id at send time via `POST /approval/short-token` (its own `HttpClient` — it's a
  ServiceLoader component with no Spring beans) and keeps the form link in the text as a fallback.
  Resolve → `answerCallbackQuery` + `editMessageText`. Use a dedicated approvals bot (one webhook
  per bot).
- **Discord** — interaction buttons with `custom_id = <shortId>:a|d` (100-char cap → short id);
  Interactions Endpoint URL `/discord/interactivity` verifies the Ed25519 signature
  (`bytechef.webhook.discord.public-key`, JDK-native Ed25519) and answers `PING`→`PONG`; a button
  returns an inline `UPDATE_MESSAGE`. Reuses the short-token store + mint endpoint.

**Shared short-token store** (`ApprovalShortTokenStore` + `POST /approval/short-token`,
platform-webhook-rest-impl): maps a random short id → the signed token, for channels whose button
payload is too small to carry the whole token (Telegram/Discord). **Process-local, lost on restart**
— acceptable because those channels also keep the hosted-form link in the message, so a lost mapping
degrades to the form link. A distributed EE deployment with multiple coordinator replicas needs a
shared store (Redis / DB) — the one remaining open item for these two.

### Will NOT build (by decision, 2026-07-22) — Twilio / Infobip SMS+WhatsApp, Rocket.Chat

These stay on the current URL buttons (open the hosted form) — a deliberate decision, not a backlog
item. They are technically buildable but only through a degraded path, so they are not worth the
build + maintenance:

- **Twilio / Infobip SMS + WhatsApp** — SMS has no buttons and BSP WhatsApp interactive buttons need
  a pre-approved Content Template, so the only route is a **reply-code** flow (*"reply `A <code>` /
  `D <code>`"*), parsed by a BSP inbound webhook. Worse UX than buttons, plus Twilio's `X-Twilio-
  Signature` (HMAC-SHA1 over the exact URL + sorted params) verification is fiddly and only
  checkable live.
- **Rocket.Chat** — no `integration.url` callback like Mattermost; would need a deployed Rocket.Chat
  App (UIKit) or a button-`msg` command + an operator-configured outgoing-webhook. Too heavy for the
  payoff.

The design sketches above are retained only as a record of why; there is no plan to implement them.

## Not doing (by decision)

- **Field-less card "terminal" degrade state** — would contradict the deliberate keep-buttons-
  enabled-for-retry design (pinned by `ApprovalForm.test.tsx`); reconciled by correcting the docs
  instead.
