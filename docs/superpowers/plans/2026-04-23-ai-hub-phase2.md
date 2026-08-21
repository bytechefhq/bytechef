# AI Hub Phase 2 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Extend the Phase 1 AI Hub shell with workflow, data-table, and knowledge-base tabs (read-only) plus cross-domain `ToolCallback`s so the `ai_hub` agent can list/read/open those resource types. Add `@`-mention composer support for inline resource references.

**Architecture:** All extensions piggyback on Phase 1's machinery. The tabs store becomes a discriminated union keyed by `kind`. Each new resource type gets: (a) a signaling `openXxxTabToolCallback` (mirrors `OpenFileTabToolCallback`), (b) listing/reading tool callbacks that call existing EE facades, (c) a client viewer component, (d) a subscriber-interception case in `AiHubRuntimeProvider`. The agent system prompt is rewritten to enumerate new capabilities. No new agent, no new route, no new runtime protocol.

**Tech stack:** Same as Phase 1 (Java 25 / Spring AI / AG-UI server; React 19 / TypeScript / Zustand / assistant-ui).

**Reference spec:** [docs/superpowers/specs/2026-04-23-ai-hub-phase2-design.md](../specs/2026-04-23-ai-hub-phase2-design.md).

**Depends on:** Phase 1 (merged). Do not start Phase 2 until Phase 1's commits are on the feature branch.

---

## File structure

### Server (EE)

| Action | Path | Responsibility |
|---|---|---|
| Create | `server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ee/ai/copilot/tool/OpenWorkflowTabToolCallback.java` | Signaling no-op, mirrors `OpenFileTabToolCallback` |
| Create | `server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ee/ai/copilot/tool/OpenDataTableTabToolCallback.java` | Same pattern |
| Create | `server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ee/ai/copilot/tool/OpenKnowledgeBaseTabToolCallback.java` | Same pattern |
| Create | `server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ee/ai/copilot/tool/ListWorkflowsToolCallback.java` | Workspace-scoped read via existing `WorkflowService` |
| Create | `server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ee/ai/copilot/tool/GetWorkflowToolCallback.java` | Returns compact workflow definition |
| Create | `server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ee/ai/copilot/tool/ListDataTablesToolCallback.java` | Reads via `automation-data-table` service |
| Create | `server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ee/ai/copilot/tool/QueryDataTableToolCallback.java` | Runs a scoped read-only query (max 50 rows) |
| Create | `server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ee/ai/copilot/tool/ListKnowledgeBasesToolCallback.java` | Reads via `automation-knowledge-base` service |
| Create | `server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ee/ai/copilot/tool/QueryKnowledgeBaseToolCallback.java` | RAG query via existing KB vector service |
| Create | matching `*Test.java` for each of the 9 callbacks | Unit tests |
| Modify | `server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ee/ai/copilot/agent/AiHubSpringAIAgent.java` | Read `state.referencedResources` and `state.activeTab` in `createSystemMessage`; append as context blocks |
| Modify | `server/ee/libs/ai/ai-copilot/ai-copilot-service/src/test/java/com/bytechef/ee/ai/copilot/agent/AiHubSpringAIAgentTest.java` | Two new tests covering the new context blocks |
| Modify | `server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ee/ai/copilot/config/CopilotConfiguration.java` | Instantiate and add all 9 new callbacks to `aiHubSpringAIAgent`'s tool list |
| Modify | `server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/resources/prompt_ai_hub_ask.txt` AND prompt_ai_hub_build.txt | Rewrite both to enumerate the new read/query capabilities (same content — Phase 2 adds no mutations) |

### Client

