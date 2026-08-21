<!-- Extracted from CLAUDE.md so the agent-facing reference does not sit in every prompt.
     Load this when working on approvals, the tool gate, or a delivery channel. -->

# Agent HITL approvals (chat cards + tool gate)

Spec: `docs/superpowers/specs/2026-07-21-agent-hitl-approval-chat-design.md`; user docs
`docs/content/docs/automation/human-in-the-loop.mdx`. Load-bearing pieces:

- Two primitives, disjoint jobs: **Approval** (decision; comment valid on BOTH outcomes under the
  reserved `comment` key) and **AskUserQuestion** (LLM clarification). No free-text mode on
  Approval; typing in chat NEVER resolves an approval (D4).
- **ChatApprovalChannel** (`chat/chat` APPROVAL_CHANNELS element, on the **chat** component — it moved off `approval` so a channel is not itself a cluster root) publishes an
  `__eventType: approval_request` data event (`AiAgentSseEventType.APPROVAL_REQUEST`) onto the
  job's SSE stream via the `SSE_STREAM_EVENTS` broker route; requires `getJobId()` (now on
  `JobContextAware`), throws without one. Both SSE bridges + `JobResumeSseStreamBridge` map
  `__eventType` payloads to named events; `AgUiStreamBridge` passes them as AG-UI CustomEvents and
  folds a persist-only markdown form-link marker into accumulated text for reload.
