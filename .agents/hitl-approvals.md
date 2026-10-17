<!-- Extracted from CLAUDE.md so the agent-facing reference does not sit in every prompt.
     Load this when working on the approval step, a delivery channel, interactivity, the resume contract,
     the approval monitors, or approval-task rows. -->

# HITL approvals

User docs: `docs/content/docs/platform/automation/build/workflows/human-in-the-loop.mdx`,
`docs/content/docs/platform/automation/monitor/approval-tasks.mdx`. Load-bearing pieces:

- **Two primitives, disjoint jobs.** **Approval** is a decision: resume data carries a boolean `approved`, plus
  `comment` (valid on BOTH outcomes) and `approvedBy`. **AskUserQuestion** (`aiAgentUtils` `askUserQuestion` tool)
  is LLM clarification and resumes with `STREAMING_RESUME`. Typing in chat NEVER resolves an approval.
- **The approval step** is `approval/v1/requestApproval` (`ApprovalRequestApprovalAction`) plus the same action
  as an agent TOOLS element (`ApprovalRequestApprovalTool`, same component). Both take `APPROVAL_CHANNELS`
  children (`ApprovalComponentDefinition`). Expiry comes from `expiresIn`/`expiresInUnit` (HOURS/DAYS); blank means
  `ActionContextAware.DEFAULT_APPROVAL_EXPIRY` (60 days). The suspend's continue parameters carry `formUrl`,
  `approvalRaisedAt` and `APPROVAL_RESUME=true`. The tool also stores its own inputs under
  `APPROVAL_FORM_PARAMETERS`, because otherwise the form would be built from the agent's parameters. In editor runs the
  action suspends but skips delivery, and the tool neither delivers nor suspends: it returns a "cannot be approved in
  the editor" result. `resumePerform` always outputs `comment` and `approvedBy` as strings (`""` when absent or the
  approver is unknown), so later steps can reference them.
  An action with no channels delivers nothing, so the run can be reached only through the inbox or the hosted form.
  The older `approval-link` component and `waitForApproval` dispatcher take a separate path (`ApprovalId`,
  `JobResumeFacade.resumeApproval`).
- **The suspend is recorded when it happens.** `SuspendTaskCompletionHandler` writes the whole `Suspend`
  (`expiresAt` + continue parameters) to the suspended task execution's `suspend` metadata. It also copies
  `APPROVAL_RESUME`/`STREAMING_RESUME`/`SUBFLOW_RESUME` and `TASK_EXECUTION_RESUME_ID` onto the job. Read them only
  through `JobSuspends`: `isAwaitingApproval` (STOPPED + resume id + approval flag) and `isSuspendedForApproval`
  (the flag alone, any status). Read expiry and raised-at through `SuspendUtils`. `SuspendTaskExecutionPostOutputProcessor`
  schedules a one-time timer at `expiresAt`.
- **Links.** `ActionContextAware.getApprovalLinks(expiresAt)` returns `ApprovalLinks`:
  - a signed resume id whose TTL is the expiry plus `ApprovalFormUrls.APPROVAL_TOKEN_TTL_MARGIN` (1h);
  - `resumeUrl` = `/job/resume/{token}`;
  - `formUrl` = `/resume/{token}`;
  - `shortResumeId`, minted in-process by `ActionContextImpl` through `ApprovalShortTokenService` (the
    `approval_short_token` table, always in the default tenant's schema, deduplicated by the token's SHA-256, purged
    hourly). `ApprovalChannelFunction.SHORT_RESUME_ID` hands it to Telegram and Discord, whose button payload
    (`ApprovalButtonPayload`, `<shortId>:a|:d`) cannot hold the signed token.

  If minting fails, the id is null and channels fall back to form links. Distributed EE has only the stub
  `RemoteApprovalShortTokenService`, which throws, so it always falls back. The anonymous
  `POST /approval/short-token` remains for external callers.
