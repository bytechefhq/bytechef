# HITL Phase 3 — platform tool gate: implementation plan

*Companion to `docs/superpowers/specs/2026-07-21-agent-hitl-approval-chat-design.md` (D3, phase 3).
Code anchors verified 2026-07-21.*

## Verified machinery anchors

- **`SuspendableToolCallingManager`** (`components/ai/agent/.../tool/SuspendableToolCallingManager.java`):
  `executeToolCalls` delegates the whole round (`:77`), then inspects the shared agent
  `ActionContextAware.getSuspend()` (`:95`) — a suspending tool sets it via `context.suspend(...)`.
  Suspend path (`:112-126`): `findSentinelToolResponseId` locates the ToolResponse whose
  `responseData` equals `ToolSuspendConstants.SUSPENDED_SENTINEL` (throws on >1 suspend/round),
  re-suspends with `CONVERSATION_STATE` (`ConversationState.from(conversation)`) +
  `PENDING_TOOL_CALL_ID` added to continueParameters, returns `returnDirect=true`.
- **Tool list construction**: `AbstractAiAgentChatAction.getToolCallbacks` (`:708-793`) iterates
  `clusterElementMap.getClusterElements(TOOLS)`; per entry `AiAgentToolFacade.getFunctionToolCallback`
  reads `clusterElement.getParameters()` (the workflow JSON per-entry parameters map — where
  `requiresApproval` lives) and names the callback via
  `AbstractToolFacade.getToolName(componentName, elementName, toolParameters)` (platform-ai-api,
  `:207-237`: `TOOL_NAME` parameter override, else `COMPONENTNAME_ElementNameSnaked` uppercase).
  `workflowNodeName` is only the connection key, NOT the callback name. Wrappers already applied
  after facade construction: simulation-aware (`:761-770`), observable audit (`:776-792`).
- **Resume seam**: `AbstractAiAgentChatAction.buildPatchedRequestSpec` (`:317-344`) reads
  `CONVERSATION_STATE` + `PENDING_TOOL_CALL_ID` from continueParameters and calls
  `ConversationResume.patchPendingToolResponse(messages, pendingToolCallId, JsonUtils.write(data.toMap()))`
  — the human's `{approved, comment, ...fields}` submission becomes the tool response text, then the
  request spec re-runs `.call()`. Both sync and stream resumePerform share it.
- **Channel delivery precedent**: `ApprovalRequestApprovalAction.perform` (`:266-307`) — resolves
  `getResumeUrl()`, derives formUrl, executes APPROVAL_CHANNELS entries via
  `clusterElementDefinitionService.executeApprovalChannel(...)`, suspends with 60-day expiry.
  `ApprovalRequestApprovalTool` shows the tool-side wrapper: cast context to
  `ClusterElementContextAware`, `toActionContext(...)` (returns the LIVE agent ActionContext when
  present — `ClusterElementContextImpl.toActionContext` `:agentActionContext` branch), run the
  action perform, return `SUSPENDED_SENTINEL`.
- **Cluster element types on the agent**: `AiAgentComponentDefinition.getClusterElementTypes()`
  (platform-component-api, `:44-47`) returns `MODEL, CHAT_MEMORY, RAG, GUARDRAILS, TOOLS`. The
  editor renders sections purely from this list (`clusterElementsUtils.ts` iterates
  `clusterElementTypes`; `getFilteredClusterElementTypes` `:285-310`). Adding `APPROVAL_CHANNELS`
  here surfaces the "Channels" section on the agent node.
- **Audit**: agent-path audit = `createObservableToolCallback` (`AbstractAiAgentChatAction:246-304`)
  emitting component-local `ToolExecutionEvent(toolName, inputs, output, reasoning, confidence)` —
  has name+args+outcome, sufficient to log the approval decision. The platform
  `ToolExecutionRecorder` seam does NOT cover the agent surface (only MCP/embedded facades).

## Design

