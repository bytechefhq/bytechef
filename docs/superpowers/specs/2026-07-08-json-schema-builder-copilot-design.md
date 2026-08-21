# JSON Schema Builder Copilot — Design

**Date:** 2026-07-08
**Status:** Approved (pending spec review)

## Summary

Add the existing `CopilotPanel` chat as a **toggleable** right-hand column of the JSON
Schema Builder sheet, backed by a **new `JSON_SCHEMA` chat agent** that sees the live
schema on every turn and **auto-applies full-schema updates** back into the builder.

Today the only AI in the sheet is a stateless one-shot "Generate with AI" bar
(`JsonSchemaCopilotBar`) that replaces the whole schema from a prompt and never sees the
current schema. This adds conversational, context-aware refinement without removing that
bar.

## Decisions (locked)

| Question | Decision |
|----------|----------|
| Capability | Chat **and apply changes** back into the builder |
| Backend | **New `JSON_SCHEMA` chat agent** (schema-aware prompt + apply tool) |
| Apply mechanism | Agent returns the **complete** schema; builder **auto-applies** (no confirm) |
| Panel visibility | **Toggle** via a header button (sheet widens when open) |
| Existing "Generate with AI" bar | **Keep** for now (quick path); revisit later |

## Architecture

Reuse the `PropertyCodeEditorDialog` template almost 1:1 — it already embeds this exact
`CopilotPanel` beside an editor, seeds context through `useCopilotStore`, and applies
results back via a tool-result handler.

### Client

**Layout & toggle — `PropertyJsonSchemaBuilderSheet.tsx`**
- Restructure `SheetContent` from a single vertical column into a **horizontal flex row**:
  - left = existing `Tabs` block (Designer / Code Editor), `flex-1 min-w-0`;
  - right = `<CopilotPanel open={copilotPanelOpen} source={Source.JSON_SCHEMA} onClose={...} className="h-full border-l" />` (the width-collapse variant).
- Add a **Copilot/Sparkles toggle button** in the header (beside the Designer/Code Editor
  tabs) driving a **local** `copilotPanelOpen` state — not the global `useCopilotPanelStore`
  (dialog embeds own their local open state, per `PropertyCodeEditorDialog`).
- When open, widen the sheet's `sm:max-w-*` cap so the builder isn't cramped.
- The toggle button renders only when the copilot is available — same gate as today's
  generator: `ai.copilot.enabled && ff-1570 && workflowId && workflowNodeName &&
  environmentId`. When unavailable, the sheet is exactly as it is today.

**Conversation lifecycle & context**
- On panel **open**: `saveConversationState()` → `resetMessages()` →
  `generateConversationId()` → `setContext({ source: JSON_SCHEMA, mode: MODE.ASK,
  parameters: { propertyPath, title } })`.
- **Live schema each turn:** register a `useCopilotStateContributorRegistry` contributor
  that emits the current `localSchema`, so the agent always sees the latest schema even
  after the user hand-edits pills (not only what was set at open time).
- On panel **close**: `restoreConversationState()` (restores the suspended outer
  workflow-editor conversation).
- Add `JSON_SCHEMA` to the client `Source` enum
  (`@/shared/components/copilot/stores/useCopilotStore`), which routes the chat to
  `/api/platform/internal/ai/chat/json_schema`.

**Apply-back (auto, full schema)**
- The agent calls an `applyJsonSchema({ schema })` tool. A
  `useCopilotToolResultHandlerRegistry` handler receives the schema and calls the sheet's
  existing `handleSchemaChange(schema)` → updates `localSchema` (live builder re-render)
  and fires `onChange`. No confirm dialog (auto-apply); the user can keep chatting to
  adjust.

### Server

The chat endpoint is already generic: `CopilotApiController` exposes
`@PostMapping("/ai/chat/{agentId}")`, and agents are defined by a server-side `Source`
enum + `CopilotAgentType` + per-agent tool callbacks in `server/libs/ai/ai-copilot/`
(e.g. `CodeEditorAgentToolCallback`, `WorkflowEditorAgentToolCallback`).