- **Channel fan-out is best-effort.** `deliverToChannels` tries every channel and fails the step only when every
  channel fails, and the step error lists each channel's root cause. `ClusterElementDefinitionServiceImpl.executeApprovalChannel`
  counts deliveries in `bytechef_approval_request` and failures in `bytechef_approval_delivery_failure`.
  `TokenRefreshAspect` refreshes only the connection of the failing component, and surfaces the HTTP error when it
  cannot refresh.
  The step passes server-owned parameters (`EXPIRES_AT` ISO-8601, `PUBLIC_URL`, `RESUME_ID`, `RESUME_URL`,
  `SHORT_RESUME_ID`), which override the channel's own parameters. 15 elements: chat, Slack, Discord, Telegram,
  Mattermost, Rocket.Chat, Gmail, Outlook 365, SMTP email, WhatsApp (Meta), Twilio SMS/WhatsApp, Infobip SMS/WhatsApp,
  approval task.
  - The approval-task channel maps `EXPIRES_AT` to the row's `dueDate`.
  - Mattermost and Rocket.Chat escape characters that form markdown links.
  - **Chat** (`chat/chat`, on the chat component) sends an `__eventType: approval_request` data event
    (`ApprovalRequestEvents`) over the `SSE_STREAM_EVENTS` route. It throws unless the job carries `STREAMED_EXECUTION`,
    which only the chat-trigger streaming path sets, so a non-chat run needs a fallback channel.
  - One-click `?approved=` links NEVER auto-submit: `ApprovalForm` shows a pre-selected Confirm view, so link
    scanners cannot resolve approvals.
- **In-place resolution (5 interactivity controllers** in `platform-webhook-rest-impl`, anonymous, permit-listed by
  `WebhookAuthorizeHttpRequestContributor`). The tenant always comes from the resume id, and each controller then
  verifies the callback against that tenant's connections:
  - Slack `/slack/interactivity`: `signingSecret` on the connection. The `X-Slack-Signature` HMAC is computed over the
    **raw request body** (read from the stream, not re-encoded form params). The message is rewritten through
    `response_url`.
  - WhatsApp (Meta) `/whatsapp/interactivity`: `appSecret`, `X-Hub-Signature-256`. The GET handshake checks
    `bytechef.webhook.whatsapp.verify-token`.
  - Telegram `/telegram/interactivity`: `webhookSecretToken`, `X-Telegram-Bot-Api-Secret-Token`.
  - Discord `/discord/interactivity`: Ed25519 check against `bytechef.webhook.discord.public-key`, and `PING` is
    answered with `PONG`.
  - Mattermost `/mattermost/interactivity`: unsigned, so no `approvedBy`.

  On `GONE`, `ApprovalResolutionLookup.describe` words the reply by how the approval closed: `EXPIRED` (the job's
  `expiredJobResumeId` marker), `RESOLVED` (consumed id, or the run moved past it) or `UNAVAILABLE` (cancelled via the
  `cancelledJobResumeId` marker, failed, or not found). Twilio/Infobip and Rocket.Chat stay on URL buttons.
- **Resume contract** (`JobResumeController` `/job/resume/{id}` → `JobResumeFacadeImpl`):
  - An approval resume requires a **signed** token, whatever `bytechef.approval.signed-token.required` is set to, and
    `approved` must be a JSON boolean. Otherwise the result is `INVALID_ID` → 400.
  - A **GET** on an approval resume id answers **405** (`Allow: POST`) and never resolves it. GETs on non-approval
    suspends (wait webhooks) still work. A **HEAD** of any resume link answers 405 and resumes nothing.
  - A streaming resume (`Accept: text/event-stream`) is allowed only when the job has `STREAMING_RESUME` or
    `STREAMED_EXECUTION`; otherwise 406.
  - Other outcomes: `GONE` 410 (consumed, expired, or lost `tryClaimResume`), `JOB_FAILED` 422, `NOT_YET_SUSPENDED`
    409 after a 10s backoff retry. A job suspended for a sub-workflow returns `NOT_YET_SUSPENDED` and resumes only
    itself.
  - **Restart** of an approval-suspended run: `JobApiController.restartJob` → **409**. Restarting a failed run clears
    its previous error.
  - **Stop** → `JobResumeFacade.cancelApproval`: CANCELLED, resume id consumed and recorded as cancelled
    (`JobSuspends.closeCancelledApproval`), `JobApprovalClosedEvent`. Falls back to `stopJob` when the run is not
    waiting on an approval.
  - Every resolution metric is `bytechef_approval_resolution{approved}`.