- **Channel fan-out is best-effort**: `ApprovalRequestApprovalAction.deliverToChannels` and
  `ApprovalGateToolCallback.deliverApprovalRequest` try/catch per channel (warn log), failing the
  step ONLY when every configured channel fails. Per-channel counters:
  `bytechef_approval_request` (success) + `bytechef_approval_delivery_failure` (failure), both
  incremented in `ClusterElementDefinitionServiceImpl.executeApprovalChannel`. Channels receive
  the computed expiry under `ApprovalChannelFunction.EXPIRES_AT` (ISO-8601) and render it in
  their messages; the approval-task channel maps it onto the task's `dueDate`; the chat channel
  carries it in the `approval_request` event. Markdown channels (Mattermost, Rocket.Chat) escape
  link-forming characters in title/description — gate descriptions embed AI-chosen tool args.
  One-click `?approved=` links NEVER auto-submit:
  `ApprovalForm` shows a pre-selected Confirm view (link scanners must not resolve approvals).
  Delivery channels: chat, Slack, Discord, Telegram, Mattermost, Rocket.Chat, Gmail, Outlook 365,
  generic SMTP email, WhatsApp (Meta/Twilio/Infobip), SMS (Twilio/Infobip), approval task.
  In-place Slack resolution IS implemented: an optional `signingSecret` on the Slack connection
  switches `SlackApprovalChannel` to `block_actions` buttons (value = tokenized resume id), and
  `SlackInteractivityController`/`SlackInteractivityHandler` (platform-webhook-rest-impl,
  anonymous `/slack/interactivity`, permit-listed) verify the `X-Slack-Signature` HMAC per
  tenant (anchored by the resume id), resolve via `JobResumeFacade`, and rewrite the message via
  `response_url`. Spec: `docs/superpowers/specs/2026-07-22-slack-inplace-approval-interactivity-design.md`.
  In-place is also implemented for **WhatsApp (Meta)** (`appSecret` on the connection →
  interactive reply buttons; `WhatsAppInteractivityController`/`Handler` verify `X-Hub-Signature-256`,
  GET verify-handshake via `bytechef.webhook.whatsapp.verify-token`; button id carries the
  decision-prefixed signed token), **Mattermost** (interactive attachment buttons whose
  `integration.url` = `/mattermost/interactivity` carry the token in `integration.context`; unsigned,
  so no `approvedBy`), **Telegram** (`webhookSecretToken` on the connection → inline-keyboard callback
  buttons; `/telegram/interactivity` verifies `X-Telegram-Bot-Api-Secret-Token`), and **Discord**
  (`publicKey` on the connection → interaction buttons; `/discord/interactivity` verifies the Ed25519
  signature against `bytechef.webhook.discord.public-key`, answers `PING`→`PONG`). Telegram/Discord
  cap the button payload below the signed token, so the channel mints a short id via the anonymous
  `POST /approval/short-token` (`ApprovalShortTokenStore`, process-local — form-link fallback covers a
  restart; distributed EE needs a shared store). Twilio/Infobip SMS+WhatsApp and Rocket.Chat
  intentionally stay on URL buttons (won't be built — SMS/BSP reply-code UX and Rocket.Chat's lack of
  a callback aren't worth it; see `docs/superpowers/plans/2026-07-22-hitl-gap-remaining-backlog.md`).
- **Tool gate**: the `approvalGateTool` cluster element (`AiAgentUtilsApprovalGateTool`, TOOLS type on the
  **aiAgentUtils** component) declares two child types — `TOOLS` (what it gates) and
  `APPROVAL_CHANNELS` (where requests go) — plus `name` and expiry (`approvalExpiresIn` /
  `approvalExpiresInUnit`) properties. It returns its children already wrapped in
  `ApprovalGateToolCallback`, so the agent action has NO gate special case; simulation and
  observable wrapping run over the flattened list afterwards, preserving gate-inside-audit
  ordering. An agent may carry several gates, each with its own channels and expiry; an empty
  channel list defaults to the chat channel. On a hand-authored workflow those channels are picked on
  the canvas; the **Agents** feature does not offer that choice — it derives them from the agent's own
  inbound channels (see `.agents/agents.md`), so an approval goes back over the channel the
  conversation arrived on, reusing its connection. Suspends via the sentinel protocol with
  `GATED_TOOL_NAME`/`GATED_TOOL_INPUT` continueParameters; a second gated call in one round defers
  (single-suspend-per-round invariant). Resume branch
  `AbstractAiAgentChatAction.resolveGatedToolResumeData`: approve → RAW callback executes original
  args; reject → denial JSON. **The gate and the agent action live in different modules**
  (`ai/agent/utils` vs `ai/agent`) and couple only through `ToolSuspendConstants`; because a gated
  callback reports its delegate's tool definition, resume unwraps it via `DelegatingToolCallback`
  (`ai/llm`) rather than importing the gate. Gates are rejected under a subagent — the sentinel
  would be lost in the subagent's own ChatClient and the suspend orphaned — and `requestApproval`
  is rejected as a gate child. Editor runs deliver the
  card via the agent's ToolContext SSE emitter (channels are production transports); the standalone
  Approval action delivers it in editor runs by returning a one-shot `SuspendAwareSseEmitterHandler`
  (suspend happens INSIDE the handler — suspending before returning makes `checkSuspend` swallow
  the emitter output) that the in-process post-output processor drains into the test-run stream.
- **Client cards**: `ApprovalRequestMessage` (`data-approval-request` in `aiChatDataComponents`) —
  self-contained buttons+comment for field-less approvals (`hasInputs` flag threaded from event
  `inputs`), embeds `ApprovalForm` only when fields exist. Resolution goes through
  `ApprovalResolutionContext` when the surface provides it (CE Chats page, canvas test chat,
  AI Hub workflow chat — each points its SSE machinery at `POST /job/resume/{id}` with
  `Accept: text/event-stream`, streaming the continuation through its normal event handlers), else
  the plain resume mutation. AI Hub persists the continuation on stream close via the
  `appendAiHubChatAssistantMessage` GraphQL mutation. The `@bytechef/chat` widget has its own
  inline card + `drainSseResponse`-based continuation.
- `WebhookBridgeAgent` routes runs with an approval task onto the streaming path
  (`WebhookWorkflowExecutor.hasApprovalTask`). The coordinator's `SseStreamApplicationEventListener`
  emits a named `result` data event on COMPLETED (message read from the `WEBHOOK_RESPONSE`-tagged
  task execution, published before the terminal job-status event) so approval-only chat workflows
  keep their final reply; `AgUiStreamBridge` renders it only when nothing was streamed. MCP/A2A
  sync runs paused on an approval return "approval required — resolve at <form URL>"
  (`ApprovalFormUrls.buildFormUrl`, STOPPED + `jobResumeId` metadata) instead of an empty result;
  MCP workflow tools additionally URL-elicit the form on capable clients (form-elicitation
  fallback with a simple approved/comment schema on form-only clients; bounded at 3 rounds per
  call for chained approvals) and return the resumed run's output in the same tools/call
  (`ApprovalElicitingToolSpecifications` + `AutomationMcpToolFacade.awaitApprovedWorkflowRun`,
  which polls the job out of STOPPED before awaiting — STOPPED is terminal to
  `JobCompletionAwaiter`), and A2A surfaces the task as `input-required`
  (`A2AAgentResult.ofInputRequired(text, jobId)`); a later `tasks/get` refreshes it through the
  `A2AAgentExecutor.pollRun(jobId)` SPI. Enriched `task_started` events
  (`{event, payload:{taskExecutionId,name,type}}` from the coordinator) render as AG-UI tool-call
  step chips in `AgUiStreamBridge` and as a floating step chip on the CE Chats page.
- **Suspend expiry is enforced and configurable**: the Approval action's `expiresIn`/`expiresInUnit`
  properties (HOURS/DAYS, default 60 days) and a gated tool's `approvalExpiresIn`/
  `approvalExpiresInUnit` entry parameters (`ToolConstants`) drive the suspend `expiresAt`.
  `JobResumeFacadeImpl` rejects expired resumes (GONE), and
  `ApprovalExpiryMonitor` (platform-coordinator, 15-min per-tenant sweep over
  `getStaleJobs(STOPPED, now)`, `bytechef.workflow.execution.approval-expiry.enabled` default on)
  fails runs whose suspend `expiresAt` passed. Metrics: `bytechef_approval_expired{source=resume|sweep}`
  counter + `bytechef_approval_pending` gauge. `ApprovalReminderMonitor` (platform-coordinator,
  15-min sweep, `bytechef.workflow.execution.approval-reminder.*` — `enabled` default on,
  `lead-time` default PT24H) fires a `JOB_APPROVAL_EXPIRING` notification (new
  `NotificationEvent.Type`, append-only ordinal; `ApprovalReminder{Email,Slack,Webhook}NotificationHandler`)
  through the central notification registry once per run (idempotence via job metadata
  `approvalReminderSentAt`), carrying expiry + form URL in `NotificationHandlerContext`.
  `ApprovalEscalationMonitor` (platform-coordinator, 15-min sweep,
  `bytechef.workflow.execution.approval-escalation.*` — `enabled` default on, `after` a Duration
  with NO default so the sweep is a no-op until set) fires a `JOB_APPROVAL_ESCALATED` notification
  (new `NotificationEvent.Type`, append-only ordinal 7; liquibase id 8/type 7;
  `ApprovalEscalation{Email,Slack,Webhook}NotificationHandler`) once a run has been paused on an
  approval longer than `after` (measured from the suspended task execution's startDate) and is
  still unexpired — routed to a DIFFERENT subscribed `Notification` than the reminder. Idempotence
  via job metadata `approvalEscalatedAt`; both `approvalReminderSentAt` and `approvalEscalatedAt`
  are cleared on resume in `SuspendTaskDispatcherPreSendProcessor` (per-suspend, not per-run).
  The gate's expiry is editable via the "Approval expires in" preset submenu in
  `AiAgentToolDropdownMenu` (expiresIn 0 = clear override). `ApprovalTaskReconciliationMonitor`
  (automation-task-service, per-tenant sweep) closes OPEN/IN_PROGRESS Approval Task rows whose
  backing run is no longer STOPPED: COMPLETED run → COMPLETED row (covers cross-process resumes
  the in-JVM `ApprovalTaskCompletionListener` misses), FAILED/CANCELLED/purged run → EXPIRED row
  (`ApprovalTask.Status.EXPIRED`, appended last — ordinal storage). The **pending-approvals inbox**
  (`ApprovalTaskFacade.getPendingApprovals` → `pendingApprovals` GraphQL query →
  `PendingApprovalsList` on the Approval Tasks page) lists all STOPPED runs carrying a
  `jobResumeId`, with workflow label, form URL, createdDate, and expiry — channel-independent.
  In distributed EE the approval-task channel + inbox are NOT monolith-only: worker-app's
  `RemoteApprovalTaskFacadeClient` issues real `LoadBalancedRestClient` calls to configuration-app's
  `automation-task-remote-rest` `RemoteApprovalTaskFacadeController` (`/remote/approval-task-facade`),
  and configuration-app hosts the real `automation-task-service` (facade + `ApprovalTaskReconciliationMonitor`
  + `ApprovalTaskCompletionListener`) and `automation-task-graphql`. The service-level
  `RemoteApprovalTaskServiceClient` stays a stub — `ApprovalTaskService` is only consumed by the
  GraphQL controller, which runs in configuration-app against the real bean.
- **Chat-only channel validation**: `WorkflowValidator.validateChatOnlyApprovalChannels`
  (platform-workflow-validator-service) warns, when the workflow has no `chat/` trigger, about a
  task whose own `approvalChannels` are chat-only (the standalone Approval action) and about any
  `approvalGateTool` whose channels are chat-only OR absent (absent defaults to chat). Such
  webhook/schedule runs pause with no live card and are only reachable via the pending-approvals
  inbox or the hosted form. The warning names the gate.
- AI Hub copilot chat is OUT of scope (keeps its pinned `askUserQuestion`).
