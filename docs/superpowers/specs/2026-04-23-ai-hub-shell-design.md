# AI Hub Shell (v1) — Design

**Status**: Draft
**Date**: 2026-04-23
**Scope**: Sub-project #1 of a multi-phase "Mothership-style" command center feature. Covers the shell
and its file viewer only. Non-file resources, research agent, scheduled prompt-jobs, generative
files beyond markdown/text, and cross-domain agent tools are out of scope and will be addressed in
later sub-projects.

---

## Goal

Ship a full-page AI command center at `/automation/ai-hub` that pairs the existing EE
`CopilotPanel` with a new collapsible resource panel on the right. The resource panel hosts workspace
files as closable tabs with Editor / Preview / Split view modes. A new `ai_hub` Spring AI
agent authors and opens files on the user's behalf via the existing AG-UI copilot transport.

Success in v1 means: a user can open the AI Hub, ask the agent to generate a markdown file,
watch it appear as a new tab in the right panel, toggle between Editor and Preview, and close the
tab. Manual file picking from a "+" menu also works. No other resource types (workflows, data
tables, knowledge bases) are in v1.

## Non-goals (v1)

- Non-file resource types in tabs (workflows, data tables, knowledge bases). Phase 2+.
- `@`-mentions and drag-drop of workspace resources into the chat. Phase 2+.
- Research agent, scheduled prompt-based jobs, generative images or slide decks, KB connectors.
  Separate sub-projects.
- Cross-domain agent tools (workflow CRUD, data-table queries, KB queries). Phase 2+.
- BUILD mode for the command center agent. v1 operates with the ASK/BUILD toggle kept in the panel
  (for consistency with `CopilotPanel`) but only ASK is wired up server-side. BUILD is a no-op.
- CE support. AI Hub is gated EE-only, consistent with the existing copilot
  (`bytechef.ai.copilot.enabled=true` + `@ConditionalOnEEVersion`).

## Architecture overview

Two-pane layout on a dedicated route:

```
┌─────────────────────────────────────────────────────────┐
│ /automation/ai-hub (EE-only)                    │
├───────────────────────────┬─────────────────────────────┤
│                           │  Resource Panel (optional)  │
│  CopilotPanel             │  ┌──────┬──────┬──────┐  ✕  │
│  (Source.AI_HUB)  │  │ f1.md│ f2.md│  +   │     │
│  ASK / BUILD toggle       │  ├──────┴──────┴──────┘     │
│                           │  │ Editor │ Preview │ Split │
│  ┌─ messages ─────────┐   │  ├───────────────────────── │
│  │ Thread             │   │  │                          │
│  │ (@assistant-ui)    │   │  │  AiHubFileViewer │
│  │                    │   │  │                          │
│  └────────────────────┘   │  │                          │
└───────────────────────────┴─────────────────────────────┘
```

The right pane is collapsible and hidden by default; it opens automatically the first time a tab is
added (via agent tool call or manual picker) and can be toggled via a header button.

Tabs are pure client state (new `useAiHubTabsStore`). The agent interacts with tabs through
two channels:

- **Open** — a client-side AG-UI tool `openFileTab({fileId, name})` registered by
  `AiHubRuntimeProvider`. When the agent calls it, the runtime executes it in the browser,
  updates the tabs store, and returns a tool result.
- **Awareness** — `AiHubRuntimeProvider` injects `currentTabs`, `activeFileId`, `workspaceId`,
  and `mode` into the AG-UI `state` on every turn. The agent's `createSystemMessage` reads them and
  appends them as context, mirroring the pattern in `WorkflowEditorSpringAIAgent.createSystemMessage`
  (which does the same for `workflowId`).

This is loose coupling by design: manual tab changes and agent tab changes share one store, and the
agent never pushes state out-of-band.

## Server-side design

No new Gradle modules. All changes sit in the existing `ai-copilot-service`, `ai-copilot-api`, and
`ai-copilot-rest` modules.

### New files

- `server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ee/ai/copilot/agent/AiHubSpringAIAgent.java`
  — subclass of `SpringAIAgent`. Structurally mirrors `FilesSpringAIAgent`:
    - Overrides `createSystemMessage(State, List<Context>)` to append `state.currentTabs` and
      `state.activeFileId` as contexts before delegating to the base-class formatting.
    - Overrides `run(RunAgentInput, AgentSubscriber)` to build a `WorkspaceInvocationContext`
      (same helper used by `FilesSpringAIAgent`) so workspace-scoped tool callbacks resolve the
      right workspace.
    - `ADDITIONAL_RULES` system prompt: instructs the agent that it can list, read, and create
      workspace files via server-side tools, and must call the client-side tool `openFileTab`
      after creating or referencing a file so the user sees it.

