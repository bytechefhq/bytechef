# AI Hub phase 1b: workflow chats

Port the AI Hub's workflow chats from `0_732` onto `0_p_ai_hub`. Commit ticket `732`.

## Why

In EE the AI Hub replaces the CE Chats page, and the Chats nav entry is now CE only. The hub therefore has to be
where a user talks to a deployed workflow that has a chat trigger. On `0_732` this already works: the composer's
provider picker lists those workflows, picking one opens a hub chat bound to it, and every turn runs the workflow
instead of an LLM. The behaviour is defined by `0_732`; this phase ports it, it does not redesign it.

## What a user gets (as on 0_732)

- The composer's provider picker (on the home panel and in a chat) has a "Workflow chats" cascade with a search
  filter. It lists the current workspace's chat workflows in the current environment, labelled
  `projectName — workflowLabel` (the em dash is `0_732`'s UI string and is also the persisted chat title; the
  no-em-dash rule covers prose only).
- Picking one creates a new AI Hub chat of kind `WORKFLOW_CHAT` and opens it at `/automation/ai-hub/chats/{id}`.
- In that chat each message runs the workflow through its chat trigger and the workflow's output streams back as the
  assistant reply. Streaming triggers stream; others return when the run ends.
- When the workflow asks the user a question (`ask_user_question`), the reply resumes the paused run instead of
  starting a new one.
- Stop ends the stream at once and stops the running workflow job when the run is on the streaming path. On the
  non-streaming path the job id is never known, so Stop ends the stream only and the chat stays held until the job
  finishes or the drain timeout passes (same on `0_732`).
- The chat is a normal hub chat: listed in the sidebar with a workflow icon and the workflow's live label as tooltip,
  kept across reloads, renamed, archived and deleted like any other. Bulk archive of workflow chats exists as a
  server mutation only, as on `0_732`, which has no UI for it.
- A workflow chat has no model picker, no suggestion chips, no `@` resource mentions, no artifacts card and no
  artifact recording; its header shows a workflow badge.
- At most one turn runs per chat: a second message gets the phase 1 turn-in-flight 409 and its toast. Turns are rate
  limited per chat by `WorkflowChatGuard`, which reports a friendly run error.
- The hub agent can list chat workflows, create a workflow chat and open its tab (`listChatWorkflows`,
  `createWorkflowChat`, `openWorkflowChatTab`) in BUILD mode; ASK gets `openWorkflowChatTab` only.

## Scope

Ported from `0_732`, file names kept identical (see the phase 1 design for the naming rule):

Server (`server/ee/libs/ai/ai-hub/` unless noted):
- `AiHubChatKind` (`STANDARD`, `WORKFLOW_CHAT`, same ordinals as `0_732`; `AGENT_CHAT` omitted) with
  `isWebhookBridged()`.
- `AiHubChat`: `kind`, `workflowExecutionId`, `projectDeploymentId`.
- Changelog `20260502000001_ai_hub_chat_add_workflow_chat_kind.xml` as on `0_732` (`kind` INT default 0,
  `workflow_execution_id`, `project_deployment_id`). A separate file, not an in-place edit, so existing dev databases
  migrate without hand patching.
- Service and GraphQL: `createWorkflowChat` / `createWorkflowChatAiHubChat`, `cancelWorkflowChatTurn`,
  `bulkArchiveWorkflowChatAiHubChats`; `kind`, `workflowExecutionId` and `projectDeploymentId` on the `AiHubChat`
  type.
- Agents: `AiHubRoutingAgent` becomes the `ai_hub_ask` / `ai_hub_build` `LocalAgent`, routing `WORKFLOW_CHAT` turns to
  `WebhookBridgeAgent` and everything else to the existing `AiHubSpringAIAgent`. The LLM agent beans get
  `@Bean(defaultCandidate = false)` as on `0_732`, so they leave the `List<LocalAgent>` and their ids do not collide
  with the router's (several consumers build an id map with no merge function and would fail at startup). Plus
  `AgUiStreamBridge`, `WorkflowChatGuard`, `WorkflowChatJobRegistry`, `WebhookResumeRegistry`,
  `metric/WorkflowChatMetrics` (self-contained Micrometer counters, ported as is).
