# AI Hub phase 1 port (0_732 → 0_p_ai_hub) - Design

**Status**: Draft
**Date**: 2026-10-17
**Branch**: `0_p_ai_hub`, created from `0_p_1570`
**Source**: branch `0_732` in the same repository (working tree at `/Volumes/Data/bytechef/bytechef`)

---

## Intent

Bring the AI Hub to the `0_p_*` port line, restricted to what is needed to **manage projects, workflows
and project deployments from a chat**, and nothing more.

The goal that shapes every decision below: **building a flow through the management MCP server, the
workflow editor Copilot, and the AI Hub must give the same result for the same ask.** Three front doors,
one path. The hub therefore calls the *same tool beans* and the *same workflow-authoring sub-agent* the
other two doors already use on `0_p_1570`; it never carries a hub-flavoured copy of either.

The `0_732` AI Hub is ~35.6k server lines, 17 Liquibase tables and ~20k client lines, and depends on 23
Gradle modules that do not exist on `0_p_1570`. This port is a **hand-extraction**, not a cherry-pick: the
hub landed on `0_732` as a few squashed commits, so there is no commit sequence to replay.

## Goals

1. A full-page `/automation/ai-hub` (EE, `bytechef.ai.hub.enabled`) with persisted chats, an ASK/BUILD
   agent, a resource panel whose only tab kind is a workflow, an artifacts card, and a tool approval gate.
2. From that page a user can: list/search/create/update/publish/delete projects; list/search/create
   (empty)/delete workflows and **author workflow content through `buildWorkflow`** (and
   `importWorkflow`); list/create/update/delete/enable/disable/rollback deployments and promote a workflow;
   open a workflow the agent just made or found in the right-hand tab and edit it there.
3. Parity: the hub's BUILD agent holds `ProjectTools`, `ProjectWorkflowLifecycleTools`, the deployment
   callbacks from `DeploymentToolCallbacksFactory`, and the `buildWorkflow` / `importWorkflow` entries of
   `IntelligentToolCatalog`, exactly the beans `ManagementMcpServerConfiguration` and
   `copilotAgentToolCallbackContributor` / `deploymentFlatCrudMcpContributor` hand the MCP server. For that
   to be literally true, the management MCP server on this line moves from `ProjectWorkflowTools` to
   `ProjectWorkflowLifecycleTools` as well (see D0): the MCP door loses raw `updateWorkflow`, and its
   `createProjectWorkflow` becomes empty-only, so an MCP client authors content only through `buildWorkflow`,
   like the hub.
4. Every line ported is traceable to `0_732` so later phases (files, data tables, KBs, sharing, ...) can be
   ported on top without re-architecting.

## Non-goals (this slice)

Files / asset files, data tables, knowledge bases, skills, custom components, code workflows, hub MCP
servers, connectors, chat-tool attach (`ai_hub_chat_component` / `_tool`), workspace settings, sharing /
presence / `ai_hub_chat_turn`, scheduling, auto-memory, metrics, guardrails, LLM/tool usage, audit, workflow
chats and agent chats (`WebhookBridgeAgent`), the routing agent, pgvector tool search and the pinned /
catalog tool tiers, generative sub-agents (research, data analyst, image, slides), per-specialist sub-agent
conversation memory (`SubAgentSessionMemoryContributor`, `SubAgentGuardrailedChatClient`), voice /
push-to-talk, attachments, mount-time run resume, the `/status` and `/presence` endpoints.
Sub-agent **interactive questions** are IN (D13); only their memory sibling is out.

## Anti-goals

- **A second workflow-authoring path.** The hub's own LLM must never hold `ProjectWorkflowTools`
  (`updateWorkflow`). Port the `WorkflowContentSinglePathTest` guard with the hub surfaces.
- **Porting an excluded feature "because it was cheap".** Every excluded table, bean and client branch is
  excluded on purpose; a later phase brings it with its own design.
