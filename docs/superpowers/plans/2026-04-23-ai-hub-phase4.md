# AI Hub Phase 4 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Unlock in-tab editing for Phase 2's non-file tabs (workflow metadata/node properties, data-table rows/columns, knowledge-base document management) plus cross-domain mutation tool callbacks on the `ai_hub` agent, and introduce the `workflow_builder` subagent via Spring AI `TaskTool` — the second application of the mandatory subagent pattern.

**Architecture:** Each viewer gains an Edit toggle; when on, inline controls call existing workspace services directly (React Query mutations). The agent gains `update*`, `add*`, `delete*` tool callbacks that wrap the same services. A new `workflow_builder` subagent is configured exactly like Phase 3's `research` subagent — `ChatClient` + Firecrawl/mcp-tools + system prompt, wrapped by `TaskToolCallbackProvider`, registered on `ai_hub`. The parent decides autonomously when to delegate workflow-construction requests.

**Reference spec:** [docs/superpowers/specs/2026-04-23-ai-hub-phase4-design.md](../specs/2026-04-23-ai-hub-phase4-design.md).

**Depends on:** Phase 1 (merged), Phase 2 (merged — Phase 4 edits Phase 2 viewers), Phase 3 (merged — Phase 4 copies its TaskTool pattern).

---

## File structure

### Server (EE)

#### Mutation tool callbacks (one class + one test each)

| Path (under `ai-copilot-service/src/main/java/com/bytechef/ee/ai/copilot/tool/`) | Operation |
|---|---|
| `UpdateWorkflowToolCallback.java` | Update workflow definition (partial merge) |
| `CreateWorkflowToolCallback.java` | Create a workflow from a builder-produced JSON |
| `AddDataTableRowToolCallback.java` | Insert row |
| `UpdateDataTableRowToolCallback.java` | Update row |
| `DeleteDataTableRowToolCallback.java` | Delete row |
| `AddDataTableColumnToolCallback.java` | Add column (additive only) |
| `AddKnowledgeBaseDocumentToolCallback.java` | Add text document |
| `DeleteKnowledgeBaseDocumentToolCallback.java` | Delete document |

Each follows the Phase 1 `CreateAssetFileToolCallback` pattern: constructor-injected facade service, `@Nullable ToolContext` support, `toolError` helper, records for input/output.

#### Workflow-builder subagent

| Action | Path | Responsibility |
|---|---|---|
| Create | `ai-copilot-service/src/main/resources/prompt_workflow_builder.txt` | System prompt |
| Create | `ai-copilot-service/src/main/java/com/bytechef/ee/ai/copilot/config/WorkflowBuilderConfiguration.java` | `ChatClient workflowBuilderChatClient(...)` + `TaskToolCallbackProvider workflowBuilderTaskTool(...)` beans |
| Create | `ai-copilot-service/src/test/java/com/bytechef/ee/ai/copilot/config/WorkflowBuilderConfigurationTest.java` | Unit tests |

#### Wiring + prompt updates

| Action | Path | Responsibility |
|---|---|---|
| Modify | `CopilotConfiguration.java` | Register BOTH TaskToolCallbackProviders with correct scoping: research on ask+build, workflow_builder on build-only. Use ObjectProvider to inject (research + workflow_builder) via `List<TaskToolCallbackProvider>` or two separate `ObjectProvider`s; add all 8 new mutation callbacks to the agent's tool list; register new facade services as needed. |
| Modify | `prompt_ai_hub_build.txt` | Add mutation capability paragraph (ASK prompt stays unchanged in Phase 4) + workflow-builder delegation guidance from the spec. |
| Modify | `AiHubSpringAIAgentTest.java` | Tests for: both subagents in tool list, mutation callbacks present, no mutation callbacks leak to other agents. |

### Client