### Modifications

- `server/ee/libs/ai/ai-copilot/ai-copilot-api/src/main/java/com/bytechef/ee/ai/copilot/util/Source.java`
  — add `AI_HUB` enum value.
- `server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ee/ai/copilot/config/CopilotConfiguration.java`
  — register a `AiHubSpringAIAgent` bean with `agentId = "ai_hub"`. Tool callbacks
  in v1 are the same asset-file tool callbacks already wired for the `files` agent
  (`createAssetFile`, `listAssetFiles`, `getAssetFileContent`). No server-side tool for
  `openFileTab` — it is a client-side tool declared in the agent's tool list so the LLM knows it
  exists, but executed in the browser.
- `server/ee/libs/ai/ai-copilot/ai-copilot-rest/src/main/java/com/bytechef/ee/ai/copilot/web/rest/CopilotApiController.java`
  — extend the `switch (agentId)` at line 60 with `case "ai_hub" -> "ai_hub"`.
  No ASK/BUILD split (v1 is ASK-only server-side).

### AG-UI state contract (server reads)

On every chat turn, the client sends `state` with at least:

```
{
  "source": "AI_HUB",
  "workspaceId": <string>,   // decimal-formatted Long; server parses via asLong() helper
  "mode": "ASK" | "BUILD",
  "currentTabs": [{"fileId": <string>, "name": <string>, "viewMode": "editor"|"preview"|"split"}, ...],
  "activeFileId": <string | null>
}
```

All IDs cross the AG-UI boundary as strings for JSON-safety (JSON numbers > 2^53 are unsafe, and the
client-side `openFileTab` tool schema is `z.string()` by the same principle). The server parses them
back to `Long` using the existing `asLong()` helper in `FilesSpringAIAgent`.

`AiHubSpringAIAgent.createSystemMessage` reads `currentTabs` and `activeFileId` and appends
them as AG-UI `Context` entries (titled "Open Tabs" and "Active File"), then delegates to the same
string-formatting used by other agents.

## Client-side design

### New route and nav entry

- Route path: `/automation/ai-hub`. Registered alongside existing automation routes
  (`/automation/projects`, `/automation/workspace-files`, etc.).
- Top-level nav entry in the Automation sidebar, EE-gated by the same flag used to show the copilot
  button (feature flag / edition check).

### New files (all under `client/src/pages/automation/ai-hub/`)

- `AiHub.tsx` — route-level page. Lays out two panes with a draggable divider (reuse
  existing resizable-pane primitive if one exists; otherwise simple CSS with a hover handle).
  Renders `CopilotPanel` on the left with the new `widthMode="fill"` prop, and
  `AiHubResourcePanel` on the right when `rightPanelOpen` in the tabs store is true.
  Collapse/expand button in the divider.
- `AiHubResourcePanel.tsx` — header with a tab strip (scrollable, each tab has a close
  button), a "+" button that opens an asset-file picker, a collapse-right-panel "✕" button, and a
  body that renders the active tab's `AiHubFileViewer`.
- `AiHubFileViewer.tsx` — three view modes selected via `ToggleGroup` in the viewer header:
    - **Editor**: `MonacoEditorWrapper` (existing component) bound to the file's text content.
      Autosave-on-blur via the existing asset-file API.
    - **Preview**: rendered output. Markdown via `react-markdown` (already in `package.json`).
      HTML sandboxed in an `<iframe sandbox>` to prevent script execution. Unknown binary types
      show metadata only (filename, size, type).
    - **Split**: editor on the left, preview on the right, live-synced.

    **Default view mode rule** (applied when a tab is first opened and no prior mode is stored):
    - `.md` / `.markdown` / `.html` / `.htm` → `preview`
    - all other text types (`.txt`, `.json`, `.yaml`, `.java`, `.py`, `.sql`, …) → `editor`
    - unknown binary types → `preview` (shows the metadata placeholder)

    Once a user switches modes on a tab, that per-tab mode persists in the store for the session.