- Tools: `ListChatWorkflowsToolCallback`, `CreateWorkflowChatToolCallback`, `OpenWorkflowChatTabToolCallback`. This
  branch has no tool catalog or `searchTool`, so all three are pinned: BUILD gets all three, ASK gets
  `OpenWorkflowChatTabToolCallback` only. The BUILD prompt's "Workflow execution" section is ported without
  `runChatWorkflow` and with its "find it with searchTool" line rewritten to name the pinned tool.
  `AiHubConfigurationToolSetTest`, `AiHubTurnToolDispatchTest` and `docs/agents/ai-hub.md` follow.
- Listing: `0_732`'s `ProjectDeploymentFacade.getWorkspaceChatWorkflows(workspaceId, environmentId)` with its
  `ChatWorkflow` record (`projectWorkflowId`, `workflowId` added) and `ProjectDeploymentFacadeChatWorkflowTest`
  (automation-configuration-service); the GraphQL controller delegates to it, as on `0_732`. `HostedChatTriggers`
  (platform-configuration-api) replaces the inlined `hasHostedChatTrigger`. Filters kept: enabled deployment and
  deployment workflow, hosted chat trigger, resolvable webhook execution id, plus the permission rule below.
- Cache registrations in `server/libs/config/cache-config/.../CacheConfiguration.java`, Redis and Caffeine, as on
  `0_732`: `WebhookResumeRegistry.resumeUrls` (30 min), `WorkflowChatJobRegistry.jobIds` (30 min),
  `WorkflowChatGuard.lastTurn` (5 min). The `inFlight` cache is not registered (see Stop below). The three classes
  switch from `@ConditionalOnProperty(bytechef.ai.hub.enabled)` to this branch's `@ConditionalOnAiHub`.
- `ai-hub-service/build.gradle.kts` gains the modules the ported code imports: `platform-webhook-api`,
  `atlas-execution-api`, `atlas-configuration-api`, `platform-component-api`, `platform-configuration-api`,
  `:sdks:backend:java:component-api`, `platform-ai-api`, `io.micrometer:micrometer-core`.

Client (`client/src/ee/pages/automation/ai-hub/` unless noted):
- `ModelPicker` already has the workflow-chat props; wire them through `hooks/useAiHubChatLaunchers.ts` into
  `AiHubHomePanel` and `AiHubPanel`.
- `chats/api/chats.api.ts`: `ChatKindType`, `isWebhookBridgedChat`, mapping of `kind` / `workflowExecutionId` /
  `projectDeploymentId`.
- Composer, panel, sidebar, `AiHub.tsx` (artifact recording off), `messages/AiHubThread.tsx` (suggestions off),
  `artifacts/useAiHubArtifactsCard.ts`, `chats/hooks/useRecordReferencedArtifacts.ts`,
  `chats/hooks/useLiveWorkflowLabel.ts`, `chats/hooks/useAiHubChatActions.ts` (cancel), the runtime provider's
  `ask-workflow-question` handling and the `openWorkflowChatTab` tool result.
- GraphQL documents: new `createWorkflowChatAiHubChat.graphql` and `cancelWorkflowChatTurn.graphql` under
  `client/src/graphql/ai/aihub/chat/`, `workspaceChatWorkflows.graphql` and
  `workflowChatProjectDeploymentWorkflow.graphql` under `client/src/graphql/automation/configuration/`; the existing
  `aiHubChats.graphql`, `createAiHubChat.graphql` and `updateAiHubChat.graphql` select `kind`, `workflowExecutionId`
  and `projectDeploymentId`. No client document for `bulkArchiveWorkflowChatAiHubChats`. Codegen regenerated.

## Not in this phase

