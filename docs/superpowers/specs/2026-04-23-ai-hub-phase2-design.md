# AI Hub Phase 2 — Resource panel expansion + cross-domain tools

**Status**: Draft
**Date**: 2026-04-23
**Builds on**: [Phase 1 design](2026-04-23-ai-hub-shell-design.md) — ship before this spec is implemented.
**Scope**: Extend the Phase 1 AI Hub shell so the resource panel hosts workflows, data tables, and knowledge bases (read-only) in addition to files, and so the `ai_hub` agent has tool callbacks to drive them.

---

## Goal

Turn the AI Hub from a files-only assistant into a workspace-wide conversational surface. A user can ask the agent to "show me the Slack connector workflow," "query the leads table for qualified rows," or "search the product-docs knowledge base for pricing" — and the agent opens the relevant resource as a tab in the right panel, reading it via new tool callbacks. The resource panel becomes the unified viewer for all four resource types. User-initiated mutation of workflows, tables, and KBs stays on the dedicated pages (Phase 4 will add in-tab editing).

Success in v2: a user can open the AI Hub, type "list my workflows," and the agent responds with a summary plus opens a workflow tab the user can inspect. Same for data tables (with a live query box) and knowledge bases (with docs + search). `@`-mentions in the chat let the user reference any of the four resource types inline.

## Non-goals (v2)

- **Mutation of workflows/tables/KBs via the panel**. The panel is read-only; mutations happen either via the agent's tool callbacks or via the existing dedicated pages. Phase 4 adds in-tab editing.
- **Research agent**. Phase 3.
- **Scheduled prompt-jobs, generative non-file outputs, new KB connector types**. Later sub-projects.
- **Cross-workspace references**. Tabs are scoped to the current workspace, same as every other resource.

## Architecture overview

Phase 1 established a clean extension model that Phase 2 uses verbatim:

1. **New tab types in the tabs store**. `useAiHubTabsStore` gains a discriminated-union `kind: 'file' | 'workflow' | 'dataTable' | 'knowledgeBase'`, each with its own reference id payload.
2. **New signaling tools on the agent**. For each non-file type, a new `openXxxTab` server-side `ToolCallback` (modeled on Phase 1's `OpenFileTabToolCallback`) echoes `{opened: true, id, name}` back; the client subscriber intercepts the result and calls the matching `openXxxTab` store action.
3. **New read/query tool callbacks**. The agent gets `listWorkflows`, `getWorkflow`, `listDataTables`, `queryDataTable`, `listKnowledgeBases`, `queryKnowledgeBase`. Each is a Spring AI `ToolCallback` that talks to the corresponding facade (`ProjectWorkflowService`, `DataTableService`, `KnowledgeBaseService`) via the existing EE wiring.
4. **New viewer components**. Three new files under `client/src/pages/automation/ai-hub/`: `AiHubWorkflowViewer.tsx`, `AiHubDataTableViewer.tsx`, `AiHubKnowledgeBaseViewer.tsx`. Each consumes an id from the active tab and renders a read-only view.
5. **`@`-mention composer**. A new `AiHubComposer.tsx` that wraps `Thread`'s input with an `@`-picker (popover showing the four resource types). Selecting an item inserts a `@`-ref token and adds that resource to the next turn's `state.referencedResources`.

Two-pane shell, `AiHubPanel`, `AiHubResourcePanel`, the tabs store's activeTab/rightPanelOpen machinery — all unchanged.

```
┌─────────────────────────────────────────────────────────┐
│ /automation/ai-hub (EE-only — unchanged)        │
├───────────────────────────┬─────────────────────────────┤
│  AiHubPanel       │  Resource Panel             │
│  (now with @ picker)      │  ┌────┬────┬────┬────┬──┐  │
│                           │  │f.md│w:x │t:y │kb:z│+ │  │
│  chat stream…             │  ├────┴────┴────┴────┴──┤  │
│                           │  │ <viewer for kind>    │  │
└───────────────────────────┴─────────────────────────────┘
    file      workflow       data table    knowledge base
    (Phase 1)  (new)           (new)         (new)
```

## Tab kind model

The tabs store replaces the single `AiHubTabI` type with a discriminated union:

```ts
type AiHubTabI =
    | {id: string; kind: 'file'; fileId: string; name: string; viewMode: AiHubViewModeType}
    | {id: string; kind: 'workflow'; workflowId: string; projectId: string; name: string}
    | {id: string; kind: 'dataTable'; dataTableId: string; name: string; query: string | undefined}
    | {id: string; kind: 'knowledgeBase'; knowledgeBaseId: string; name: string; search: string | undefined};
```

Store actions split by kind: `openFileTab`, `openWorkflowTab`, `openDataTableTab`, `openKnowledgeBaseTab` — each dedupes by its own id field.

The resource panel dispatches on `activeTab.kind` to pick the correct viewer component. The view-mode `ToggleGroup` (Editor / Preview / Split) only renders for file tabs. Other tab kinds have their own header affordances (query box, search box, "open in dedicated page" link).

## Agent tool callbacks

New `ToolCallback` classes (one per operation), registered as beans in `CopilotConfiguration` and added to the `aiHubSpringAIAgent`'s tool list alongside the existing asset-file callbacks and `OpenFileTabToolCallback`:

### Workflow tools (EE, reuses existing `WorkflowService`)

- `listWorkflows` — no args. Lists workflows in the current workspace scoped by `WorkspaceInvocationContext`. Returns `[{id, projectId, name, description, tags}, ...]`.
- `getWorkflow(workflowId)` — returns `{id, name, definition (compact JSON), connections, tags}`.
- `openWorkflowTab(workflowId, name)` — signaling no-op mirroring `OpenFileTabToolCallback`.

### Data table tools (reuses `automation-data-table` services)

- `listDataTables` — lists tables in current workspace. Returns `[{id, name, columns, rowCount}, ...]`.
- `queryDataTable(dataTableId, where?, limit?)` — runs a scoped read-only query, returns up to 50 rows.
- `openDataTableTab(dataTableId, name)` — signaling no-op.

### Knowledge base tools (reuses `automation-knowledge-base` services)

- `listKnowledgeBases` — lists KBs in current workspace. Returns `[{id, name, documentCount, lastSyncedAt}, ...]`.
- `queryKnowledgeBase(knowledgeBaseId, question, limit?)` — RAG query using the existing KB vector-search service. Returns `[{docId, docTitle, excerpt, score}, ...]` (up to limit, default 5).
- `openKnowledgeBaseTab(knowledgeBaseId, name)` — signaling no-op.

All tool callbacks scope by the ambient `WorkspaceInvocationContext` (same mechanism Phase 1 used for asset-file tools). Cross-workspace references return an error.

### System prompt update

`prompt_ai_hub.txt` is rewritten to enumerate the new capabilities:

```
You can:
- List, read, and create workspace files (listAssetFiles, getAssetFileContent, createAssetFile)
- List and read workflows (listWorkflows, getWorkflow) and open them in the panel (openWorkflowTab)
- List data tables (listDataTables), query them read-only (queryDataTable), and open them in the panel (openDataTableTab)
- List knowledge bases (listKnowledgeBases), query them via RAG (queryKnowledgeBase), and open them in the panel (openKnowledgeBaseTab)

Always open a resource in the panel (via the matching openXxxTab tool) after
creating or discussing it so the user sees it. Use the ids returned from the
listing tools — never invent ids.

When the user references a resource via @-mention, prefer acting on that
specific resource. The State's `referencedResources` field lists the
@-mentions for the current turn.
```

## Viewer components

### `AiHubWorkflowViewer.tsx`

- Header: workflow name + "Open in editor" button (routes to `/automation/projects/:projectId/workflows/:workflowId`).
- Body: a read-only rendered view of the workflow's nodes, reusing the existing read-only workflow editor component (`client/src/shared/components/read-only-workflow-editor/`). No edit affordances.
- Fetches workflow structure via the existing project-workflow GraphQL query.

### `AiHubDataTableViewer.tsx`

- Header: table name + "Open in full view" button (routes to `/automation/datatables/:id`) + a search input.
- Body: paginated grid (up to 50 rows per page, ~5 pages scrollback cap). Columns show type badges.
- Search input updates `activeTab.query`, which is injected into `state` for the next chat turn so the agent can act on the user's current filter.
- Reuses an existing data-table row-renderer component if one exists; otherwise a simple HTML table suffices for v2.

### `AiHubKnowledgeBaseViewer.tsx`

- Header: KB name + "Open in full view" button (routes to `/automation/knowledge-bases/:id`) + a search input that runs `queryKnowledgeBase` locally (via a shared mutation) and displays excerpts.
- Body, two tabs:
  - **Documents** — list of source documents in the KB, each clickable to show a preview.
  - **Search** — results of the latest search, excerpts highlighted.

## `@`-mention composer

A new file `AiHubComposer.tsx` wraps `<Thread />`'s input with an `@`-listener. The composer:

1. Detects `@` in the textarea, opens a popover listing all four resource types (files/workflows/tables/KBs) filtered by the typed text after `@`.
2. Inserts a chip-like token `@[name]` into the composer; the raw message text sent to the agent contains the token as plain text (e.g. `@leads-table`) so the LLM sees it in context.
3. Tracks selected references in a local `useAiHubComposerStore` and, on submit, adds them to the AG-UI state's `referencedResources: [{kind, id, name}, ...]`.
4. The agent's `createSystemMessage` reads `referencedResources` and appends them as a `Context` block titled "Referenced Resources" — same pattern as Phase 1's "Open Tabs."

Drag-drop from the right panel's tab bar into the composer has the same effect as `@`-mentioning that resource (phase-2 scope-permitting; drop-and-convert-to-token is a small additional hook).

## State contract additions (AG-UI)

Phase 1's `state` payload gains two fields:

```
{
  // Phase 1 fields:
  "source": "AI_HUB",
  "workspaceId": "<string>",
  "mode": "ASK" | "BUILD",
  "currentTabs": [...],
  "activeFileId": "<string | null>",

  // Phase 2 additions:
  "activeTab": {"kind": "<kind>", "id": "<id>", "name": "<string>"} | null,
  "referencedResources": [{"kind": "<kind>", "id": "<string>", "name": "<string>"}, ...]
}
```

`currentTabs`'s item shape is broadened to include the `kind` field and kind-specific id; the agent's `createSystemMessage` dispatches on `kind` when formatting the "Open Tabs" context block.

`activeFileId` is kept for backward compatibility but deprecated in favor of `activeTab`; Phase 1 tests continue to pass against it.

## Testing

### Server

- One unit test per new `ToolCallback`: definition has right name + input schema; happy-path call returns expected JSON; error paths (missing input, not-found, workspace mismatch) return error JSON.
- `AiHubSpringAIAgentTest` gains:
  - `testCreateSystemMessageIncludesReferencedResources` — when `state.referencedResources` is populated, the system message contains a "Referenced Resources" context block.
  - `testCreateSystemMessageHandlesMixedKindTabs` — a tabs list with mixed kinds renders each appropriately.
- Integration: a smoke test that `CopilotConfiguration` wires every new tool into `aiHubSpringAIAgent`'s tool list and not into other agents.

### Client

- `useAiHubTabsStore.test.ts` gains cases for each new `openXxxTab` action, dedup behavior, and close-with-mixed-kinds.
- One test file per viewer (workflow, dataTable, knowledgeBase): renders correctly with test data; "Open in full view" link has the right href.
- `AiHubComposer.test.tsx`: `@` opens picker; selection inserts token; submit emits `referencedResources` via the runtime provider's `buildStateToSend`.
- `AiHubRuntimeProvider.test.tsx` gains cases for the three new tool-result interceptions.

## Risks and open questions

- **Read-only workflow preview reuse**. The existing `read-only-workflow-editor` component may or may not accept an `embedded` prop. The plan phase confirms the interface; if it doesn't, the viewer renders a simple node-list fallback until Phase 4 adds a proper in-tab editor.
- **GraphQL query availability**. Existing queries likely cover workflows, data tables, and KBs, but some may only expose detail via REST. The plan phase maps each needed read to a real endpoint or codegen-hook; the fallback is to add a thin GraphQL query.
- **`@`-picker UX coupling to assistant-ui/react**. The `<Thread />` input is internal to `@assistant-ui/react`. The composer wraps the Thread or replaces the default input via the library's composition points; the plan phase confirms the supported extension API. If unsupported, the fallback is a second composer rendered above Thread for `@`-ref selection only.

## Commit convention

Per CLAUDE.md: `CC2 client - …` / `CC2 …` as placeholder tickets; user can rewrite.