- `stores/useAiHubTabsStore.ts` — zustand store:
  ```ts
  interface AiHubTabI {
      id: string;       // internal tab id, distinct from fileId
      fileId: string;
      name: string;
      viewMode: 'editor' | 'preview' | 'split';
  }

  interface AiHubTabsStateI {
      activeTabId: string | undefined;
      closeTab: (tabId: string) => void;
      openFileTab: (fileId: string, name: string) => string; // returns tabId
      openTabs: AiHubTabI[];
      rightPanelOpen: boolean;
      setActiveTab: (tabId: string) => void;
      setRightPanelOpen: (open: boolean) => void;
      setViewMode: (tabId: string, mode: 'editor' | 'preview' | 'split') => void;
  }
  ```
  Opening a file whose `fileId` already has a tab focuses that tab rather than creating a duplicate.
  Opening the first tab also sets `rightPanelOpen = true`.
- `runtime-providers/AiHubRuntimeProvider.tsx` — wraps `<Thread />` for this route.
  Registers a client-side AG-UI tool:
  ```ts
  {
      name: 'openFileTab',
      description: 'Open a workspace file in the AI Hub resource panel so the user can see it.',
      parameters: z.object({
          fileId: z.string(),
          name: z.string(),
      }),
      execute: async ({fileId, name}) => {
          const tabId = useAiHubTabsStore.getState().openFileTab(fileId, name);

          return {opened: true, tabId};
      },
  }
  ```
  Injects `currentTabs`, `activeFileId`, `workspaceId`, `mode` into AG-UI `state` on each turn by
  reading from `useAiHubTabsStore` and the workspace/copilot stores.

### Modifications

- `client/src/shared/components/copilot/CopilotPanel.tsx` — add a `widthMode: 'fixed' | 'fill'`
  prop, default `'fixed'`. In `'fill'` mode, replace the hardcoded `w-[450px]` at line 66 with
  `h-full w-full` and skip the outer width-transition wrapper around line 174 (which is also
  hardcoded to `w-[450px]` / `w-0`). The existing call sites keep the default and are unaffected.
- `client/src/shared/components/copilot/stores/useCopilotStore.ts` — add `AI_HUB =
  'AI_HUB'` to the `Source` enum.
- Automation sidebar nav config — add a top-level entry under `/automation/` for AI Hub,
  EE-gated. The planning phase locates the canonical nav-config file by grepping for existing
  entries (e.g. `workspace-files` or `mcp-servers`) and inserts the new entry following the same
  convention.
- Router config — register `/automation/ai-hub` pointing at `AiHub.tsx`. EE gate
  applied at route level.

### Runtime flow examples

**Agent creates and opens a new file**

1. User types "write a technical spec for X as a markdown file" in the chat.
2. `AiHubRuntimeProvider` sends the turn over SSE to `/internal/ai/chat/ai_hub`
   with `state = { source: "AI_HUB", workspaceId, mode: "ASK", currentTabs: [], activeFileId: null }`.
3. Agent calls `createAssetFile` (server-side tool) → file persists; tool returns `{fileId, name}`.
4. Agent calls `openFileTab({fileId, name})` (client-side tool) → browser executes → tabs store
   inserts a new tab, sets it active, opens the right panel.
5. Assistant message streams back summarizing the file. User sees the file in the right pane in
   Preview mode (default for markdown).

**User opens an existing file manually, then asks the agent about it**

1. User clicks "+" in the resource panel → file picker lists existing asset files → user selects
   one → tabs store opens the file.
2. User types "rewrite the introduction to be more concise" in the chat.
3. Next chat turn includes `state.activeFileId = <that fileId>`, `state.currentTabs = [...]`.
4. `AiHubSpringAIAgent.createSystemMessage` appends "Active File: fileId=…" and
   "Open Tabs: […]" as context entries.
5. Agent calls `getAssetFileContent`, computes a rewrite, calls `createAssetFile` with updated
   content (same filename to overwrite, or new filename per the existing asset-file semantics).
6. Agent calls `openFileTab` for the resulting file (same fileId if in-place edit, new one if
   renamed) — the store focuses the existing tab rather than duplicating.

## Testing

### Server

- `AiHubSpringAIAgentTest` under
  `server/ee/libs/ai/ai-copilot/ai-copilot-service/src/test/java/com/bytechef/ee/ai/copilot/agent/`
  mirroring `FilesSpringAIAgentTest`:
    - `testBuildInvocationContext` — given a `RunAgentInput` with `state.workspaceId`, returns a
      `WorkspaceInvocationContext` with the right workspaceId and `Source.AI_HUB.ordinal()`.
    - `testCreateSystemMessage` — given `state.currentTabs` and `state.activeFileId`, the generated
      system message contains "Open Tabs" and "Active File" context blocks with the expected
      values.
    - `testCreateSystemMessageWithEmptyTabs` — missing/empty tabs produce no context block (not a
      malformed one).