- **Drifting from `0_732` for taste.** Where `0_732` and `0_p_1570` disagree, prefer the `0_p_1570`
  shape (it is the port line's convention); where only `0_732` has an opinion, keep it as-is. Do not
  "improve" ported code.

## Constraints

- Repo conventions in `CLAUDE.md` apply (EE license header + `@version ee` under `server/ee/`, commit
  message format `<ticket> <desc>` / `<ticket> client - <desc>`, no explanatory inline comments, no em
  dashes in prose, `./gradlew check` + `npm run check` before claiming anything green).
- Enum ordinals are persisted as INT; never reorder an enum that reaches a column.
- New entities get a nullable `workspace_id BIGINT`, not a relation table.
- The commit ticket for this work is **`732`** (the AI Hub ticket), server and client alike.
- Target branch `0_p_1570` already carries: the extended CE `Source` enum (`PROJECT`, `PROJECT_DEPLOYMENT`,
  `DEPLOYMENT`, `ASSET_FILE`, ...), `CopilotChatFacade`, `WorkspaceScopedFlatToolCallback` /
  `WorkspaceScopedSubAgentToolCallback`, `AutomationToolInvocationContext`, `ScopedProjectGuard`,
  `IntelligentToolCatalog` with `buildWorkflow` / `importWorkflow`, `DeploymentToolCallbacksFactory` (7
  callbacks, registered by `ToolCallbacksFactoryConfiguration`), `ProjectTools`, `ReadProjectTools`,
  `ReadProjectWorkflowTools`, `ProjectWorkflowTools`, the Spring AI session store (`spring-ai-session`
  0.10.0, `BuiltInSessionRepositoryFactory`), `CatalogChatClientResolver`, `CatalogChatModel` (`@Primary`),
  `SecurityContextRehydrator`, `RehydrateContextToolCallback`, `ToolErrors`, `ApplicationProperties.Ai.Hub`
  (`enabled` only), the shared `ai-chat` client components, `ModelPicker`, `ModeSwitch`, the vendored
  `SpringAIAgent` (a superset of `0_732`'s).

---

## Decisions

### Where things land

**Anything shared that the Copilot touches lands on `0_p_1570` as Copilot work; only hub-specific code
lands on `0_p_ai_hub`.** `0_p_ai_hub` is rebased onto `0_p_1570` after each prerequisite batch.

Commit tickets follow the feature area, not the branch: Copilot-side commits `1570`, management-MCP /
shared tool commits `5539`, hub commits `732`.

On `0_p_1570` (D0, D13, D14): the workflow-authoring single path for the MCP, the artifact recorder SPI,
the context keys, `DelegatingToolCallback`, the sub-agent ask seam and its EE relay, the picker / card
parity work, and the shared client pieces (`toToolResultDataPart`, `aiChatDataComponents`, the card
components, `useAiChatToolCallStore` rename, `useReportQueryError`).

On `0_p_ai_hub` (D1-D12): the four hub modules, the hub pages and runtime, the approval gate and its
cards, `useApplicationInfoStore`'s `ai.hub` flag, routes and nav, the workflow tab's editor edits.

### D0. Prerequisite on `0_p_1570`: one workflow-authoring path for the management MCP

Before any hub code, and committed on **`0_p_1570`**:

- Port `ProjectWorkflowLifecycleTools` (122 lines on `0_732`, CE `automation-ai-tool`, `@Component`) with
  its three call-site adjustments for the target's `ToolContext`-taking `ProjectWorkflowTools` methods.
- Switch `ManagementMcpServerConfiguration` from `ProjectWorkflowTools` to `ProjectWorkflowLifecycleTools`:
  constructor, `toolCallbackProvider()`, `INSTRUCTIONS` (the "To build a workflow" paragraph already says
  `createProject → createProjectWorkflow → buildWorkflow`; it now also says `createProjectWorkflow` creates an
  empty workflow and content is authored only by `buildWorkflow` / `importWorkflow`), and its tests.
- Port `WorkflowContentSinglePathTest` from `0_732`
  (`server/apps/server-app/src/test/java/com/bytechef/server/config/`) so it asserts that no MCP surface
  holds `ProjectWorkflowTools`; the hub slice later extends it to the hub agents.
- Regenerate the management MCP instructions for the Claude Code plugin if this line has that generator
  (`./gradlew :claude-code-plugin:plugin-tools:generateMcpInstructions`); skip if the plugin is not on
  this line.

This is a deliberate contract change for MCP clients on this line: raw `updateWorkflow` and
definition-carrying `createProjectWorkflow` disappear from the MCP door. The commit message says so.

Also on `0_p_1570`, as separate small commits, because they are CE seams the hub plugs into and `0_732`
keeps them in CE modules (so file paths stay aligned for later phases):

- `WorkflowArtifactRecorder` (50 lines) and `ToolArtifactRecorder` (67 lines) SPIs in CE
  `automation-ai-tool`, plus the `ObjectProvider<WorkflowArtifactRecorder>` hook in `ProjectWorkflowTools`
  (`createProjectWorkflow` → created, `updateWorkflow` → updated; ~35 lines at the existing
  `capturePersistedWorkflow` / `captureUpdatedWorkflow` points). With no implementation on the classpath the
  provider is empty and nothing changes.
- The four extra keys on `AutomationToolInvocationContext` (`TOOL_CONTEXT_USER_ID_KEY`,
  `TOOL_CONTEXT_SOURCE_ORDINAL_KEY`, `TOOL_CONTEXT_LAST_USER_PROMPT_KEY`, `TOOL_CONTEXT_THREAD_ID_KEY`),
  constants only; the record's fields stay as they are on this line.
- `DelegatingToolCallback` (53 lines) in `components/ai/llm`'s `tool` package, where `0_732` has it.

Not brought to `0_p_1570` by this work, named so nobody wonders: the Copilot panel agents for projects
and deployments (`ProjectAgentConfiguration`, `DeploymentAgentConfiguration`,
`ProjectDeploymentAgentConfiguration`; a Copilot front door, not needed for the hub), per-specialist
sub-agent memory, the `AppCallableDraftGuard` / Apps changes in the sub-agent wrapper, and the
externalised `mcp-instructions.md` resource (the builder may adopt it while touching `INSTRUCTIONS`;
optional).

### D13. Sub-agent interactive questions (on `0_p_1570`, Copilot area, ticket `1570`)

A delegated specialist (`buildWorkflow`, `configureMcpServer`, ...) can put a real multiple-choice question
to the user instead of guessing. Ported from `0_732` with one deliberate relocation:

- **CE seam, as on `0_732`**, in `ai-copilot-tool`'s `com.bytechef.ai.copilot.tool.ask` package:
  `SubAgentAskRelay` (interface), `SubAgentAskRelayToolCallback` (wraps the delegate; when a question was
  raised, returns the question payload as the delegate tool's own result and discards the specialist's
  text), `SubAgentPendingQuestionSignal`, `SubAgentQuestionFormatter`, `SubAgentQuestionRenderer` (~370
  lines). `IntelligentToolDefinition#askCapable()` (default `true`; the converter / `importWorkflow`
  returns `false` because its prompt forbids asking) and the one edit in
  `IntelligentToolCatalog#buildToolCallback` that attaches the ask tool to every ask-capable delegate's
  `ChatClient` innermost and wraps the callback. `IntelligentToolSurfaceParityTest` pins the flag and its
  premise. With no relay bean present, nothing is attached and nothing is wrapped.
- **EE implementation relocated beside the Copilot**: `SubagentAskChannel` (thread-bound, LIFO-safe,
  at most one question per delegation), `SubagentAskChannelRelay` (the `SubAgentAskRelay` bean) and
  `SubagentAskUserQuestionToolCallback` (decorates the CE `AskUserQuestionToolCallback`, so the payload is
  byte-identical to the main agent's) move from `0_732`'s `com.bytechef.ee.ai.hub.subagent` to
  `com.bytechef.ee.ai.copilot.subagent` in EE `ai-copilot-service`, `@ConditionalOnEEVersion` only. Reason:
  the management MCP must ask the same way whether or not the hub is deployed, and a Copilot panel that
  delegates later inherits it. (~250 lines.)
- The management MCP `INSTRUCTIONS` keep `0_732`'s sentence telling the client that a specialist resumes
  with no memory, so the caller restates context when answering; `SubAgentQuestionFormatter`'s
  re-invocation sentence says the same.
- **Client**: nothing Copilot-specific. The shared `toToolResultDataPart` payload-kind fallback (D14)
  renders a delegate's question through the existing `AskUserQuestionMessage` in any runtime; the user's
  answer goes back as the next message, as for a main-agent question.

The Copilot panel agents on this line do not delegate, so the relay changes nothing for them today; it is
placed where it is for the MCP door and for future panels. The hub (D2) relies on it: its `buildWorkflow`
delegate is ask-capable by the catalog edit, with no hub-side wiring.

Details the builder must carry over with it:
- `AskUserQuestionToolCallback.KIND` becomes public (the relay checks it).
- Both MCP contributors (`ToolCallbackContributorConfiguration`, `EmbeddedCopilotMcpContributorConfiguration`)
  pass `SubAgentQuestionRenderer.PLAIN_TEXT`; panels and the hub use the default `JSON` renderer, whose
  envelope still carries `awaitingAnswer: true`, so this line's `endsTurn` ends the parent turn when a
  delegate asks, with no extra work.
- `SubAgentPendingQuestionSignal` is ported with the seam (the relay marks it), but
  `WorkspaceScopedSubAgentToolCallback` is **not** changed: on `0_732` the only reader of the signal there is
  the Apps callable check, which this line does not have. A later consumer re-adds the read.
- This line's `LookupPropertyTracker` rejects an ask that names an unresolved `lookupRequired` property with
  "call `selectPropertyOption`", a tool a delegate does not have. The specialist ask path is exempted from
  that check (the builder picks the narrowest exemption).
- **Prompt fix neither branch has:** the `buildWorkflow` sub-agent's prompt
  (`prompt_workflow_editor_build_sub_agent.txt`, this line only) tells it that it has no select / create
  connection tools: leave connections unbound, use `lookupComponentPropertyOptions` and `askUserQuestion`,
  and report unresolved lookups for the parent to finish. Today the shared build prompt instructs tools the
  delegate does not hold, which can end in "No ToolCallback found".

### D14. Interactive pickers and cards: one list for Copilot and hub (on `0_p_1570`, ticket `1570`)

**Finding.** The workflow-editor Copilot on `0_p_1570` already holds the full picker set
(`interactivePickerToolCallbacks()`: `listConnectionsForComponent`, `selectConnection`,
`lookupAction/TriggerPropertyOptions`, `selectPropertyOption`, `selectTriggerPropertyOption`,
`askUserQuestion`, `createConnection`) and the client renders all four cards through the shared `ai-chat`
module. In four places this line is **ahead** of `0_732` and stays that way: `awaitingAnswer` on picker
payloads + `CopilotSpringAIAgent.endsTurn` + `TurnEndingToolCallingManager` (the turn ends when a picker is
shown), `ScopedConnectionGuard` (connection-id entitlement), the `AllowedComponentNamesUtils` checks,
`LookupPropertyTracker`, `SelectPropertyOptionMessage` matching on `optionValue` (not label) and dimming
superseded pickers. None of these are "ported back" out.

**Decision: one picker list, built once, used by Copilot and hub.** Extract the body of
`CopilotConfiguration#interactivePickerToolCallbacks()` into a CE factory bean
(`InteractivePickerToolCallbacks` in `ai-copilot-service`, or a `@Bean List<ToolCallback>` with a
qualifier; builder's choice) with two views: `read()` = list + lookups + ask, `write()` = all eight.
`workflowEditorAsk/BuildSpringAIAgent` keep taking the full list as today; the hub ASK agent takes `read()`,
the hub BUILD agent `write()`. This is a deliberate drift from `0_732`, which pinned the hub's own unified
`SelectComponentPropertyOptionToolCallback` (one `selectPropertyOption` tool with `kind: ACTION|TRIGGER`):
that class is **not** ported. Same tool objects → same payloads, same guard, same `awaitingAnswer`, same
cards, in both chats. The hub agent's `endsTurn` override (D2) therefore stops on `awaitingAnswer` as well
as on the approval envelope.

**Shared client (`client/src/shared/components/ai-chat/messages/`):**
- `toToolResultDataPart.ts`: add `0_732`'s `toAskUserQuestionDataPart` extraction and the `readPayloadKind`
  fallback (~60 lines) so a delegate's `ask-user-question` payload renders whatever the tool name. The
  `tool-approval-request` branch is added by the hub branch (D10), the `queryKnowledgeBase` branch is not
  ported.
- `SelectConnectionMessage.tsx`: port the negative-id (virtual AI-provider connection) filter; **not** the
  visibility grouping (`ResourceVisibilityBadge` and the visibility model do not exist on this line). Keep
  this line's supersede dimming.
- `CreateConnectionMessage.tsx`, `AskUserQuestionMessage.tsx`, `SelectPropertyOptionMessage.tsx`: keep this
  line's versions (the `0_732` diffs are comments, a colour token, and a workspace-scoped tags query whose
  API this line does not have).
- `useAiChatToolCallStore`: rename `taskId → chatId` (`resetForChat`, `failRunningInChat`); no non-test
  callers of the old names exist here.
- `shared/error/useReportQueryError.ts`: new (45 lines, React + sonner).
- The hub runtime keeps `0_732`'s `hasLivePropertyOptionPicker` dedup locally (D10); moving it into the
  Copilot runtime is not part of this slice.

**Explicitly not promised, because `0_732` does not do it either:** no connection or property picker is
ever raised from *inside* a delegate (the channel carries only `ask-user-question`; connections and
lookups are the parent's job after `buildWorkflow` returns, per the hub build prompt's "Completing a built
workflow" checklist, which is kept); one question per delegation; a pick always comes back as the next
chat message, never as a tool result; no pickers on the MCP surface (plain-text questions only); no
pickers on the cluster-element or code-editor Copilot panels.

### D1. Module layout mirrors `0_732`

Four EE modules under `server/ee/libs/ai/ai-hub/`: `ai-hub-api`, `ai-hub-service`, `ai-hub-rest`,
`ai-hub-graphql`, with the same package roots (`com.bytechef.ee.ai.hub.{chat,approval,agent,config,tool,
security,util,web}`). Same file names as `0_732` wherever a file survives, so a later diff against `0_732`
is meaningful. Registered in `settings.gradle.kts` and `server/apps/server-app/build.gradle.kts` beside the
copilot EE modules (`ai-copilot-app` is NOT wired in this slice).

### D2. The agent

`AiHubSpringAIAgent` is ported and trimmed (target ~450 lines from 943), not re-based on
`CopilotSpringAIAgent`. Rationale: fidelity to `0_732` for later phases outweighs the ~150 lines saved; and
`CopilotSpringAIAgent.run` is final and keyed on state the hub controller does not write.

Kept: tenant binding (`AiHubAgentTenantBinder`), `runWithEnvironment` (minus `SubagentProgressEmitter`),
`advisorParams` (only `ChatMemory.CONVERSATION_ID`), `toolContext` (hub context + `AgentToolInvocationContext`
+ mode key), `resolveSelectedLlm`, chat-client resolution with override → default fallback,
`wrapToolCallback`, `buildInvocationContext`, `createSystemMessage` (active tab + referenced resources;
drop file tabs and the memory index), `lastUserPrompt`, `pinnedToolCallbacks()` (the approval facade
re-finds gated tools there), the builder minus memory / binding / usage / guardrails / workspace-prompt
collaborators, `NonEmptyMessagesAdvisor`, `SimpleLoggerAdvisor`.

Dropped: guardrails and workspace-prompt advisors, chat-bound tools, `AiHubModelUsageAdvisor`,
`MemoryIndexResolver`, `ChatToolBindingResolver`. `OverrideChatClientResolver` stays the **hub-local
nested interface** `AiHubSpringAIAgent.OverrideChatClientResolver`, as on `0_732`. It must not implement
the copilot's `com.bytechef.ai.copilot.agent.OverrideChatClientResolver`: copilot resolves that one with
`ObjectProvider.getIfAvailable()`, and a second implementation in the context would make every copilot
agent bean fail with `NoUniqueBeanDefinitionException` (or, if marked `@Primary`, silently route copilot
model overrides through the hub resolver).

**The hub requires the copilot.** `buildWorkflow` / `importWorkflow` are produced only when their sub-agent
chat-client factories exist, and those are beans of `CopilotConfiguration`, gated on
`bytechef.ai.copilot.enabled=true`; `IntelligentToolCatalog.getByNames` silently drops entries whose factory
is missing. So `AiHubConfiguration`, `AiHubApiController` and the GraphQL controllers are conditional on
**both** `bytechef.ai.hub.enabled` and `bytechef.ai.copilot.enabled` (`@ConditionalOnProperty` on each, or
one `@ConditionalOnAiHub` meta-annotation), `info.ai.hub.enabled` reports the effective value
(`hub.enabled && copilot.enabled`) so the client nav follows it, and `AiHubConfiguration` additionally fails
fast at startup if the BUILD tool list does not contain `buildWorkflow` (a misconfiguration must not boot a
hub that cannot author workflows). This is the same rule as the other agent configs on `0_732`: a config
that depends on another domain's toggle ANDs it in.

Two beans, `ai_hub_ask` and `ai_hub_build`, are the `LocalAgent`s directly (no routing agent, so no
`defaultCandidate = false`). Tool lists are built explicitly, never by sweeping
`ObjectProvider<ToolCallback>.orderedStream()`.

Turn history: `AiHubSessionMemory` (96 lines) is ported as-is; it is the memory the chat service reads,
truncates and appends through. The builder must verify the vendored `SpringAIAgent`'s own
`ToolCallingAdvisor` keeps history on when `SessionMemoryAdvisor` (order `HIGHEST + 400`) is attached, and
override `endsTurn` to stop on `"awaitingAnswer": true` (pickers, questions, including a delegate's relayed
question) exactly as `CopilotSpringAIAgent` does, and additionally on the approval envelope's
`"awaitingApproval": true`.

Model resolution: port `AiHubChatClientResolver` trimmed to ~90 lines and re-gate it on
`bytechef.ai.hub.enabled` (on `0_732` it is gated on the AI gateway, which does not exist here). It resolves
the user-selected provider/model from AG-UI state through `CatalogChatClientResolver`, falling back to the
default `ChatModel`.

### D3. Tool sets (the parity contract)

| Mode | Tools |
|------|-------|
| ASK | `ReadProjectTools`, `ReadProjectWorkflowTools`, `DeploymentToolCallbacksFactory.readToolCallbacks()`, the picker list's `read()` view (D14: list connections, lookups, `askUserQuestion`), `openWorkflowTab` |
| BUILD | ASK's plus `ProjectTools`, `ProjectWorkflowLifecycleTools`, `DeploymentToolCallbacksFactory.writeToolCallbacks()`, the picker list's `write()` view (D14: all eight), `buildWorkflow` and `importWorkflow` from `IntelligentToolCatalog` (`IntelligentToolVariant.BUILD`), each wrapped in `WorkspaceScopedSubAgentToolCallback` exactly as `copilotAgentToolCallbackContributor` does |

All tool callbacks pass through one wrap site, `AiHubToolCallbackWrappers.wrap(callback, rehydrator,
approvalGate)`: `RehydrateContextToolCallback` → `AiHubApprovalGate` → `NonEmptyToolCallback` → delegate.
`DelegatingToolCallback` arrives with D0 in `components/ai/llm`, at its `0_732` location.

`ProjectWorkflowLifecycleTools` arrives with D0 on `0_p_1570`; the hub only injects it.

The `buildWorkflow` delegate must forward the parent `ToolContext` so the inner `WorkflowEditorSpringAIAgent`'s
`updateWorkflow` runs with the hub's `conversationId` (thread id) and user. The builder verifies this on
`WorkspaceScopedSubAgentToolCallback` / the catalog factory on `0_p_1570`; if it does not forward, that is a
targeted fix in the catalog wrapper, not a hub-side workaround.

Workspace and environment reach the tools through `AiHubToolInvocationContext` → `AutomationToolInvocationContext`
(the target's is a two-field record: `workspaceId`, `environmentId`; the four extra key constants the hub
aliases arrive with D0).

`AiHubAgentType` (`AI_HUB_ASK`, `AI_HUB_BUILD`) + `AiHubAgentTypeProvider` (`@AutoService`) are ported so
`CurrentAgentContext.runWith` still names the hub; nothing reads it in this slice, but it is 90 lines and
later phases rely on it.

### D4. Chats

Entity `AiHubChat` trimmed to: `id`, `userId`, `threadId`, `title`, `lastPreview`, `messageCount`, `status`
(`ACTIVE` / `ARCHIVED` / `DELETED`), `environment`, `autoTitled`, `workspaceId`, `createdAt`, `updatedAt`.
`AiHubChatKind` is **not** ported (STANDARD is the only kind); no `kind` column.

`AiHubChatService` keeps `create`, `list`, `loadMessages`, `patch` (rename, archive / unarchive), `delete`
(cascades artifacts and approvals, purges the session), `truncateMessagesFrom`, `appendAssistantMessage`,
`cancelAiHubRun`, `findByThreadId`, `getWorkspaceId`, `getById` / `getViewable` / `getManageable`.
`TitleGenerationService` keeps the auto-title after the first turn, with a plain `ChatClient` (no
guardrails advisor).

Access policy and roles, stated once for the whole slice:

- **Chat access** (`AiHubChatAccessPolicy`): view = participate = manage = chat owner, or a user holding the
  tenant `ADMIN` authority. That is `0_732`'s rule minus visibility grants.
- **Turn dispatch** (`AiHubApiController`): the caller must be a member of the chat's workspace, checked with
  `PermissionService.hasWorkspaceRole(workspaceId, "VIEWER")` for ASK and `"EDITOR"` for BUILD, mirroring
  `CopilotChatFacadeImpl` on the target. BUILD creates and deletes projects and deployments, so VIEWERs do not
  get it; the facades behind the tools still enforce their own `@PreAuthorize`, this is the first line, not
  the only one.
- **Approval rules** (`AiHubToolApprovalRuleFacade`): list = workspace `VIEWER`; create / delete = tenant
  `ADMIN` authority. This matches the client's `canManage` (D10), which has no `myWorkspaceRole` query to
  consult on this line; a workspace-admin-only manage right is a later refinement, not a silent GraphQL-only
  power.
- **Approval resolution** (`AiHubToolApprovalFacade.resolve`): the chat owner (the only participant).

No `ResourceVisibility*`, `WorkspacePermissionScope` or `ResourceGrantService` here.

GraphQL (`ai-hub-chat.graphqls`, ~110 lines): `aiHubChats`, `aiHubChatMessages`,
`aiHubChatArtifactsByAiHubChat`, `createAiHubChat`, `updateAiHubChat`, `generateAiHubChatTitle`,
`deleteAiHubChat`, `truncateAiHubChatMessages`, `appendAiHubChatAssistantMessage`, `cancelAiHubRun`. No
visibility / participation / ownerName / authorName fields; the client operations are trimmed to match.

### D5. Artifacts

`ai_hub_chat_artifact` is ported with its entity, repository, service (minus the workspace-admin listing),
recorder and GraphQL (`recordReferencedAiHubChatArtifact`, `deleteAiHubChatArtifact` restricted to
`WORKFLOW_REFERENCED`). **`AiHubChatArtifactKind` is ported whole**, every value, so ordinals match `0_732`;
only `WORKFLOW_CREATED` (2), `WORKFLOW_UPDATED` (3) and `WORKFLOW_REFERENCED` (16) are produced. The
GraphQL enum mirrors the Java enum.

Recorder hooks (all direct calls, no advisor):
- `WORKFLOW_CREATED` / `WORKFLOW_UPDATED`: the `WorkflowArtifactRecorder` SPI and its hook in
  `ProjectWorkflowTools` arrive with D0; `WorkflowArtifactRecorderImpl` in the hub implements it. Because
  `updateWorkflow` runs inside the `buildWorkflow` sub-agent, the `conversationId` it sees must be the hub
  thread id (see D3).
- `WORKFLOW_REFERENCED`: `OpenWorkflowTabToolCallback` server-side, and the client's
  `recordReferencedAiHubChatArtifact` when a tab opens from the picker.

`AiHubChatArtifactRecorder` keeps extending `ToolArtifactRecorder` (which arrives with D0), as on `0_732`.

### D6. Tool approval gate

Ported: `AiHubToolApproval`, `AiHubToolApprovalRule`, both services, both repositories,
`AiHubToolApprovalPolicy(Impl)` (CATALOG rules only: `applyCatalogRules`; the COMPONENT and owner-flag passes
go with chat-tool attach), `AiHubToolApprovalDefaults` (the 14 names + `delete|drop|rollback|promote`
prefix rule; in scope that gates `deleteProject`, `deleteWorkflow`, `deleteProjectDeployment`,
`rollbackProjectDeployment`, `promoteWorkflow`), `AiHubApprovalGate`, `AiHubApprovalGateToolCallback`
(always CATALOG kind), `AiHubToolApprovalFacade(Impl)` with `findCallback` reduced to the PINNED lookup
over `ask/buildSpringAIAgent.pinnedToolCallbacks()`, `AiHubToolApprovalRuleFacade(Impl)` with
`@PreAuthorize` replaced by the checks in D4 (workspace VIEWER to list, tenant ADMIN authority to create /
delete), the two GraphQL controllers and `ai-hub-tool-approval.graphqls` unchanged.

Dropped from the slice: audit publisher, metrics, `ToolNameNormalizer`, `AiHubChatComponent` /
`AiHubChatTool` repositories, `ClusterElementToolCallback` detection.

**Continuation stays.** An approved tool executes from the approval mutation and its result must reach the
chat, so `AiHubChatStreamer` (232), `InFlightAiHubRunRegistry` (367), `AiHubRunState` (81) and the
`GET /ai/chat/ai_hub/{threadId}/attach` endpoint are in; the client's `attachToContinuation` (and the
~120-line `attachToInFlightRun` core of `inFlightRunClient.ts`) are in. The registry also backs
`cancelAiHubRun` and the controller's one-turn-per-chat gate. Fail-closed on persistence failure, one
pending approval per chat, `supersedePending` on the next turn: all as on `0_732`.

### D7. REST

`AiHubApiController` in `ai-hub-rest`, gated `@ConditionalOnEEVersion` + `@ConditionalOnCoordinator` +
`bytechef.ai.hub.enabled`: `POST /internal/ai/chat/ai_hub` (workspace check → thread check → one-turn gate →
supersede approvals → inject verified state → mode → agent → streamer; no `recordTurn`) and
`GET /internal/ai/chat/ai_hub/{threadId}/attach`. `/in-flight`, `/status`, `/presence` are not ported.
`TurnInFlightException` becomes a plain 409 with no `RunningUser`.

The hub does **not** register through `CopilotChatFacade` / `Source.AI_HUB`: that path checks only
workspace role, writes different state keys, and has no one-turn gate or continuation.

### D8. Database

Four tables, each as a **single collapsed init changeset** (the schema is unreleased on this line):

- `ai_hub_chat`: `id` PK autoIncrement startWith 1050, `user_id` BIGINT NN, `thread_id` VARCHAR(255) NN
  UNIQUE, `title` VARCHAR(255), `last_preview` TEXT, `message_count` INT NN default 0, `status` INT NN,
  `environment` INT NN, `auto_titled` BOOLEAN NN default true, `workspace_id` BIGINT, `created_at`,
  `updated_at` TIMESTAMP NN. Indexes: `(user_id, status, updated_at desc)`, `(environment)`.
- `ai_hub_chat_artifact`: as `0_732` init + environment: `id`, `chat_id` NN FK → `ai_hub_chat` ON DELETE
  CASCADE, `kind` INT NN, `artifact_id` VARCHAR(255) NN, `artifact_name` VARCHAR(255) NN, `metadata_json`
  TEXT, `created_at` NN, `status` INT NN default 0, `status_changed_at`, `environment` INT NN. Index
  `(chat_id, created_at desc)`.
- `ai_hub_tool_approval_rule` and `ai_hub_tool_approval`: column-for-column as `0_732`'s
  `20260902000001` / `20260902000002` (keep `tool_kind`, `component_name`, `component_version`,
  `connection_id` so the schema does not diverge from `0_732`).

One `includeAll` for `classpath:config/liquibase/changelog/automation/aihub` in
`liquibase-config/.../master.xml`, context `mono or configuration or multitenant`, placed where `0_732`
places it (the `asset_file` include it followed there does not exist here; place it after the last
`automation/*` include). No `aihub_execution` include. `AiHubJdbcRepositoryConfiguration` scans
`chat.repository` + `approval.repository` and is registered in `AutoConfiguration.imports`.

### D9. Configuration and properties

`bytechef.ai.hub.enabled: false` in `application-bytechef.yml`, `true` in `application-dev.yml` (this
line has no `application-local.yml`), and `info.ai.hub.enabled` in `application.yml` set to the effective
value (`hub.enabled && copilot.enabled`, see D2) so the client's `useApplicationInfoStore` reads one flag.
`ApplicationProperties.Ai.Hub` already exists on the target with `enabled`; nothing to add. No cache,
scheduler or Quartz wiring.

Prompts `prompt_ai_hub_ask.txt` and `prompt_ai_hub_build.txt` are rewritten for this tool universe:
projects, workflows (create empty → `buildWorkflow` with the workflow id and a plain-language
instruction; `importWorkflow` for n8n / Make / Zapier / Workato definitions), deployments and promotion,
`openWorkflowTab` after creating or finding a workflow, and the "Tool approvals" section from the `0_732`
build prompt. The workflow-building instruction text must say the same thing
`ManagementMcpServerConfiguration.INSTRUCTIONS` says (create project if needed → `createProjectWorkflow` →
`buildWorkflow`; each intelligent-tool call is independent and re-reads state).

### D10. Client

Everything lives under `client/src/ee/pages/automation/ai-hub/` with `0_732`'s file names. Per-file
scope from the survey, condensed:

- **Page**: `AiHub.tsx` (drop sidebar peek, bridged-chat check, pin button), `AiHubPanel.tsx` (drop
  sharing / presence / launchers / bridged badges), `AiHubResourcePanel.tsx` (workflow branch only; no
  file picker, no view-mode toggle), `AiHubHomePanel.tsx` (no launchers), `AiHubChatsSidebarToggle`,
  `AiHubErrorBoundary`, `AiHubWorkflowViewer`, `WorkflowTabLabel`.
- **Chats**: `AiHubChatsSidebar.tsx` (search, Active / Archived, paging, new chat, row menu; no "Shared
  with me", no thread-status probe), `AiHubChatActionDialogs` (no share), `api/chats.api.ts`,
  `hooks/useAiHubChatActions`, `useChats`, `useRecordReferencedArtifacts` (WORKFLOW_REFERENCED only),
  `useSwitchChat` (workflow-only artifact → tab), `useTruncateChatMessages`, `pendingToolApproval.ts`
  (owner only), `stores/useAiHubChatsStore` (no thread status / presence).
- **Composer**: `AiHubChatComposer.tsx` reduced to input, `ModeSwitch`, model picker slot, Send / Stop
  (`cancelAiHubRun`), pending-approval placeholder. No attachments, mic, skills, chips, typing presence.
  `useAiHubComposerStore` is not ported.
- **Messages**: `AiHubThread`, `AiHubMessage` (no author attribution), `AiHubMessageContent`,
  `AiHubSuggestionChips`, `AiHubToolCallRenderer` (keep the generic card machinery and the workflow
  entries of `ARTIFACT_OPEN_META`; drop subagent / run-chat-workflow / attach / memory bodies).
- **Artifacts**: all five files, trimmed to workflow kinds.
- **Runtime**: a dedicated trimmed `AiHubRuntimeProvider.tsx` (~1,100-1,200 code lines from 3,087):
  `HttpAgent` to `/api/platform/internal/ai/chat/ai_hub` with `threadId = chatId`, state injection
  (`workspaceId`, `environmentId`, `mode`, `currentTabs`, `activeTab`, selected LLM), optimistic turn
  markers, the subscriber (run error / finished / text / tool-call start, end, result; `buildWorkflow` /
  `importWorkflow` result → invalidate `ProjectWorkflowKeys` and `WorkflowTestConfigurationKeys`;
  `openWorkflowTab` → tabs store; `data-tool-approval-request` activity flag), `projectMessagesWithToolCalls`,
  `onNew` / `onEdit` / `onReload` / `onCancel`, `reRunAgentTurn`, `attachToContinuation`,
  `resolveToolApproval`, `ApprovalResolutionContext` provider with `resolveApproval` stubbed. Plus
  `stores/useAiHubRunStateStore`, `stripLeakedToolMarkup`, the `attachToInFlightRun` core of
  `inFlightRunClient`. Not a wrapper around `CopilotRuntimeProvider` (it is bound to one global
  conversation and has no cancel, tool-call cards or pre-turn chat creation).
- **Stores / hooks**: `useAiHubTabsStore` (workflow kind only, ~300 lines), `useAiHubStore` (no voice),
  `useAiHubSettingsStore`, `useResetAiHubChatOnEnvironmentChange`, `useResetAiHubStoresOnWorkspaceChange`.
- **Settings**: `settings/ToolApprovals.tsx` + its three components. `canManage` comes from the ADMIN
  authority (the `myWorkspaceScopes` query is not on the target).
- **Workflow tab**: `EmbeddableWorkflowEditor.tsx` (~250 lines, modelled on the target's `Project.tsx`
  provider value; no `WorkflowDataStoreProvider`, which is a no-op even on `0_732`; `useGetConnectionTagsQuery()`
  without a workspace id), plus small edits: `WorkflowEditorLayout` and `WorkflowEditor` gain
  `fitViewOnWorkflowChange`, `useWorkflowEditorCanvas` gains the fit-view effect (~20 lines),
  `ProjectHeader` gains `showPublishDeploy`, `showWorkflowSelect`, `onWorkflowChange` and the embedded gating
  (~35 lines). Keep the effect that resets the node-details, data-pill and test-chat singleton stores on
  `projectWorkflowId` change. The `useWorkflowTestRunGuard` ref fix is ported (34 lines).
- **Shared additions made by the hub branch** (hub-only by nature): `ai-chat/messages/ToolApprovalRequestMessage.tsx`,
  `ai-chat/approvalResolutionContext.ts`, the `tool-approval-request` entry in `aiChatDataComponents` and
  its branch in `toToolResultDataPart`, and `useApplicationInfoStore`'s `ai.hub.enabled`. Everything else
  shared (`toToolResultDataPart`'s payload-kind fallback, the card components, `useAiChatToolCallStore`'s
  `taskId → chatId` rename, `useReportQueryError`) arrives from `0_p_1570` via D14. `ModelPicker`,
  `ModeSwitch`, `Header`, `LayoutContainer` are used as they are on the target.
- **AI Hub is the default automation page** (user decision, 2026-10-17; new behaviour, `0_732` still
  lands on projects). The automation index route's loader (`routes.tsx`, today `redirect('projects')`)
  redirects to `ai-hub` when the hub is available (`edition === EE` and `ai.hub.enabled`, the same effective
  flag as the nav filter), and falls back to today's `projects` otherwise, so CE and hub-off installs are
  unchanged. The loader reads the flag from `useApplicationInfoStore.getState()`, which must be loaded before
  the router evaluates the loader (it is, on this line, for the existing `ai.knowledgeBase` checks; the builder
  confirms). The hub nav entry goes first in `automationNavigation`. Explicit links to `/automation/projects`
  keep working.
- **Routes / nav**: lazy `AiHub` + `AiHubToolApprovalsPage` in `routes.tsx`; `ai-hub` and
  `ai-hub/chats/:chatId` next to the automation `chats` route (`PrivateRoute` → `EEVersion` →
  `LazyLoadWrapper hasLeftSidebar`); `ai-hub/tool-approvals` in `currentWorkspaceSettingsRoutes` under a
  nested `{title: 'AI Hub', items: [...]}` group (the target's settings-nav shape); the nav entry goes
  first in `App.tsx`'s `automationNavigation` with an icon other than `MessagesSquareIcon` (Chats uses it
  there) and is filtered on `edition === EE && ai.hub.enabled`; `Settings.tsx` filters
  `ai-hub/tool-approvals` the same way.
- **GraphQL**: `client/src/graphql/ai/aihub/{chat,artifact,tool-approval}/` with the operations listed in
  D4-D6 (no sharing, chat-tool, mcp-server, workspace-settings dirs); `codegen.ts` gets
  `../server/ee/libs/ai/ai-hub/ai-hub-graphql/src/main/resources/graphql/*.graphqls`; operations and the
  regenerated `graphql.ts` are committed separately, per convention.

### D11. Tests

Port the `0_732` unit tests that cover surviving code, adapted to the trimmed shapes: agent builder /
system-message tests, approval policy / gate / facade tests (CATALOG paths), chat and artifact service
tests, the controller tests for the two kept endpoints, and an extension of the `WorkflowContentSinglePathTest`
ported in D0 so it also asserts that neither hub agent's tool list contains `ProjectWorkflowTools` and that
BUILD's contains the catalog's `buildWorkflow`. A startup test covers the fail-fast in D2 (hub on, copilot
off → context fails with a message naming both properties). Add a Testcontainers `*IntTest` that boots the four
repositories against the collapsed changelogs (that is how changelog edits are verified on this repo).
Client: port the tests of every kept file, trimmed with it; Playwright `tests/ai-hub` specs that only
touch sidebar / new chat / mode toggle are ported, the rest are not.

### D12. Documentation

A new `docs/agents/ai-hub.md` on this branch (that is where this line keeps its agent deep-dives;
`.agents/` holds only the coming-soon inventory), short, describing only what exists here (chats, tool
sets and the parity contract, the approval gate, the artifact recorders, the wrap site) and listing the
deferred phases by name with a pointer to the `0_732` specs. Add a one-line pointer to it under
`CLAUDE.md`'s "Agent skills" section, matching the existing `docs/agents/*` pointers there.

---

## Acceptance

1. Server: `./gradlew check` green; `./gradlew testIntegration` for the hub modules green against
   Testcontainers. Client: `npm run check` green.
2. With `bytechef.ai.hub.enabled=true` on EE, `/automation/ai-hub` renders; new chat → BUILD → "create a
   project called Demo with a workflow that posts 'hello' to Slack every morning" → the agent calls
   `createProject`, `createProjectWorkflow`, `buildWorkflow`, `openWorkflowTab`; the workflow tab opens
   in the editor with the built definition; the artifacts card lists `WORKFLOW_CREATED`,
   `WORKFLOW_UPDATED`, `WORKFLOW_REFERENCED` rows for that chat.
3. Parity check, done by hand and recorded in the PR: the same instruction through the management MCP
   (`createProject → createProjectWorkflow → buildWorkflow`) and through the workflow editor Copilot BUILD
   on an empty workflow yields a definition with the same trigger and task types and the same component
   versions as the hub's. Differences in parameter prose are acceptable; differences in structure are a bug
   in whichever door deviates from the shared tools. The MCP tool list no longer contains `updateWorkflow`
   (D0).
4. "delete the Demo project" → approval card renders, the tool does not run until approved; approve →
   the project is deleted and the result streams into the chat via the continuation; reject → nothing
   runs. A workspace `EXEMPT` rule for `deleteProject` lifts the gate; a `REQUIRE` rule for
   `publishProject` adds one.
5. Interactivity parity: in the hub BUILD chat, "build a workflow that posts to Slack" with two Slack
   connections in the workspace → after `buildWorkflow` returns, a `selectConnection` card renders, the turn
   ends, the pick is posted as the next message and bound via `saveWorkflowTestConnection`; the same ask in
   the workflow-editor Copilot panel renders the identical card. With an ambiguous instruction ("post to
   the channel"), the `buildWorkflow` delegate's question renders as an `AskUserQuestionMessage` in the hub
   and, over the management MCP, comes back as a plain-text question; answering and re-invoking continues
   the build.
6. Reload the page mid-chat: the transcript is restored from the session store with tool-call cards.
   Archive / unarchive / rename / delete chats work; deleting a chat removes its artifacts and approvals.
7. Opening `/automation` on EE with the hub enabled lands on `/automation/ai-hub`; with the hub off, or on
   CE, it lands on `/automation/projects` as today.
8. With `bytechef.ai.hub.enabled=false`, no hub beans load, no nav entry, no routes, and the copilot and
   management MCP behave exactly as on `0_p_1570` after D0. With `hub.enabled=true` and
   `copilot.enabled=false` the hub stays off the same way (the gate is an AND). The startup fail-fast
   (message naming both properties, D2) covers the remaining misconfiguration: copilot and hub on, but
   `buildWorkflow` missing from the BUILD tool list.

## Left to the builder

- Exact trimming inside each ported file, within the scope above.
- Order of work and commit granularity (one ticket, `732`).
- Whether `NonEmptyMessagesAdvisor` is worth its 355 lines in this slice (keep if the vendored
  `SpringAIAgent` can emit an empty messages list on the first turn; otherwise drop and note it).
- The hub nav icon.
- Whether `ToolExecutionRecorder` (present on the target) is wired into the approval facade's execute path.