New agent, mirroring the code-editor agent:
- Add `JSON_SCHEMA` to the server `Source` enum
  (`server/libs/ai/ai-copilot/ai-copilot-api/.../util/Source.java`) and the matching
  `CopilotAgentType`.
- Add a `JsonSchemaAgentToolCallback` exposing one tool: `applyJsonSchema(schema)` that
  returns the complete updated schema (shape compatible with the client's
  `SchemaRecordType`).
- Provide a **schema-aware system prompt** (understands JSON Schema and this builder's
  `SchemaRecordType` conventions), receiving the current schema from the per-turn client
  state contributor.

## Data flow (one turn)

1. User types a message in the panel.
2. `CopilotRuntimeProvider` sends the turn to `/ai/chat/json_schema`, merging
   `context.parameters` + the state-contributor output (current `localSchema`) +
   environment/workspace/LLM into the agent state.
3. Agent responds; when it wants to change the schema it calls `applyJsonSchema(schema)`.
4. Client tool-result handler applies `schema` via `handleSchemaChange` → builder updates
   live + `onChange` propagates to the property value.

## Error handling

- Copilot unavailable / no AI provider → toggle button hidden; sheet unchanged (existing
  `CopilotPanel` empty-state also covers the no-provider case if opened).
- `applyJsonSchema` returns an invalid/unparseable schema → handler rejects it and surfaces
  an error in the chat rather than corrupting `localSchema` (reuse the validation posture
  of the existing generator, which JSON-parses and validates before applying).
- Panel close mid-stream → `restoreConversationState()` still runs so the outer
  conversation is not lost.

## Testing

**Client**
- Context is set on panel open and `restoreConversationState()` runs on close.
- State contributor emits the current `localSchema` (including after a manual pill edit).
- Tool-result handler applies a returned schema to `localSchema`; an invalid schema is
  rejected without mutating state.
- Toggle button hidden when the availability gate is off.
- (Zustand-store test patterns per CLAUDE.md; `vi.hoisted` for store mocks.)

**Server**
- `json_schema` agent responds via `/ai/chat/{agentId}`.
- `applyJsonSchema` tool round-trips a valid schema and rejects an invalid one.
- `EnumOrdinalStabilityTest`-style stability if the server `Source`/`CopilotAgentType`
  enums are ordinal-pinned — append the new value at the end.

## Out of scope / later

- Removing or merging the one-shot "Generate with AI" bar (kept for now).
- Incremental/patch-based schema edits (this design always returns the full schema).
- Per-source chat suggestions (currently `COPILOT_SUGGESTIONS` are hardcoded and
  workflow-oriented); schema-oriented suggestions can be added later.

## Key files

**Client**
- `client/src/pages/platform/workflow-editor/components/properties/components/property-json-schema-builder/PropertyJsonSchemaBuilderSheet.tsx` — layout, toggle, lifecycle.
- `client/src/shared/components/copilot/CopilotPanel.tsx` — reused panel.
- `client/src/shared/components/copilot/stores/useCopilotStore.ts` — `Source` enum, context, save/restore.
- `client/src/shared/components/copilot/runtime-providers/CopilotRuntimeProvider.tsx` — state merge / endpoint selection.
- `.../property-code-editor/property-code-editor-dialog/PropertyCodeEditorDialog.tsx` + `usePropertyCodeEditorDialog*.ts` — reference implementation.

**Server**
- `server/ee/libs/ai/ai-copilot/ai-copilot-rest/.../CopilotApiController.java` — `/ai/chat/{agentId}`.
- `server/libs/ai/ai-copilot/ai-copilot-api/.../util/Source.java` — server `Source` enum.
- `server/libs/ai/ai-copilot/ai-copilot-tool/.../CodeEditorAgentToolCallback.java`, `CopilotAgentType.java` — agent/tool pattern to mirror.