- Agent chats (`AGENT_CHAT`, `aiAgentId`, `workspaceChatAgents`, `createAgentChat`).
- `runChatWorkflow` (`RunChatWorkflowToolCallback`, `runtime-providers/workflowStreamHandler.ts` and the
  `runChatWorkflow` branch of the runtime provider). It runs a chat workflow inline from a standard chat and is a
  separate feature; its BUILD prompt paragraphs and tool listing are cut.
- Attachments in workflow chats. On `0_732` the bridge and the router store them as asset files
  (`AssetFileSystemFacade`), which belongs to the files phase and is not on this branch. The router's attachment
  upload block and the bridge's attachment handling (`MAX_ATTACHMENT_BYTES`, `extractAttachments`,
  `createFromUpload`, `recordAttachmentFailure`) are dropped; the request body carries an empty `attachments` list.
  The composer keeps attachments off for workflow chats, as it already does on `0_732` for the drop zone. My call.
- Approval and wait steps inside a chat workflow. `0_732` routes such workflows to the streaming path through
  `WebhookWorkflowExecutor.hasSuspendingTask` and renders an approval card from
  `AiAgentSseEventType.APPROVAL_REQUEST`; neither exists here, both come with the HITL approvals work. Only
  `hasStreamingTask` decides the path and the approval-card branch of `AgUiStreamBridge` is dropped. My call.
- Any change to the CE Chats page. The listing gate below also runs for the CE page, which shares
  `workspaceChatWorkflows`; in CE the permission service only requires an authenticated user, so nothing changes
  there.

## Fitting this branch's rules

`0_732` predates two things phase 1 added, so the bridge is adapted rather than copied where it meets them.

**Stop and delete (my call).** A workflow-chat turn follows the phase 1 run rules in `InFlightAiHubRunRegistry`, like
any other turn: one run per chat, the turn-in-flight 409, Stop by `cancel(threadId, runId)` including the early-Stop
tombstone, and `retireThread` on delete. The registry has no per-run callback, so the bridge subscribes to
`stopRequested(threadId)` the way `RunStopAdvisor` does, and stops the workflow job (`WorkflowChatJobRegistry` lookup,
`JobFacade.stopJob`) as soon as it both knows the job id and has seen the stop. One subscription covers the Stop
click, chat deletion, and a Stop that lands before the `start` event delivers the job id. Timing: Stop closes the SSE
stream at once; the chat stays held until the bridge reports the run finalized, which happens when the job's terminal
status reaches the stream, normally right after the stop, or at the latest `DRAIN_TIMEOUT` (2 minutes) after the
cancel. `0_732` has the same shape with a 30-minute backstop. The client sends one `cancelAiHubRun` for every chat
kind; `cancelWorkflowChatTurn` stays as a server-side GraphQL entry point that goes through the same path, and the
client does not call it (the mutation has no `runId`, so it cannot tombstone). Deleting a workflow chat also clears
its pending `ask_user_question` slot in `WebhookResumeRegistry`. `WorkflowChatGuard` keeps only its rate limit; the
one-turn-per-chat rule comes from the registry.

**Who may list and run chat workflows (my call).** This branch has no project visibility filter, environment-scoped
permission or generated-workflow type, so `0_732`'s gate reduces to the part this branch has: the workspace scope
`WORKFLOW_VIEW` through `PermissionService.hasWorkspaceScope(workspaceId, "WORKFLOW_VIEW")`, refused for embedded
connected users. The check runs:
- on listing, in the facade method (`@PreAuthorize("hasPermission(#workspaceId, 'Workspace', 'WORKFLOW_VIEW')")`),
  which the GraphQL query and `ListChatWorkflowsToolCallback` both use;
- on creation, in the GraphQL mutation and the `createWorkflowChat` tool, which verify that the given
  `workflowExecutionId` is one of the workspace's chat workflows across all environments (a facade membership
  method), before anything parses the id (`WorkflowExecutionId.parse` is not hardened here);