### Client

- `useAiHubTabsStore.test.ts`:
    - `openFileTab` adds a new tab, sets it active, opens the right panel.
    - Opening a fileId that already has a tab focuses the existing tab (no duplicate).
    - `closeTab` removes the tab; closing the active tab picks the next or previous as active; closing
      the last tab leaves `activeTabId` undefined but keeps the right panel state.
    - `setViewMode` updates only the target tab's mode.
- `AiHubFileViewer.test.tsx`:
    - Renders Monaco editor in Editor mode.
    - Renders markdown preview for `.md` files in Preview mode.
    - Renders both panes in Split mode and keeps preview in sync with editor edits (debounced).
    - Unknown binary file type in Preview mode renders a metadata placeholder, not a crash.
- `AiHubRuntimeProvider.test.tsx`:
    - `openFileTab` client-side tool, when invoked with `{fileId, name}`, updates the tabs store and
      returns `{opened: true, tabId: <string>}`.
    - The injected `state` on a chat turn reflects current store contents (`currentTabs`,
      `activeFileId`) and workspace/mode.
- `CopilotPanel.test.tsx` — new cases for `widthMode="fill"`: panel fills its container, no
  hardcoded width constraint, outer transition wrapper is skipped.

### Integration / manual

- Full flow on the dev server: open `/automation/ai-hub`, ask agent to create a markdown
  file, verify it appears, toggle view modes, close tab, reopen via "+" picker.
- EE gating: confirm route returns 404 / nav entry hidden on CE build.

## Risks and open questions (to resolve during planning)

- **Client-side tool declaration in AG-UI**: the ByteChef copilot currently uses server-side
  `ToolCallback` beans. We need to confirm how the `@assistant-ui/react` runtime declares and
  dispatches a client-side tool in this stack, and whether the agent prompt needs to list it
  explicitly or whether the runtime forwards the tool schema automatically. Fallback plan if
  unsupported: add a server-side `openFileTab` tool that no-ops but emits an AG-UI `CUSTOM` event
  the client intercepts to update the store.
- **Resize divider**: confirm whether an existing resizable-pane primitive exists in
  `client/src/components/ui/` before building one. If none exists, use `react-resizable-panels`
  (the shadcn-recommended library) and install per the project's existing dependency conventions.
- **File picker UI**: v1 can reuse a compact wrapper over the existing asset-files list. A full
  picker dialog design is out of scope.
- **EE gating for route**: identify the canonical edition check used for the existing copilot
  button/route and apply the same.

## Phase 2+ preview (not in scope, documented for continuity)

- Add workflow, data-table, and knowledge-base resource types to the tabs store and resource panel.
  Each new type is a new viewer component plus a new `openXxxTab` client-side tool.
- Add more tool callbacks to the ai-hub agent so it can query/mutate those domains.
- `@`-mentions in the chat for workspace resources.
- Research agent as a new `SpringAIAgent` with web-search tools, producing reports as files via the
  same `openFileTab` path.
- Scheduled prompt-jobs built on `platform-scheduler`.

### Subagents orchestration (future)

When Phase 2+ needs the ai-hub agent to delegate specialized work (research, workflow
authoring, data analysis, etc.) to dedicated subagents with isolated context windows, use the
`TaskTool` pattern from Spring AI:

- Artifact: `org.springaicommunity:spring-ai-agent-utils` (v0.4.2+).
- Entry point: `TaskToolCallbackProvider.builder()` — produces a `ToolCallback` that routes
  delegation calls to subagent references based on their descriptions.
- Each subagent runs in its own context window, receives a focused prompt, and returns only the
  synthesized result to the parent. Parallel execution, per-subagent model routing, and
  hierarchical control are all supported.
- Reference: <https://spring.io/blog/2026/01/27/spring-ai-agentic-patterns-4-task-subagents>.

In v1 the ai-hub agent is single-agent (one `AiHubSpringAIAgent` with direct tool
callbacks), so `TaskTool` is not wired up. Add it the first time a Phase 2+ subproject needs
delegation — do not introduce it preemptively.
