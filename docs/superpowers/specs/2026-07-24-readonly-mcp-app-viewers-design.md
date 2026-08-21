# Feature — Read-only MCP App viewers (data table, code workflow, custom component, file)

**Date:** 2026-07-24
**Status:** Design — approved for planning
**Author:** Ivica Cardic

## Context

There is one MCP App today: `mcp-apps/workflow-editor/`, a self-contained HTML "workflow viewer"
served by `McpAppWorkflowEditor` as the `ui://bytechef/workflow-editor` resource on the **management**
MCP server. Workflow-building tools opt in via `_meta.ui.resourceUri`; the host (Claude Desktop /
Claude.ai) renders the iframe and pushes each matching tool result to it. The widget is a pure
projection of `structuredContent.definition` — no tool calls, no fetches beyond icon `<img>` loads.

This feature extends that pattern to four more `openResourceTab` resource types, read-only ("phase
1"): **DATA_TABLE**, **CODE_WORKFLOW**, **CUSTOM_COMPONENT**, **FILE**. Preps A + B already moved the
four backing read tools into `automation-ai-tool` (data-table + file are CE; code-workflow +
custom-component are EE).

## Goal

- Four new self-contained widgets under `mcp-apps/` (one per resource type — separate bundles).
- Each rendered by the management MCP server as its own `ui://bytechef/<name>` resource.
- The four backing read tools surfaced as first-class management-MCP tools, each carrying
  `_meta.ui.resourceUri` for its viewer and emitting `structuredContent` in the viewer's shape —
  exactly what `getWorkflow` already does.
- A generalized SPI so EE contributors can attach the UI descriptor without the CE server importing
  EE result-shape knowledge.

## Non-goals

- No write / edit interactivity (read-only).
- KNOWLEDGE_BASE and SKILL viewers (no full-content read tool; separate future work).
- The automation MCP server surface (management only, like the workflow viewer).
- Images / PDF / binary in the file viewer, and CSV-as-table (text mimes only, phase 1).

## Widget layer (`mcp-apps/`)

Four sibling Vite `viteSingleFile` bundles, each following `workflow-editor` exactly (host bridge via
`@modelcontextprotocol/ext-apps` `App`, `ontoolresult` registered pre-connect, `connecting/waiting/
ready/error` states, `__pushToolResult` dev hook, fixture mode):

- `mcp-apps/data-table-viewer/` → `ui://bytechef/data-table-viewer`. Renders a plain HTML table;
  reads `structuredContent.{name, rows}` (columns inferred from row keys; ≤50 rows, the tool cap).
- `mcp-apps/code-workflow-viewer/` → `ui://bytechef/code-workflow-viewer`. Reads
  `structuredContent.{name, language, source}`; highlight.js (sync, offline-safe under CSP).
- `mcp-apps/custom-component-viewer/` → `ui://bytechef/custom-component-viewer`. Same shape as
  code-workflow (`language: "java"`); separate bundle per the one-widget-per-type decision.
- `mcp-apps/file-viewer/` → `ui://bytechef/file-viewer`. Reads `structuredContent.{name, mimeType,
  content}`; renders text with highlight.js keyed off `mimeType` (text mimes only, ≤1 MB — the tool's
  existing limits).

A small shared `mcp-apps/shared/` module carries the host bridge + base Tailwind tokens so the four
new widgets do not each copy the boilerplate; `workflow-editor` stays untouched. None of these four
need component icons, so — unlike the workflow viewer — their serving helpers need no `publicUrl`
injection and no CSP `img-src`.

## Serving layer (CE, `platform-mcp-server-support` + `ai-mcp-server`)

- Four `McpApp*Viewer` helper classes (copy of `McpAppWorkflowEditor`, minus icon/CSP): each owns
  `RESOURCE_URI = ui://bytechef/<name>`, mime `text/html;profile=mcp-app`, classpath slot
  `mcp-apps/<name>.html`, and `getResourceSpecifications()` (no `publicUrl` param).
- Gradle: four more `build<Name>` Exec tasks + four `processResources` `from(dist/index.html) rename
  <name>.html` blocks (Node-gated, absent bundle → server still boots).
- Register all four resources on the management server (Streamable HTTP + per-secretKey SSE).

## Backend: SPI-driven `_meta.ui` + `structuredContent`

The workflow-editor UI-attach in `ManagementMcpServerConfiguration` is currently hardcoded to the
three workflow tool names with a CE `extractDefinition` shaper. Generalize it:

- **Extend `McpServerToolCallbackContributor`** (CE, `ai-mcp-server-api`) so a contributor can
  attach, per tool, an optional UI descriptor: `{ uiResourceUri: String, structuredContentShaper:
  Function<String, Object> }`. CE applies both mechanically — rebuilds the immutable `McpSchema.Tool`
  with `_meta.ui.resourceUri` and wraps the call handler to inject `structuredContent` from the
  shaper. CE never imports EE result-shape types; it just invokes the provided shaper.
- **Refactor** the CE workflow-specific `attachWorkflowEditorUi` / `withDefinitionStructuredContent`
  into a generic `attachMcpAppUi(descriptor)` used by both the existing workflow tools (a CE
  descriptor) and the four new contributed tools (SPI descriptors).
- **Contribute the four read tools** with descriptors, from their post-Prep-B homes:
  - CE contributor in CE `automation-ai-tool` → `queryDataTable` (data-table-viewer),
    `getAssetFileContent` (file-viewer).
  - EE contributor in EE `automation-ai-tool` → `getCodeWorkflowSource` (code-workflow-viewer),
    `getCustomComponentSource` (custom-component-viewer).
  Each descriptor's `structuredContentShaper` converts the tool's JSON result into the widget's
  `structuredContent` shape (§Widget layer). The tools must be surfaced as first-class management-MCP
  tools (added to the server's tool list), not just viewer-backing — mirroring `getWorkflow`.

## Testing

- Per widget: typecheck + build (`tsc --noEmit && vite build`) and a `?fixture` sample render.
- CE: `McpApp*ViewerTest` (resource served, mime, classpath-absent → `List.of()`); a
  `ManagementMcpServer*Test` asserting each of the four tools carries `_meta.ui` + shaped
  `structuredContent`, and that the generalized `attachMcpAppUi` still produces the workflow tools'
  existing `_meta`.
- Contributor tests (CE + EE) asserting the four tools are contributed with their descriptors.

## Risks

- **Bundle size / CSP.** highlight.js must be inlined (no async grammar fetch) to satisfy the
  single-file + strict-CSP constraint. Keep the language set curated.
- **SPI change blast radius.** Extending `McpServerToolCallbackContributor` must stay
  backward-compatible — existing contributors (managers, copilot specialists) return no descriptor
  and are unaffected (default null → no UI attach).
- **structuredContent shape drift.** Each shaper must match its widget's expected keys; a mismatch
  renders an empty viewer. Pin the shape in both the widget's `extract` and the shaper via tests.
- **Provenance sync.** The workflow-graph port is already tracked as "the third copy"; the
  data-table/code/file widgets are new and small, but any shared graph code must note its origin.

## Rollout

Feature is additive: new widgets, new resources, four tools gain `_meta.ui` + `structuredContent`.
Hosts that don't support MCP Apps ignore the `ui://` resources; the tools still return their text
content. No migration, no flag.
