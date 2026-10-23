# AI Hub

The AI Hub is the full-page agent chat at `/automation/ai-hub` (EE). It manages projects, workflows and
deployments through chat. This describes the code on this line; the phase 1 design is
[the port spec](../superpowers/specs/2026-10-17-ai-hub-phase1-port-design.md) and the plan is
[Plan B](../superpowers/plans/2026-10-18-ai-hub-port-b-hub.md). Where they disagree with the code, the code wins.

Modules: `server/ee/libs/ai/ai-hub/{ai-hub-api,ai-hub-service,ai-hub-rest,ai-hub-graphql}`. Client:
`client/src/ee/pages/automation/ai-hub/`.

## Gate

Every hub bean is `@ConditionalOnAiHub`, which is `bytechef.ai.hub.enabled` AND `bytechef.ai.copilot.enabled`. The
one exception is the `AiHubJdbcRepositoryConfiguration` auto-configuration, which only enables the hub's Spring Data
JDBC repositories. The
`buildWorkflow` and `importWorkflow` delegates come from the Copilot's sub-agents, so `AiHubConfiguration`
(`requireBuildWorkflow`) fails startup if BUILD has no `buildWorkflow`. Turn dispatch is also
`@ConditionalOnEEVersion` and `@ConditionalOnCoordinator`.

`info.ai.hub.enabled` in `application.yml` mirrors only `bytechef.ai.hub.enabled`; the client applies the same AND
when it loads application info (`useApplicationInfoStore`), so the nav entry, the redirect and Settings stay hidden
while the Copilot is off.

## Default page

`loadAutomationIndexRedirect` in `client/src/routes.tsx` sends `/automation` to `ai-hub` when the edition is EE and
`ai.hub.enabled`, otherwise to `projects`. The nav entry is filtered the same way in
`client/src/shared/navigation/automationNavigation.ts`. Tool approval settings live at `ai-hub/tool-approvals`.

## Chats and artifacts

`AiHubChat` (table `ai_hub_chat`) is owner-only; `AiHubChatAccessPolicy` decides view, participate and manage.
History is not stored on the chat: `AiHubSessionMemory` keeps the session events keyed by the chat's `threadId`.
Chats are managed over GraphQL (`AiHubChatGraphQlController`, `ai-hub-chat.graphqls`).

Artifacts (`AiHubChatArtifact`, `AiHubChatArtifactKind`, shown in `AiHubArtifactsCard`) are written by two
recorders: `WorkflowArtifactRecorderImpl` (called when `buildWorkflow` or `importWorkflow` persists a workflow;
a no-op outside an AI Hub conversation) and `AiHubChatArtifactRecorderImpl` (used by `OpenWorkflowTabToolCallback`).
The client records `WORKFLOW_REFERENCED` through `useRecordReferencedArtifacts` when a tab opens from the picker.

## Tool sets and the three doors

`AiHubConfiguration` builds two `AiHubSpringAIAgent` beans, `ai_hub_ask` and `ai_hub_build`, with explicit tool
lists (never a sweep of every `ToolCallback` bean). ASK is read only: `ReadProjectTools`,
`ReadProjectWorkflowTools`, deployment reads, read pickers, `openWorkflowTab`, `openWorkflowChatTab`. BUILD adds
`ProjectTools`, `ProjectWorkflowLifecycleTools`, deployment writes, write pickers, the workflow chat tools
`listChatWorkflows` and `createWorkflowChat`, and `buildWorkflow` / `importWorkflow`.
Both modes also get, per turn, the Brave and Firecrawl tools of the web search providers enabled for the verified
environment (`WebSearchToolCallbackProvider` through `additionalToolCallbacks`). They are read only, skip the
approval gate and are not pinned, so an approval never resumes one.
The controller needs workspace role VIEWER for ASK and EDITOR for BUILD. Embedded connected users
(`ConnectedUserAuthentication`) are refused on the REST entry points and in `AiHubWorkspaceAccess` for GraphQL.

Parity contract: the Copilot, the Hub and the management MCP server (`ManagementMcpServerConfiguration`) author
workflow content only through the same `buildWorkflow` / `importWorkflow` specialist. No door holds raw
`ProjectWorkflowTools.updateWorkflow`; `WorkflowContentSinglePathTest` in `server-app` enforces it.