| Action | Path | Responsibility |
|---|---|---|
| Modify | `client/src/pages/automation/ai-hub/stores/useAiHubTabsStore.ts` | Convert `AiHubTabI` to a discriminated union keyed by `kind`. Add `openWorkflowTab`, `openDataTableTab`, `openKnowledgeBaseTab` actions. Keep `openFileTab` backward-compatible |
| Modify | `client/src/pages/automation/ai-hub/stores/tests/useAiHubTabsStore.test.ts` | Cover all four `openXxxTab` actions and mixed-kind close behavior |
| Create | `client/src/pages/automation/ai-hub/viewers/AiHubWorkflowViewer.tsx` | Read-only workflow preview |
| Create | `client/src/pages/automation/ai-hub/viewers/AiHubDataTableViewer.tsx` | Paginated row view + inline search box |
| Create | `client/src/pages/automation/ai-hub/viewers/AiHubKnowledgeBaseViewer.tsx` | Documents list + RAG search results view |
| Create | matching `tests/` directory and `*.test.tsx` per viewer | Component tests |
| Modify | `client/src/pages/automation/ai-hub/AiHubResourcePanel.tsx` | Switch-on-kind dispatch for the body; hide view-mode toggle for non-file kinds |
| Move | `client/src/pages/automation/ai-hub/AiHubFileViewer.tsx` → `.../viewers/AiHubFileViewer.tsx` | Colocate viewers under `viewers/` |
| Create | `client/src/pages/automation/ai-hub/composer/AiHubComposer.tsx` | `@`-mention popover + token insertion |
| Create | `client/src/pages/automation/ai-hub/composer/stores/useAiHubComposerStore.ts` | Tracks selected `@`-refs for the next turn |
| Create | matching `tests/` for composer | Tests |
| Modify | `client/src/pages/automation/ai-hub/AiHubPanel.tsx` | Replace raw `<Thread />` body with a Thread inside `AiHubComposer` wrapper (verify `@assistant-ui/react` extension points) |
| Modify | `client/src/pages/automation/ai-hub/runtime-providers/AiHubRuntimeProvider.tsx` | Add `onToolCallResultEvent` cases for `openWorkflowTab`, `openDataTableTab`, `openKnowledgeBaseTab`. Update `buildStateToSend` to include `activeTab` and `referencedResources` |
| Modify | `client/src/pages/automation/ai-hub/runtime-providers/tests/AiHubRuntimeProvider.test.tsx` | Add subscriber cases + new state fields |

### Commit convention

`CC2 …` / `CC2 client - …`. Match Phase 1's prefix convention.

---

## Task list

### Task 1: Discriminated-union tabs store

**Files:**
- Modify: `client/src/pages/automation/ai-hub/stores/useAiHubTabsStore.ts`
- Modify: `client/src/pages/automation/ai-hub/stores/tests/useAiHubTabsStore.test.ts`

**Summary**: Introduce `kind` on each tab. `AiHubTabI` becomes a union of four shapes. Existing `openFileTab(fileId, name)` stays a thin wrapper that now sets `kind: 'file'`. Three new actions `openWorkflowTab(workflowId, projectId, name)`, `openDataTableTab(dataTableId, name)`, `openKnowledgeBaseTab(knowledgeBaseId, name)` each dedupe by their own id field. `closeTab` and `setActiveTab` become kind-agnostic.

**TDD sequence**: write new tests (open/dedupe/close for each kind, plus mixed-kind close), confirm fail, modify store, confirm pass.

Tests should include: opening a workflow tab after an already-open file tab preserves both; closing the active workflow tab with a file tab still open makes the file the active tab; `inferDefaultViewMode` is called only when opening a file.

**Commit**:
```
CC2 client - Make tabs store a discriminated union keyed by kind

Adds workflow/dataTable/knowledgeBase tab kinds and per-kind open actions.
File tab behavior is preserved via a thin wrapper over the new union.
```

### Task 2: Server — signaling callbacks for workflow/table/KB opens

**Files (3 new + 3 new tests)**: `OpenWorkflowTabToolCallback.java`, `OpenDataTableTabToolCallback.java`, `OpenKnowledgeBaseTabToolCallback.java`, plus matching `*Test.java`.

**Summary**: Each mirrors `OpenFileTabToolCallback` exactly: Spring AI `ToolCallback`, input schema with id + name, echo `{opened:true, id, name}`; four tests per callback (definition, happy path, invalid JSON, missing id).

**Commit**:
```
CC2 Add signaling ToolCallbacks for workflow/dataTable/knowledgeBase tabs
```

### Task 3: Server — workflow read tools