- **Expired approvals FAIL the run, with the reason stored on the job error.** There are three paths:
  - A resume after expiry fails the run and answers `GONE` (410), `bytechef_approval_expired{source=resume}`.
  - The suspend timer → `JobResumeFacade.resumeExpiredJob` → `failExpiredApproval` (`source=timer`). Non-approval
    suspends are still resumed without data.
  - `ApprovalExpiryMonitor` (platform-coordinator, per tenant, every 15 min over `getStaleJobs(STOPPED, now)`):
    `source=sweep`, plus the `bytechef_approval_pending` gauge.

  Every path fails through `JobSuspends.failExpiredApproval`, which spends the resume id and records it as
  `expiredJobResumeId`; the resume and timer paths also fail the suspended task execution. All paths that fail or
  cancel publish `JobApprovalClosedEvent`, and `ApprovalTaskExpiryListener` marks that job's open rows EXPIRED.
- **Reminder/escalation act only on approval suspends** (`JobSuspends`), every 15 min, through
  `ApprovalNotificationDispatcher`, which counts a send as delivered when any subscribed `Notification` succeeded.
  - `ApprovalReminderMonitor` fires `JOB_APPROVAL_EXPIRING` within `lead-time` of expiry.
  - `ApprovalEscalationMonitor` fires `JOB_APPROVAL_ESCALATED` once `after` has elapsed since `approvalRaisedAt`
    (falling back to the task's startDate) and the approval has not expired. `after` has no default, so the sweep
    does nothing until it is set.
  - Handlers: `Approval{Reminder,Escalation}{Email,Slack,Webhook}NotificationHandler`.
  - Each claims a job-metadata marker (`approvalReminderSentAt` / `approvalEscalatedAt`) BEFORE sending and keeps it
    only when something was delivered; it is released on failure and when no notification is configured, so the next
    sweep retries.
  - `SuspendTaskDispatcherPreSendProcessor` clears both markers on resume, so they apply per suspend.
  - Notification, inbox, MCP and A2A form links are signed to expire with the approval.
  - `NotificationEvent.Type` ordinals are append-only: escalated is ordinal 7, notification_event id 8.
- **Approval-task rows** (`ApprovalTask.Status`: OPEN, IN_PROGRESS, COMPLETED, EXPIRED, appended in that order) close
  three ways:
  - `ApprovalTaskCompletionListener` on `JobResumedEvent` → COMPLETED.
  - `ApprovalTaskExpiryListener` → EXPIRED.
  - `ApprovalTaskReconciliationMonitor` (automation-task-service, 15 min,
    `bytechef.automation.approval-task.reconciliation.enabled`) catches what the listeners miss: COMPLETED run or
    moved-on resume id → COMPLETED; FAILED/CANCELLED/purged run, or an unparseable token past `dueDate` → EXPIRED.

  The **pending-approvals inbox** (`ApprovalTaskFacade.getPendingApprovals` → `pendingApprovals` GraphQL →
  `PendingApprovalsList`) lists only `isAwaitingApproval` runs, with form links signed to the approval's expiry.
  Distributed EE: worker-app's `RemoteApprovalTaskFacadeClient` makes real calls to configuration-app's
  `/remote/approval-task-facade`. configuration-app hosts `automation-task-service`, which includes the reconciliation
  monitor and listeners.
- **The hosted form** is the SPA route `/resume/{token}`, which loads `/api/platform/internal/approval-form/{id}`
  (permit-all). In multi-tenant mode `MultiTenantInternalFilter` passes that path through without a session, and
  `ApprovalFormFacadeImpl` takes the tenant from the signed token. It returns a form only while the job is STOPPED on
  that exact resume id. Load failures are 500.
- **SSE.** `SseStreamBridgeRegistry` keys open streams by **(tenantId, jobId)**, because job ids are unique only
  within a tenant, and routes each event by the tenant in its metadata. `SseEmitterStreamBridge` maps `__eventType`
  payloads to named events. On COMPLETED, `SseStreamApplicationEventListener` sends a named `result` event (taken from
  the `WEBHOOK_RESPONSE`-tagged task) before the terminal status event.
- **Sync callers cannot pause.**
  - `SuspendingTasks` tells the steps that can pause a run: `approval/`, `wait/` and `waitForApproval/` tasks.
  - `WebhookWorkflowExecutorImpl.executeSync` rejects a workflow with such a task anywhere in it, nested ones
    included, before the run starts (`ConfigurationException`, `SUSPENDING_TASK_IN_SYNC_EXECUTION`, unwrapped by
    `WebhookTriggerController` rather than surfacing as a 500).
  - `SyncExecutionSuspendRejectingTaskDispatcher` (registered in `WebhookConfiguration` and
    `WorkflowSubflowSyncExecutorConfiguration`) fails a sync run before such a step is dispatched, so the step never
    delivers anything. `SyncExecutionSuspendRejectingTaskCompletionHandler`, which must sit ahead of the suspend
    handler, fails any sync run that still suspends.
  - MCP workflow tools return an `approval_required` descriptor with `formUrl` and `jobId`, never the raw resume token.
    `ApprovalElicitingToolSpecifications` URL-elicits, or form-elicits with server-held tokens (at most 3 rounds), then
    `awaitApprovedWorkflowRun`.
  - A2A returns `input-required` with the form URL.
- **Sub-workflow suspension.** An agent's sub-workflow tool suspends with `SUBFLOW_RESUME` (no expiry) when the run
  can pause (`SubflowToolSupport`), and otherwise runs synchronously. `AgentSubflowLauncher`/`AgentJobResumer` resume
  the parent. `JobCompletionAwaiter` and SSE status events (`METADATA_AWAITING_SUBFLOW`) treat that suspend as
  non-terminal. Nesting is capped by `bytechef.workflow.execution.agent.max-subflow-depth`.
  `ChildJobPrincipalFactoryImpl` links a child job to the deployment that contains the called workflow: the caller's
  own deployment when it contains it, otherwise the called workflow's project deployment in the caller's environment.
- **One suspend per round.** `SuspendableToolCallingManager` runs a round's calls one at a time. After a call
  suspends, the rest get `NOT_EXECUTED_TOOL_RESULT`, and a second suspend sentinel in one round is an error.
- **Deferred calls are re-issued.** `DeferredToolCallReplay.of(conversation)` replaces each `NOT_EXECUTED_TOOL_RESULT`
  in the suspended round with `DEFERRED_TOOL_RESULT`. It wraps the chat model so the first model response of the
  resumed turn re-issues those calls (ids suffixed `_deferred`), and they run without depending on the model to repeat
  them. This applies to every suspending tool.
- **No approval under a subagent.** A `requestApproval` tool under a subagent is rejected by
  `WorkflowValidator.validateApprovalPlacement` (a save-time error attributed to the node, recursively through nested
  task tools) and by `AiAgentUtilsTaskTool.checkSubagentTools` before the agent runs, because the suspend would be
  lost in the subagent's own ChatClient.
- **Chat-only channel validation.** `WorkflowValidator.validateChatOnlyApprovalChannels` adds a warning, attributed
  to the node, when a workflow with no `chat/` trigger has a task whose `approvalChannels` are all chat. Such runs
  pause with no live card and can be reached only through the inbox or the form.
- **Client.**
  - `ApprovalRequestMessage` (`data-approval-request` in `aiChatDataComponents`) renders buttons and a comment field;
    it embeds `ApprovalForm` only when the request has `inputs`.
  - `ApprovalResolutionContext` is provided only by the CE Chats page (`ChatRuntimeProvider`), which streams the
    continuation. Other surfaces use the resume mutation.
  - The `@bytechef/chat` SDK has its own `ApprovalCard` plus a `drainSseResponse` continuation.
  - A 410 reads as "expired or already resolved".
- **Config** (declared in `ApplicationProperties`; defaults from `application-bytechef.yml`):

  | Key | Default |
  |-----|---------|
  | `bytechef.approval.signed-token.secret` | unset; derived from the encryption key |
  | `bytechef.approval.signed-token.previous-secrets` | empty |
  | `bytechef.approval.signed-token.default-ttl` | `P30D` |
  | `bytechef.approval.signed-token.clock-skew` | `PT60S` |
  | `bytechef.approval.signed-token.required` | `false` (fail startup without a secret; approvals need a signed token regardless) |
  | `bytechef.webhook.discord.public-key` | unset |
  | `bytechef.webhook.whatsapp.verify-token` | unset |
  | `bytechef.workflow.execution.approval-expiry.enabled` | `true` |
  | `bytechef.workflow.execution.approval-reminder.enabled` | `true` |
  | `bytechef.workflow.execution.approval-reminder.lead-time` | `PT24H` |
  | `bytechef.workflow.execution.approval-escalation.enabled` | `true` |
  | `bytechef.workflow.execution.approval-escalation.after` | unset (sweep does nothing) |
  | `bytechef.automation.approval-task.reconciliation.enabled` | `true` |
  | `bytechef.workflow.execution.agent.max-subflow-depth` | `10` |

- **Only approval-scoped sweeps exist.** There is no orphaned-job recovery, no worker heartbeat, no per-run timeout
  and no retention monitor. A run that never resolves its approval ends only through expiry or stop.