Workflow chats: the `ai_hub_ask` and `ai_hub_build` beans are `AiHubRoutingAgent`s that wrap the mode's
`AiHubSpringAIAgent`. A turn on a `WORKFLOW_CHAT` chat goes to `WebhookBridgeAgent`, which runs the chat's
workflow through its webhook trigger instead of an LLM; every other turn goes to the LLM agent. A workflow chat
turn needs the `WORKFLOW_VIEW` workspace scope in place of the EDITOR role, and creating one through GraphQL or
`createWorkflowChat` also requires `ProjectDeploymentFacade.findWorkspaceChatWorkflow` to find the workflow, and the
chat stores the project deployment id of the listing row it returns, not the one the caller sent. The workflow sees the
conversation id `workflow-chat:<threadId>`: its own chat memory keys on that id, which differs from the thread id
the hub session uses because both can live in the same session store. BUILD finds chat workflows
with `listChatWorkflows`, creates a chat with `createWorkflowChat`, and opens it with `openWorkflowChatTab`.

## Model resolution

The Hub does not resolve models itself. `AiHubChatClientResolver` (an `AiHubSpringAIAgent.OverrideChatClientResolver`,
hub-local so the Copilot's single-bean lookup is not broken) maps the run state onto Copilot state and delegates to
the Copilot's `OverrideChatClientResolver` (EE: `CopilotChatClientResolver`). Specialists resolve through
`SubAgentChatModelResolver`.

Remembered model rule (client, `buildStateToSend` in `AiHubRuntimeProvider.tsx`): send the chat's own selection when
both provider and model are set; otherwise the workspace's remembered model, only while the provider catalog still
offers it (`resolveRememberedModel` forgets a stale one). With no selection the environment default applies.

Run errors are coded. `CopilotRunErrorCode` (`NO_AI_PROVIDER`, `SELECTED_MODEL_UNAVAILABLE`,
`MODEL_SELECTION_INCOMPLETE`) is defined in `ai-copilot-service`; `AiHubSpringAIAgent.runErrorCode` maps the thrown
cause onto it and the code rides the RUN_ERROR event. The client maps only `NO_AI_PROVIDER` to a dedicated message
(`toRunErrorData.ts`, `RunErrorMessage.tsx`).

## The wrap site and the approval gate

Every tool callback is wrapped in one place, `AiHubSpringAIAgent.Builder.build()`, via
`AiHubToolCallbackWrappers.wrap`: `NonEmptyToolCallback` innermost, then `AiHubApprovalGate`
(`AiHubApprovalGateToolCallback`), then `RehydrateContextToolCallback` outermost. `wrap` is idempotent, so a second
registration does not nest the gate.

Rules (`AiHubToolApprovalPolicy.Decision.isGated`):
- Gated by default: `AiHubToolApprovalDefaults.DEFAULT_TOOL_NAMES` (`deleteProject`, `deleteWorkflow`,
  `deleteProjectDeployment`, `rollbackProjectDeployment`, `promoteWorkflow`) and the prefixes `delete`, `drop`,
  `rollback`, `promote` followed by an uppercase letter.
- A workspace rule (`AiHubToolApprovalRule`, mode `REQUIRE` or `EXEMPT`) adds to or lifts that. `EXEMPT` always wins.
- If the rule lookup fails, only the built-in defaults gate.
- The `buildWorkflow` / `importWorkflow` sub-agents never see a tool gated by default: the hub decorates their chat
  client with `AiHubGatedToolFilterAdvisor`, so destructive calls stay on the hub's own gated tools. The Copilot's
  use of the same sub-agents is unchanged.

A gated call creates a `PENDING` `AiHubToolApproval` and returns a `tool-approval-request` envelope instead of
running; a second gated call while one is pending gets a `deferred` result; a persistence failure returns a refusal
and the tool does not run. A gated call whose chat no longer exists, is in `DELETED` status or is being deleted
(retired thread) is refused without recording an approval. Approvals expire (`isExpired`).

Binding: `AiHubTurnEndingToolResultAdvisor` calls `AiHubApprovalGate.recordToolCallId`, so the approval holds the
model's tool call id. `AiHubToolApprovalFacadeImpl.resolve` re-checks `AiHubChatService.containsActiveToolCall`
against that id; a dropped call marks the approval `SUPERSEDED` and answers 409. Approving runs the pinned callback
(`AiHubPinnedToolCallbacks`) and starts a continuation run (`AiHubContinuationStarterImpl`). When another run holds the
thread, the continuation is queued on that run and starts on the worker executor once the run ends
(`InFlightAiHubRunRegistry.runWhenReleased`). The client is not attached to it (`continuationStarted` is false), so
its output shows on the next attach or reload. Stopping the holding run therefore starts the queued continuation.
The queue is in memory and best effort: a queued continuation is lost on restart and when the holding run never
reports that it stopped (a stall past the drain timeout or the entry TTL), and deleting the chat discards it.

Superseding: a new turn (`AiHubApiController.chat`), a truncation (edit or reload) and Stop
(`cancelAiHubRun`, for approvals with no recorded call id) each supersede pending approvals.

Display only: the same advisor stores the result of a turn-ending call (the approval request) as an archived TOOL
event tagged `aiHubDisplayOnly`. The thread shows it after a reload, and readers of archived events must skip it
(`AiHubTurnEndingToolResultAdvisor.isDisplayOnly`).

## Edit and reload

Every user message keeps the client's AG-UI message id (`stores/messageId.ts`) as its session row id.
`truncateAiHubChatMessages(messageId, keepMessage)` drops everything after that message (and the message itself for
an edit), then supersedes approvals whose calls it dropped; an already dropped id is a 409.
Client: `useTruncateChatMessages`.

## Stop and in-flight turns

`InFlightAiHubRunRegistry` is the one-turn-per-chat gate and the replay buffer behind
`GET /internal/ai/chat/ai_hub/{threadId}/attach`. Stop calls `cancel(threadId, runId)`: the run's sink completes and
`RunStopAdvisor` ends the model stream with `takeUntilOther(stopRequested)`. The cancelled run still holds the thread
(draining guard, `holdsThread`) until its agent reports it stopped or `DRAIN_TIMEOUT` (2 minutes) passes, so a new
turn or a truncation never overlaps a stopped run that is still writing. The runId is tombstoned so a Stop that beats
the turn's POST still stops it.

Deleting a chat registers its rollback hook, then retires its thread (`InFlightAiHubRunRegistry.retireThread`):
queued continuations are dropped, later ones, new runs and turns (404) are refused, and the live run is cancelled
the same way. The retirement expires with the run entries; a delete that rolls back lifts it. The chat, its
approvals and its session are then removed. After the commit the approvals are deleted again in a new transaction,
which removes a PENDING row the gate committed during the delete; a row the gate commits after that cleanup can
still remain (it never executes).

A turn or truncation against a held thread is refused: the turn POST answers 409 `{"error":"TURN_IN_FLIGHT"}`
(`TurnInFlightException`), truncation throws `ConflictException`. The client turns the 409 into a toast
(`TURN_IN_FLIGHT_MESSAGE`) and rolls back its optimistic messages.

## Playwright specs

`client/test/playwright/tests/ai-hub/` (`aiHub`, `aiHubApprovals`, `aiHubBuild`, `aiHubChats`, `aiHubLive`,
`aiHubModesAndRoles`, `aiHubNavigation`, `aiHubRegressions`, `aiHubWorkflowChats`, `aiHubWorkflowTab`), with
`pages/aiHubPage.ts`, `fixtures/aiHubChats.ts` and `utils/aiHubUtils.ts`. Run setup is in
`client/test/playwright/README.md`. `aiHubWorkflowChats` covers the picker's Workflow chats cascade and its search,
creating and opening a workflow chat, the controls a workflow chat drops (model picker, resources, `@` mention), its
header badge and sidebar icon, and Stop sending one `cancelAiHubRun`; `stubWorkflowChats` in `fixtures/aiHubChats.ts`
serves the chat workflows and turns a real chat into the workflow chat, so no deployed chat workflow is needed.

## Deferred phases

Not on this line; each is a later phase with its own design, to be ported from `0_732` (specs under
`docs/superpowers/specs/` on that branch):
- Tool approval gate extras (COMPONENT rules, owner flags): `2026-09-02-ai-hub-tool-approval-gate-design.md`.
- Shared sessions, presence, `ai_hub_chat_turn`: `2026-09-02-ai-hub-shared-sessions-presence-design.md`.
- Chat tool attach: `2026-05-14-ai-hub-autonomous-tool-attach-design.md`.
- Auto-memory: `2026-06-02-ai-hub-auto-memory-resource-fork-design.md`.
- Task templates and agents: `2026-08-12-ai-hub-task-templates-and-agents-design.md`.
- Embedded automation hub: `2026-08-17-embedded-automation-hub-design.md`.
- Files, data tables, knowledge bases, skills, custom components, code workflows, MCP servers, connectors: phase
  specs `2026-04-23-ai-hub-phase2-design.md` to `phase6.1`, `2026-07-17-custom-component-ai-hub-tools-sp-b-design.md`
  and `2026-07-18-code-workflow-ai-hub-copilot-cc-a-design.md`.