| Action | Path | Responsibility |
|---|---|---|
| Modify | `client/src/pages/automation/ai-hub/stores/useAiHubTabsStore.ts` | Add `editable: boolean` to the discriminated union; add `setTabEditable(tabId, editable)` action. |
| Modify | `client/src/pages/automation/ai-hub/stores/tests/useAiHubTabsStore.test.ts` | Test for the new action. |
| Modify | `client/src/pages/automation/ai-hub/viewers/AiHubWorkflowViewer.tsx` | Edit-mode UI: editable name/description/tags + node input forms; update via React Query mutation calling existing workflow-update GraphQL/REST endpoint. |
| Modify | matching `tests/` | Component tests for editable mode. |
| Modify | `client/src/pages/automation/ai-hub/viewers/AiHubDataTableViewer.tsx` | Edit-mode UI: click-to-edit cell, add-row button, add-column dialog, delete-row icon. |
| Modify | matching `tests/` | Component tests. |
| Modify | `client/src/pages/automation/ai-hub/viewers/AiHubKnowledgeBaseViewer.tsx` | Edit-mode UI: upload-doc dialog (text paste only in v4), delete-doc icon. |
| Modify | matching `tests/` | Component tests. |
| Modify | `client/src/pages/automation/ai-hub/AiHubResourcePanel.tsx` | Thread the `editable` prop and Edit toggle button through the active-tab header. |

### Commit convention

`CC4 …` / `CC4 client - …`.

---

## Task list

### Phase 4a — Mutation tool callbacks (server, TDD)

Each callback is one task with the same shape as Phase 1 Task 2 (`OpenFileTabToolCallback`):
1. Write the failing test.
2. Implement the callback (reuse the `CreateAssetFileToolCallback` structure).
3. Verify tests pass.
4. Format + commit.

Group the 8 mutation callbacks into 3 commits for workflow/table/KB respectively, or one commit per callback — implementer's choice based on scope per commit.

### Task 1: `UpdateWorkflowToolCallback` + `CreateWorkflowToolCallback`

**Files:** 4 (2 impl + 2 test) in `tool/`.

**Summary**:
- `updateWorkflow(workflowId, patch, version?)`: constructor-inject `WorkflowService` (or its facade). Patch is a JSON object with fields to merge into the current definition. Read current, merge, write. If `version` is provided and stale, return `{error: "version conflict"}` for optimistic locking.
- `createWorkflow(projectId, definition, name)`: delegates to the existing workflow-create service. Validates the definition JSON against the workflow schema; returns `{workflowId, name}` on success.

**Tests (per callback)**: definition metadata, happy path, version-conflict (for update), schema-invalid (for create), cross-workspace error.

**Commit**: `CC4 Add workflow mutation ToolCallbacks (update + create)`.

### Task 2: Data-table mutation callbacks

**Files:** 8 (4 impl + 4 test) in `tool/`.

**Summary**: One callback each for `addDataTableRow`, `updateDataTableRow`, `deleteDataTableRow`, `addDataTableColumn`. Reuse the existing `DataTableFacade` service. Each scopes by `WorkspaceInvocationContext`.

**Commit**: `CC4 Add data-table mutation ToolCallbacks`.

### Task 3: Knowledge-base mutation callbacks

**Files:** 4 (2 impl + 2 test) in `tool/`.

**Summary**: `addKnowledgeBaseDocument` (text only — mime type guard restricts to `text/*` / `application/json`), `deleteKnowledgeBaseDocument`. Reuse existing KB services.

**Commit**: `CC4 Add knowledge-base document mutation ToolCallbacks`.

### Task 4: Agent system prompt — mutations paragraph

**Files:**
- Modify: `prompt_ai_hub_build.txt` — append the mutation and workflow-builder paragraphs (the ASK prompt stays unchanged in Phase 4) from the spec.

**Commit**: `CC4 Add mutation + workflow-builder guidance to ai_hub system prompt`.

### Task 5: Register mutation callbacks on the agent

**Files:**
- Modify: `CopilotConfiguration.aiHubSpringAIAgent` — add the 8 mutation callbacks to `aiHubBuildSpringAIAgent` **ONLY**. Do NOT add them to `aiHubAskSpringAIAgent` — ASK is read-only. This is the load-bearing difference between the two variants.