**Files**: `ListWorkflowsToolCallback.java`, `GetWorkflowToolCallback.java`, plus tests.

**Summary**: Constructor-inject `WorkflowService` and `ProjectWorkflowService`. `listWorkflows`: no-args; uses `WorkspaceInvocationContext` to scope; returns `[{id, projectId, name, description, tags}]` as JSON. `getWorkflow`: requires `workflowId`; returns `{id, name, definition, connections, tags}`. Errors (not found, cross-workspace) return `{error: "..."}` JSON (reuse the `toolError` helper pattern from Phase 1).

**Test pattern**: Mock `WorkflowService`; assert tool definition metadata + JSON shape on happy path + error paths.

**Commit**:
```
CC2 Add workflow read ToolCallbacks for the AI Hub agent
```

### Task 4: Server — data-table read tools

**Files**: `ListDataTablesToolCallback.java`, `QueryDataTableToolCallback.java`, plus tests.

**Summary**: Constructor-inject the data-table facade (locate via `Grep` in `server/libs/automation/automation-data-table/` during planning; typical name is `DataTableFacade`). `listDataTables`: returns rows of `[{id, name, columns, rowCount}]`. `queryDataTable(dataTableId, where?, limit?)`: runs a read-only query; caps limit at 50; scopes by workspace; returns `[{columnValues}]`. Invalid `where` (unparseable filter) → error JSON.

**Commit**:
```
CC2 Add data-table read + query ToolCallbacks for the AI Hub agent
```

### Task 5: Server — knowledge-base read tools

**Files**: `ListKnowledgeBasesToolCallback.java`, `QueryKnowledgeBaseToolCallback.java`, plus tests.

**Summary**: Constructor-inject the KB facade/services (locate in `server/libs/automation/automation-knowledge-base/`). `listKnowledgeBases`: returns rows. `queryKnowledgeBase(knowledgeBaseId, question, limit?)`: calls the existing vector-search service; returns `[{docId, docTitle, excerpt, score}]` up to limit (default 5, max 20).

**Commit**:
```
CC2 Add knowledge-base read + RAG-query ToolCallbacks for the AI Hub agent
```

### Task 6: Agent — system-message context for `activeTab` + `referencedResources`

**Files:**
- Modify: `AiHubSpringAIAgent.java` — extend `createSystemMessage` to append two new `Context` blocks when present
- Modify: `AiHubSpringAIAgentTest.java` — two new tests

**Summary**: Read `state.activeTab` (object with `kind`, `id`, `name`) and `state.referencedResources` (array of similar objects). When present, append:
- `new Context("Active Tab", formatActiveTab(state))`
- `new Context("Referenced Resources", formatReferencedResources(state))`

`formatActiveTab` and `formatReferencedResources` are small static helpers mirroring `formatTabs` from Phase 1. Omit the block entirely when the field is null/empty.

**Commit**:
```
CC2 Inject activeTab and referencedResources into AI Hub system message
```

### Task 7: Agent — system prompt rewrite

**Files:**
- Modify: `prompt_ai_hub_ask.txt` and `prompt_ai_hub_build.txt` — update both with with the capability enumeration from the spec's "System prompt update" section
- No test change needed

**Commit**:
```
CC2 Extend ai_hub system prompt to cover workflows/tables/KBs
```

### Task 8: Register new callbacks in `CopilotConfiguration`

**Files:**
- Modify: `CopilotConfiguration.java` — instantiate all 9 new callbacks inline in `aiHubSpringAIAgent`'s tool list. Mirror the Phase 1 pattern where `OpenFileTabToolCallback` is added directly (not registered as a bean).

