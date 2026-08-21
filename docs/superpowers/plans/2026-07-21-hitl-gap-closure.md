# HITL gap closure — work plan (items 1–6)

*Follow-up to `docs/superpowers/specs/2026-07-21-agent-hitl-approval-chat-design.md` (all phases
done). Six gaps identified in review, user approved all. Execute in order.*

## 1. Editor UI toggle for `requiresApproval` (client)

- Flag is read server-side in `AbstractAiAgentChatAction.getToolCallbacks` from the TOOLS
  cluster-element entry's `parameters` map (`ToolConstants.REQUIRES_APPROVAL = "requiresApproval"`,
  platform-ai-api). Zero client references exist.
- Find where the cluster-element editor renders a selected TOOLS element's configuration:
  `client/src/pages/platform/cluster-element-editor/` (see `clusterElementsUtils.ts` for how
  sections render from `clusterElementTypes`). Add a "Requires approval" switch for elements of
  type TOOLS on the AI Agent root that reads/writes `parameters.requiresApproval` on the entry in
  the workflow definition (same update path the tool's other parameters use).
- Nice-to-have: shield badge on gated tool chips/nodes (lucide `ShieldCheckIcon`).

## 2. User docs + CLAUDE.md

- New `docs/content/docs/automation/human-in-the-loop.mdx`: Approval action + channels (incl. the
  chat channel + loud-failure rule), approve/reject with comment, form fields, the tool gate
  (`requiresApproval`), approval channels on the AI Agent node (chat default), continuation
  streaming, surfaces matrix (CE Chats, AI Hub workflow chat, canvas test chat, `@bytechef/chat`
  widget incl. inline field-less card), editor-run behavior. Mirror a2a-servers.mdx tone. Check
  docs nav registration (meta.json in that folder).
- CLAUDE.md: add an "Agent HITL approvals" section summarizing architecture + load-bearing names
  (`approval_request` event, `ApprovalGateToolCallback`, `ChatApprovalChannel`,
  `ApprovalResolutionContext`, `appendAiHubTaskAssistantMessage`, `hasApprovalTask`,
  GATED_TOOL_* continueParameters keys, single-suspend-per-round deferral).

## 3. Approval metrics (CE)

- Pattern: `PlanLimitRejectionCounter` (platform-rate-limit) — optional
  `ObjectProvider<MeterRegistry>`, no-op without registry.
- `bytechef_approval_request{channel}` — increment per channel delivery; cleanest single point:
  `ClusterElementDefinitionServiceImpl.executeApprovalChannel` (platform-component-service) tag =
  componentName/clusterElementName; needs a MeterRegistry-backed counter bean reachable there
  (constructor ObjectProvider).
- `bytechef_approval_resolution{approved}` — where resolution lands: `JobResumeFacade.resumeJob`
  impl (platform-workflow-execution) — data map contains `approved` for approval resumes (absent
  for ask-user-question resumes; only count when key present).

## 4. Gate decisions into the audit seam (CE emission, EE persistence)

- `ToolExecutionRecorder` (platform-tool-execution-api) currently NOT wired on the agent surface.
  Check its API first (`record(...)` wraps execution; there may be a direct outcome-record
  variant). Wire an `ObjectProvider<ToolExecutionRecorder>` through the AI Agent component
  handler → `AbstractAiAgentChatAction` → record two moments: gate raised (suspend) and gate
  resolved (approved-executed / denied) inside `resolveGatedToolResumeData`. Tool name +
  outcome only (the platform event deliberately excludes payloads).

## 5. End-to-end gate IntTest

- Precedent harness: `AiAgentStreamChatActionResumeIntTest`
  (server/libs/modules/components/ai/agent/src/test). Add a variant: agent with a
  `requiresApproval` tool → first perform suspends with GATED_* continueParameters and sentinel
  patched into conversation; resume with `{approved:true, comment}` executes the tool and patches
  the result; resume with `{approved:false}` patches the denial. Runs on the same in-process
  harness (no Docker needed) if that test doesn't require Testcontainers — verify.

## 6. Residuals

- (a) **Coordinator final-result event**: `SseStreamApplicationEventListener` (platform-coordinator)
  on COMPLETED job status: load job (JobService) + read outputs (TaskFileStorage), extract
  `__webhookResponse` → body → message (String); if present publish
  `SseStreamEvent(jobId, EVENT_TYPE_DATA, Map.of("event","result","result",Map.of("message",m)))`.
  MUST first verify async jobs actually persist `__webhookResponse` into Job.outputs — the sync
  path stores it explicitly (`WebhookWorkflowExecutorImpl` runSyncJob); async path unknown. If
  absent, the response-to-request action's output needs folding into job outputs on async runs
  (investigate `chat/responseToRequest` + coordinator webhook-response handling) — otherwise this
  item becomes "wire the fold first". Client: `AgUiStreamBridge` add `{event:"result"}` branch →
  ensureStarted + TextMessageContent delta + accumulate (persistence follows free). CE client
  already handles the named `result` event.
- (b) **MCP / A2A approval surfacing** (short of full elicitation): when a sync MCP tool run
  (`AutomationMcpToolFacade`) or A2A `message/send` (`AutomationA2AServerFacade`) ends with the
  job STOPPED + a pending job-resume id, return a clear structured message: "approval required —
  resolve at <formUrl>" instead of a timeout/empty result. Full MCP elicitation / A2A
  `input-required` status remains future work; note in spec when done.

## Conventions reminders

- buildSrc patch before gradle runs; revert before ending batch. Push ivicac both branches.
- Client: sort-keys, hooks order, `Icon` suffix, prettier; component snapshots regen when
  definitions change.