**Strategy**: Inline instantiation where facade services are already `@Bean`s, with constructor param injection on the bean method for those services. Example:
```java
@Bean
AiHubSpringAIAgent aiHubSpringAIAgent(
    // ...existing deps,
    WorkflowService workflowService,
    DataTableFacade dataTableFacade,
    KnowledgeBaseFacade knowledgeBaseFacade,
    // ...
) {
    List<ToolCallback> toolCallbacks = new ArrayList<>(...);

    toolCallbacks.add(new OpenFileTabToolCallback());
    toolCallbacks.add(new UpdateWorkflowToolCallback(workflowService));
    toolCallbacks.add(new CreateWorkflowToolCallback(workflowService));
    toolCallbacks.add(new AddDataTableRowToolCallback(dataTableFacade));
    // etc.
    ...
}
```

Verify no mutation callback leaks to other agents: they don't use `@Bean`s, they're constructed inline.

**Commit**: `CC4 Register Phase 4 mutation ToolCallbacks on ai_hub agent`.

### Phase 4b — `workflow_builder` subagent

### Task 6: Workflow-builder system prompt

**Files:**
- Create: `ai-copilot-service/src/main/resources/prompt_workflow_builder.txt`

Paste the full prompt from the spec ("workflow_builder subagent system prompt"). Verify formatting.

**Commit**: `CC4 Add workflow_builder subagent system prompt`.

### Task 7: `WorkflowBuilderConfiguration` (TDD — mirrors Phase 3's `ResearchConfiguration`)

**Files:**
- Create: `config/WorkflowBuilderConfiguration.java`
- Create: `test/.../config/WorkflowBuilderConfigurationTest.java`

**Summary**: Two beans:
- `ChatClient workflowBuilderChatClient(ChatModel, ProjectWorkflowToolsImpl, ComponentTools, ReadProjectWorkflowToolsImpl, ...)` — includes all mcp-tools the existing `workflow_editor_build` agent uses (look up the full list in `CopilotConfiguration.workflowEditorBuildSpringAIAgent` for reference).
- `TaskToolCallbackProvider workflowBuilderTaskTool(ChatClient workflowBuilderChatClient)` — name `workflow_builder`, description from the spec.

**Tests (mirroring Phase 3 tests)**:
- `testWorkflowBuilderChatClientHasRequiredMcpTools`
- `testTaskToolCallbackIsNamedWorkflowBuilder`
- Condition on bean availability (gate on existence of required mcp-tool beans).

**Commit**: `CC4 Add WorkflowBuilderConfiguration with ChatClient + TaskToolCallbackProvider`.

### Task 8: Register workflow_builder on aiHubBuildSpringAIAgent ONLY

**Files:**
- Modify: `CopilotConfiguration.aiHubSpringAIAgent` — inject both `TaskToolCallbackProvider`s (research + workflow_builder). Research goes to BOTH ask and build (read-only). workflow_builder goes to BUILD ONLY (it constructs workflows, which is a workspace mutation when persisted). Simplest: accept them as a `List<TaskToolCallbackProvider>` parameter and flat-map their `getToolCallbacks()` into the agent's tool list.

```java
@Bean
AiHubSpringAIAgent aiHubSpringAIAgent(
    // ...existing deps,
    ObjectProvider<TaskToolCallbackProvider> taskToolCallbackProviders
) {
    // ...
    taskToolCallbackProviders.orderedStream()
        .forEach(provider -> toolCallbacks.addAll(provider.getToolCallbacks()));
    // ...
}
```

This keeps the agent bean-open to future TaskTool-based subagents without per-addition wiring.

**Test**: `AiHubSpringAIAgentTest` — assert both `research` and `workflow_builder` callbacks are present on the agent's tool list.

**Commit**: `CC4 Register workflow_builder subagent on ai_hub agent (generic TaskToolCallbackProvider discovery)`.

### Phase 4c — Client in-tab editing UI

### Task 9: Tabs store `editable` + `setTabEditable`

**Files:**
- Modify: `useAiHubTabsStore.ts` — add `editable: boolean` to non-file kinds (workflow / dataTable / knowledgeBase); default false on open. Add `setTabEditable(tabId, editable)` action.
- Modify: tests.

**Commit**: `CC4 client - Add editable state to non-file tabs`.

### Task 10: Workflow viewer edit mode