1. **Flag**: `requiresApproval: true` in the TOOLS cluster-element entry's `parameters` map
   (workflow JSON). Constant next to `ToolConstants.TOOL_NAME` (platform-ai-api).
2. **Gate wrapper** (`ApprovalGateToolCallback`, new, in components/ai/agent tool package),
   applied in `getToolCallbacks` around the facade-produced callback for flagged entries
   (outermost — outside observable wrapper is fine; decide once written; the observable wrapper
   should record the DENIAL outcome too, so gate goes INSIDE observable → observable sees either
   real result or denial text). `call(toolInput, toolContext)`:
   - Reads `ACTION_CONTEXT` (the agent's `ActionContextAware`) from ToolContext.
   - First call (no pending approval): builds formUrl from `getResumeUrl()`, executes the
     agent-level APPROVAL_CHANNELS cluster elements (empty channel list + chat-capable run →
     default to the chat channel; empty + no chat = loud IllegalStateException), then
     `context.suspend(new Suspend(Map.of(GATED_TOOL_NAME → name, GATED_TOOL_INPUT → toolInput,
     FORM_URL → formUrl), +60d))` and returns `SUSPENDED_SENTINEL`. The gate's approval "form" has
     no inputs — buttons + comment; formTitle = "Approve tool call: <name>", formDescription =
     pretty-printed args JSON (the reviewer sees tool name + AI-chosen arguments).
3. **Resume**: in `buildPatchedRequestSpec`, branch when continueParameters contain
   `GATED_TOOL_NAME`: parse `data` → `approved` + `comment`.
   - **Approved** → rebuild the tool callbacks (the method already has what it needs in the
     resume flow — pass the rebuilt `ToolCallback[]` in), find by name, `call(GATED_TOOL_INPUT,
     toolContext-with-ACTION_CONTEXT)`, patch the pending tool response with the REAL result.
   - **Rejected** → patch with `{"denied": true, "reason": "Denied by reviewer" + comment}` JSON
     so the LLM sees an explicit denial and can continue/replan.
   - Either way the loop resumes via the existing `.call()`.
4. **Agent channels section**: add `APPROVAL_CHANNELS` to
   `AiAgentComponentDefinition.getClusterElementTypes()`; `getToolCallbacks`/gate reads the
   agent node's channel entries from the same `ClusterElementMap`. Default rule: no channels
   configured → use the chat channel iff the run has a jobId (chat channel itself enforces its
   jobId check); otherwise loud failure at gate time.
5. **Tests**: gate wrapper unit test (suspend on first call, sentinel returned, channels invoked);
   resume branch test (approved executes + patches real result; rejected patches denial; comment
   included); tool-name mapping test (TOOL_NAME override respected); definition JSON regen for
   the agent component (clusterElementTypes change may alter snapshots).

## Constants (proposed, ToolSuspendConstants)

- `GATED_TOOL_NAME = "__bytechef_gated_tool_name__"`
- `GATED_TOOL_INPUT = "__bytechef_gated_tool_input__"`

## Open questions / watchpoints

- One-suspend-per-round invariant: if the LLM calls two flagged tools in one round, the second
  gate must NOT also suspend (SuspendableToolCallingManager throws on two sentinels). Gate should
  check `actionContext.getSuspend() != null` and, if already suspended this round, return the
  sentinel WITHOUT delivering channels/suspending again? No — sentinel without its own suspend
  corrupts findSentinelToolResponseId (finds 2). Safer: second flagged call in the same round
  returns a plain "deferred: another tool call is awaiting approval" tool response (not the
  sentinel) so the LLM retries it after resume.
- Editor environment: skip channel delivery (mirror the action's `isEditorEnvironment()` check)
  but still suspend? In the editor there is no resume webhook — follow AiAgentUtilsAskUserQuestionTool
  behavior (it suspends regardless; editor test surfaces handle resume via the test controller).
- The AI Hub copilot is out of scope (spec) — gate lives in the canvas AI Agent component only.