- on every turn, in `AiHubApiController.chat` after `resolveChat`, on the request thread. The bridge runs on an
  executor without a security context, so the check cannot live there.
Bridged turns skip the EDITOR-for-BUILD mode check, so a user who may create a workflow chat can also talk to it
whatever the mode switch shows. The "no agent-generated workflows" filter is dropped because nothing on this branch
generates workflows.

**Conversation id for the workflow's own memory (user approved).** The chat trigger's output schema declares
`conversationId`, the field the CE Chats page sends, and the trigger passes the request body through unchanged.
`0_732`'s bridge sends `message` and `chatId`, so inside a hub workflow chat `${trigger.conversationId}` is null and
an AI Agent task's chat memory falls back to a fresh random id each turn. Here the bridge sends `conversationId` in
the body and in the parameters, and keeps `chatId` (the thread id, as on `0_732`) so workflows built against `0_732`
still work. The value is `workflow-chat:<threadId>`: stable per chat, so the workflow's memory carries across hub
turns, but distinct from the hub's own session id. The two must differ because the built-in Chat Memory component
builds its repository with the same `BuiltInSessionRepositoryFactory` as `AiHubSessionMemory`, in the same backend
(`bytechef.ai.memory.provider`) and sessions are looked up by id alone; with the thread id itself, a workflow using
the built-in memory would append its events into the session the hub renders, and the bridge's transcript write would
duplicate them. The hub's own transcript store (the bridge's `persistTurnToChatMemory` into `AiHubSessionMemory`) is
unchanged and separate from the workflow's memory; rewind and delete in the hub act on the hub session only, as on
`0_732`.

**Adaptations with no behaviour change.** `WebhookWorkflowExecutor.executeSync` returns the value directly here, not a
`CompletableFuture`, so the bridge's sync path calls it directly and catches `RuntimeException` instead of unwrapping a
`CompletionException`.

## Constraints

- Parity contract from phase 1 holds: workflow chats add no workflow-authoring path.
- Everything stays behind `@ConditionalOnAiHub` and the EE gates of phase 1.
- Commit when green, no push; ticket `732`; no explanatory code comments; no em dashes in prose.
- `bytechef-master` currently has `0_p_ai_hub` checked out and is where the user runs the app; work happens there
  only with a clean tree, or in a separate worktree.

## Verification

- Port the `0_732` tests for the ported classes (`AgUiStreamBridgeTest`, `AiHubRoutingAgentTest`,
  `WebhookBridgeAgentTest`, `WebhookBridgeAgentResumeTest`, `WebhookBridgeAgentTenantContextTest`,
  `WebhookResumeRegistryTest`, `WorkflowChatGuardTest`, `WorkflowChatJobRegistryTest`,
  `CreateWorkflowChatToolCallbackTest`, `OpenWorkflowChatTabToolCallbackTest`, `HostedChatTriggersTest`,
  `ProjectDeploymentFacadeChatWorkflowTest`) and the client tests that cover the ported client files; drop or adapt
  the parts that need agent chats, attachments, suspending tasks or `runChatWorkflow`. `WebhookBridgeAgentE2EIntTest`
  is a disabled stub on `0_732` and is not ported.
- New tests for the adaptations: the stop subscription stops the job (including a Stop before the job id arrives),
  the request body carries `conversationId` as `workflow-chat:<threadId>` and `chatId` as the thread id, the per-turn and create-time permission checks refuse, and the bridged
  turn skips the BUILD mode check.
- `./gradlew check` on every touched module, `npm run check`.
- Playwright: a deterministic spec for the picker cascade, chat creation, the composer differences and Stop; a live
  spec that deploys a project with a chat-trigger workflow whose AI Agent has chat memory keyed by
  `${trigger.conversationId}`, chats with it from the hub across two turns that need the memory, answers an
  `ask_user_question`, and stops a run.

## Left to the builder

- Exact order of the three hub tools inside the BUILD list (follow `0_732` where it pins them).
- The wording of the rewritten BUILD prompt lines.