**Files:**
- Modify: `AiHubWorkflowViewer.tsx` — header Edit toggle; in edit mode render editable name/description/tags inputs and per-node input forms. Each change calls a React Query mutation that wraps the existing workflow-update GraphQL mutation (or REST endpoint); on success, refetch the workflow.
- Modify: tests — cover edit toggle + one input change + verify mutation invocation.

**Commit**: `CC4 client - Add edit mode to AiHubWorkflowViewer`.

### Task 11: Data-table viewer edit mode

**Files:**
- Modify: `AiHubDataTableViewer.tsx` — click-to-edit cells, add-row button, add-column dialog, delete-row icon. Each mutation is a React Query mutation calling the existing data-table endpoints.
- Modify: tests — cover cell edit, row add, row delete.

**Commit**: `CC4 client - Add edit mode to AiHubDataTableViewer`.

### Task 12: Knowledge-base viewer edit mode

**Files:**
- Modify: `AiHubKnowledgeBaseViewer.tsx` — upload-doc dialog (paste text, set name, set mime type — text/markdown or text/plain in v4); delete-doc icon.
- Modify: tests — cover upload flow + delete flow.

**Commit**: `CC4 client - Add edit mode to AiHubKnowledgeBaseViewer`.

### Task 13: Resource panel — Edit toggle wiring

**Files:**
- Modify: `AiHubResourcePanel.tsx` — render an Edit toggle button in the viewer header area (above or adjacent to the tab's custom controls); passes `editable` into the active viewer; toggles via `setTabEditable`.
- Skip for file tabs (file editing is already Editor mode in Phase 1).

**Commit**: `CC4 client - Add Edit toggle to resource panel viewer header`.

### Task 14: Full server + client check + manual verification

**Steps:**
- `./gradlew :server:ee:libs:ai:ai-copilot:ai-copilot-service:test`
- `cd client && npm run check`
- Manual:
  - Open a workflow tab; toggle Edit; change a node input; save; reopen full editor; verify the change persisted.
  - Open a data-table tab; toggle Edit; add a row; delete it; add a column; verify persistence.
  - Open a KB tab; toggle Edit; paste a markdown document; verify it appears in the documents list; delete it.
  - In chat, ask: "add a row to leads: name=Ada, status=qualified" — agent calls `addDataTableRow`, row appears in the open tab.
  - In chat, say: "build me a workflow that scrapes Acme's pricing page daily and appends new entries to the competitors table" — parent delegates to `workflow_builder`; a new workflow tab opens with the builder's output. Verify the workflow is sane on inspection (trigger + 2-3 steps + a write-to-table action).

**Final commit (if lint/format fixes needed)**:
```
CC4 Apply final formatting and lint fixes
```

---

## Risks to watch during execution

1. **`updateWorkflow` patch semantics**. Align with what the existing workflow facade expects — probably not an arbitrary JSON merge; may need a more structured `{nodeName: {inputs: {...}}}` shape.
2. **Optimistic locking**. The existing workflow service likely has a version field. Pass it through; on stale-version error, return a clean error from the tool callback.
3. **`workflow_builder` output validity**. Validate against the workflow schema before persisting. On invalid output, retry once with a strictness follow-up; if still invalid, report to the user.
4. **Performance of React Query cell-edit**. Debounce inputs; avoid per-keystroke mutations for data-table rows.
5. **Edit toggle state leak across tab closes**. `closeTab` must remove the tab's `editable` state; test for it.
6. **Subagent context window size**. `workflow_builder` may need many mcp-tool calls; tune the subagent's max-tokens / turn cap via the `ChatClient` builder if defaults are too small.

## Out of scope (deferred)

- Full canvas editing in-tab.
- Real-time multi-user collaboration.
- KB ingestion of binary / OCR content.
- Workflow debugging subagent.
- Data-analyst subagent.

## Architectural commitment reaffirmed

- **All subagents go through `TaskTool`** (mandated from Phase 3 onward). Phase 4 introduces the second TaskTool-based subagent; the generic `ObjectProvider<TaskToolCallbackProvider>` discovery pattern in Task 8 makes future additions zero-wiring beyond creating a new `Configuration` class.