**Summary**: Both `aiHubAskSpringAIAgent` and `aiHubBuildSpringAIAgent` bean methods (introduced in the Phase 1 ASK/BUILD retrofit, commit `cbcdc4e6ec5`) build their tool-callbacks list. Read/query callbacks go to **both** variants (they're safe in ASK — no mutation). Signaling callbacks go to both. Each bean method appends them the same way: `toolCallbacks.addAll(globalToolCallbacks); toolCallbacks.add(new OpenFileTabToolCallback()); toolCallbacks.add(new OpenWorkflowTabToolCallback()); …`. Read tools (`ListWorkflowsToolCallback`, etc.) need constructor-injected services; register them as beans so Spring wires them, then add by bean ref.

Tradeoff: read-tool callbacks need DI (they wrap services). Signaling callbacks don't. So:
- Signaling callbacks: instantiated inline in both ASK and BUILD bean methods (6 lines per agent: 3 × `new …ToolCallback()`).
- Read/query callbacks: registered as `@Bean`s, injected into both `aiHubAskSpringAIAgent` and `aiHubBuildSpringAIAgent`, added to each's tool list.

To avoid leaking read callbacks into other agents (which use `ObjectProvider<ToolCallback>.orderedStream()`), use a Spring Qualifier annotation `@AiHubToolCallback` (new marker). Filter the global provider to exclude qualified ones, and hand-pick the ai-hub-qualified ones into the ai_hub agent.

Alternatively, if qualifier filtering is too heavy, instantiate the read callbacks **inline** by resolving their service deps from `@Autowired` constructor params on the bean method itself, as the existing Phase 1 `aiHubSpringAIAgent` method already receives `ObjectProvider<ToolCallback>` — just add more constructor params to the bean method.

**Commit**:
```
CC2 Register Phase 2 tool callbacks on the ai_hub agent

Signaling callbacks (workflow/dataTable/KB tab-open) are instantiated inline.
Read/query callbacks are wired as beans and added to the agent's tool list
without bleeding into other agents.
```

### Task 9: Workflow viewer component

**Files:**
- Move: `AiHubFileViewer.tsx` → `viewers/AiHubFileViewer.tsx` (and its tests). This colocates viewers.
- Update imports everywhere (`AiHubResourcePanel.tsx`).
- Create: `client/src/pages/automation/ai-hub/viewers/AiHubWorkflowViewer.tsx`
- Create: tests

**Summary**: Props `{workflowId, projectId, name}`. Header: name + "Open in editor" button linking to `/automation/projects/:projectId/workflows/:workflowId`. Body: reuses `client/src/shared/components/read-only-workflow-editor/` if its component accepts a `workflowId` prop; otherwise a simple node/edge list fallback until Phase 4 unlocks the richer viewer. Fetches the workflow definition via the existing GraphQL query (find in `client/src/shared/queries/automation/projectWorkflows.queries.ts`).

**Commit**:
```
CC2 client - Add AiHubWorkflowViewer + move file viewer under viewers/
```

### Task 10: Data-table viewer component

**Files:**
- Create: `client/src/pages/automation/ai-hub/viewers/AiHubDataTableViewer.tsx`
- Create: tests

**Summary**: Props `{dataTableId, name}`. Header: name + "Open in full view" link to `/automation/datatables/:id` + search `<input>` box. Body: paginated table (50 rows/page, max 5 pages). Reuses an existing data-table row component if present; otherwise plain `<table>` with typed headers is acceptable for v2. On search input change, updates `activeTab.query` in the tabs store.

**Commit**:
```
CC2 client - Add AiHubDataTableViewer
```

### Task 11: Knowledge-base viewer component

**Files:**
- Create: `client/src/pages/automation/ai-hub/viewers/AiHubKnowledgeBaseViewer.tsx`
- Create: tests

**Summary**: Props `{knowledgeBaseId, name}`. Header: name + "Open in full view" link + search box. Body uses internal tabs `Documents | Search`. Documents tab lists source docs via existing KB GraphQL query. Search tab runs `queryKnowledgeBase` via a React Query mutation and shows excerpt cards.

**Commit**:
```
CC2 client - Add AiHubKnowledgeBaseViewer
```

### Task 12: Resource panel kind-dispatch

**Files:**
- Modify: `AiHubResourcePanel.tsx`

**Summary**: Replace the single `AiHubFileViewer` render with a `switch (activeTab.kind)` that picks the correct viewer. Hide the Editor/Preview/Split ToggleGroup for non-file kinds. Tab label format per kind: file → name, workflow → `w: name`, dataTable → `t: name`, knowledgeBase → `kb: name` (distinguishes in crowded tab strips).

**Commit**:
```
CC2 client - Dispatch viewer by tab kind in the resource panel
```

### Task 13: Runtime-provider subscriber interceptions

**Files:**
- Modify: `AiHubRuntimeProvider.tsx` — `buildAiHubSubscriber`'s `onToolCallResultEvent` adds cases for `openWorkflowTab`, `openDataTableTab`, `openKnowledgeBaseTab`. `buildStateToSend` computes `activeTab: {kind, id, name} | null` from the tabs store; adds `referencedResources` from the new composer store.
- Modify: tests

**Summary**: Three new subscriber cases (same shape as the existing `openFileTab` case), each calling the matching store `openXxxTab` action after validating the result JSON. `activeTab` is derived: find the tab whose `id === activeTabId` and flatten its `kind`-specific id into a generic `{kind, id, name}`.

**Commit**:
```
CC2 client - Intercept workflow/dataTable/KB tab-open results; expose activeTab + referencedResources in state
```

### Task 14: `@`-mention composer + store

**Files:**
- Create: `client/src/pages/automation/ai-hub/composer/stores/useAiHubComposerStore.ts` — `referencedResources: Array<{kind, id, name}>`, actions `addReference`, `removeReference`, `clear`.
- Create: `client/src/pages/automation/ai-hub/composer/AiHubComposer.tsx` — wraps Thread's composer with `@`-listener.
- Create: tests.
- Modify: `AiHubPanel.tsx` — replace `<Thread />` with `<AiHubComposer />` (which internally renders Thread).

**Summary**: Implementation strategy depends on `@assistant-ui/react`'s composition points. Two viable approaches to investigate during planning:
1. **Replace the default composer**: `assistant-ui` exposes `ThreadPrimitive.Composer` slots; replace with a custom composer that detects `@` and opens a popover. This is the ideal.
2. **Parallel composer above Thread**: render a small `@`-picker panel above Thread that lets the user pick refs before typing; on Thread submit, the subscriber in `AiHubRuntimeProvider` pulls from the store. Fallback if (1) is impractical.

The picker queries the four list endpoints (asset files via existing query; workflows/tables/KBs via the corresponding lists — which, in v2, still exist as server tool callbacks but can also be exposed via GraphQL if not already) and shows results under a single popover, sectioned by kind.

On message submit, the store's current `referencedResources` is captured, added to `state.referencedResources`, and cleared.

**Commit**:
```
CC2 client - Add @-mention composer with multi-type resource picker
```

### Task 15: System prompt + documentation refresh

**Files:**
- Already covered in Task 7.
- No additional file changes unless user-facing docs need updating (`docs/` — optional for this plan).

### Task 16: Full server + client check, manual verification

**Steps:**
- Run `./gradlew :server:ee:libs:ai:ai-copilot:ai-copilot-service:test` — expect all tests pass.
- Run `cd client && npm run check` — expect clean.
- Start infra + server + client dev; log in as admin.
- Manual checklist:
  - Open `/automation/ai-hub`.
  - Ask: "list my workflows"  — agent calls `listWorkflows`, summarizes.
  - Ask: "show me the Slack Outreach workflow" — agent opens a workflow tab. "Open in editor" link navigates to full editor.
  - Ask: "query the leads table for qualified rows" — agent opens a data-table tab with filtered results.
  - Ask: "what does the product-docs KB say about pricing?" — agent runs `queryKnowledgeBase`, opens the KB tab with a search-result view.
  - Type `@` in composer — picker opens; select a workflow; verify token is inserted and subsequent message includes it in `state.referencedResources`.
  - Close each tab type; neighboring tab becomes active; panel collapses gracefully when empty.
  - EE gating still honored (same route as Phase 1).

**Final commit** (if formatting/lint fixes needed):
```
CC2 Apply final formatting and lint fixes
```

---

## Out of scope (deferred to Phase 4 per user direction)

- In-tab **editing** of workflows, tables, KBs.
- Bulk row edit, inline workflow modification, KB document upload from the panel.

## Out of scope (other phases)

- Research agent (Phase 3).
- Scheduled prompt-jobs.
- Generative images / slide decks.
- Additional KB connector types.
