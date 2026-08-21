# Multiple Triggers in the Workflow Editor — Design

**Date:** 2026-06-28
**Status:** Approved (design)
**Area:** Client — `client/src/pages/platform/workflow-editor`

## Problem

The workflow JSON definition already supports multiple triggers (`triggers` is an
array). The visual workflow editor, however, assumes exactly **one** trigger
everywhere: it renders `triggers[0]`, marks node index `0` as the trigger, and on
every trigger drop it **replaces** the entire array with a single-element array.
Users cannot add a second trigger.

This design makes the editor render, add, configure, position, and delete an
arbitrary number of triggers, while preserving today's empty-state behavior.

## Decisions (from brainstorming)

1. **Layout model: fan-in to first task.** Each trigger is an independent entry
   point, shown side-by-side at the top of the canvas; all triggers connect down
   into the same first task. Any trigger can start the workflow. (n8n/Make
   convention.)
2. **Add interaction: drop *and* click.** A "+" trigger slot accepts both a
   dropped trigger component and a click that opens the trigger-filtered picker.
   Both append a new trigger. Dropping onto an existing trigger node keeps today's
   **replace** behavior.
3. **Empty state: keep the Manual placeholder.** Triggers are individually
   deletable; when the last real trigger is removed, the editor falls back to the
   synthetic "Manual" placeholder node, preserving the invariant that the canvas
   always shows at least one entry node.
4. **"+" slot node type:** a dedicated lightweight `triggerPlaceholder` node type.
5. **New-trigger naming:** sequential `trigger_2`, `trigger_3`, … derived from the
   highest existing `trigger_N` (matches the existing `trigger_1` convention).

## Current behavior (baseline)

Key single-trigger assumptions, with file references:

| File | Reference | Assumption |
|------|-----------|------------|
| `hooks/useLayout.tsx` | ~202 | `triggers?.[0]?.type` — component name from first trigger only |
| `hooks/useLayout.tsx` | ~209–217 | builds one `triggerNode`, `allNodes = [triggerNode]` |
| `utils/layoutUtils.tsx` | `convertTaskToNode` ~196 | `trigger: index === 0` — positional trigger-ness |
| `utils/layoutUtils.tsx` | `createDefaultNodes` ~377–399 | synthetic "Manual" trigger when none |
| `hooks/useWorkflowEditorCanvas.ts` | ~152–210 | trigger drop → `handleDropOnTriggerNode` (replace) |
| `hooks/useHandleDrop.tsx` | ~241–255 | `handleDropOnTriggerNode` |
| `utils/saveWorkflowDefinition.ts` | ~72–97 | `{triggers: [newTrigger]}` — **replaces array** |
| `utils/extractDefinitionPositions.ts` | ~37–39 | `parsed.triggers?.[0]` |
| `utils/saveWorkflowNodesPosition.ts` | ~200–215 | `triggers?.[0]?.name` |
| `utils/removeWorkflowNodePosition.ts` | ~160–171 | `triggers?.[0]?.name` |
| `utils/clearAllNodePositions.ts` | similar | `triggers?.[0]` |
| `stores/useWorkflowDataStore.ts` | init | `workflow.triggers?.[0] || createDefaultNodes(...)` |

The data model (`WorkflowTrigger`, `WorkflowDefinitionType.triggers?: Array<...>`)
already supports an array — **no data-model changes required.**

## Design

### 1. Explicit trigger-ness in node building

`convertTaskToNode` currently infers `trigger: index === 0`. Change it to take an
explicit `isTrigger` flag (or a small options object), so trigger-ness no longer
depends on array position. Task nodes pass `isTrigger: false`; trigger nodes pass
`isTrigger: true`.

### 2. Build N trigger nodes in `useLayout`

Replace the single-`triggerNode` construction with a map over the whole `triggers`
array:

- For each trigger, resolve its component definition from the **bulk
  component-definitions list the editor already loads**, not a per-trigger query.
  React hooks cannot be called in a variable-length loop, so the existing single
  `useGetComponentDefinition(triggers[0])` query is replaced by a lookup into the
  already-available definitions collection (indexed by component name) so each
  heterogeneous trigger gets its own icon/title.
- When `triggers` is empty, fall back to `createDefaultNodes(...)[0]` (the Manual
  placeholder) exactly as today.
- Append one `triggerPlaceholder` ("+") node to the trigger row.

`allNodes` becomes `[...triggerNodes, triggerPlaceholderNode, ...taskNodes]`.

### 3. `triggerPlaceholder` node type ("+" slot)

A new lightweight node type rendered in the trigger row:

- **Drop** a trigger component onto it → append (see §5).
- **Click** it → open the existing trigger-filtered component-selection popover →
  append on selection.
- Has **no outgoing edge**. It is positioned in the trigger row via a post-dagre
  constraint (reuse the `postDagreConstraints.ts` pattern), placed to the
  right of the rightmost trigger in `TB` and below the lowest trigger in `LR`,
  because dagre would otherwise require a phantom edge to rank it.

### 4. Fan-in edges

Each real trigger node (and the Manual placeholder when present) gets an edge into
the **first task** node, or into the final task `+` placeholder when there are no
tasks yet. Edge building (`useLayout` edge section) changes from "edge from the
single trigger" to "one edge per trigger into the first downstream node." The
`triggerPlaceholder` node is excluded from edge generation.

### 5. Save: append vs replace

`saveWorkflowDefinition` stops hardcoding `{triggers: [newTrigger]}`:

- **Append path** (drop/click on the "+" slot): read current `triggers`, generate
  a unique name (`trigger_N` from the max existing trigger index, also checked
  against task names to avoid collisions), push the new trigger, write the **full
  array**.
- **Replace path** (drop on an existing trigger node): swap the targeted trigger
  in place **by name**, write the full array.

The drop handlers distinguish the two cases: `handleDropOnTriggerNode` (replace,
existing) and a new `handleDropOnTriggerPlaceholder` (append), wired from
`useWorkflowEditorCanvas.onDrop` based on the drop target's node type. The click
path on the placeholder reuses the same append logic.

### 6. Position persistence by name

`extractDefinitionPositions`, `saveWorkflowNodesPosition`,
`removeWorkflowNodePosition`, and `clearAllNodePositions` change from indexing
`triggers[0]` to **find-by-name across all triggers** (mirroring how they already
locate tasks by name), so each trigger's `metadata.ui.nodePosition` is saved and
restored independently.

### 7. Deletion

Each trigger node is independently deletable through the existing node-delete path,
which removes the trigger from the array by name. When the array becomes empty, the
Manual placeholder reappears (the empty-state fallback in §2).

### 8. Both layout directions

All of the above must work in `TB` (triggers side-by-side at top, "+" to the right)
and `LR` (triggers stacked at left, "+" below). The post-dagre placement of the
"+" slot is direction-aware.

## Out of scope

- Independent per-trigger branches (each trigger starting its own task chain).
- Changes to the server, workflow definition schema, or validator.
- Reordering triggers via drag.

## Testing

Unit tests (Vitest):

- Node building: N trigger nodes produced from an N-element `triggers` array, each
  with the correct per-component icon/title; `triggerPlaceholder` appended.
- Empty `triggers` → single Manual placeholder + `triggerPlaceholder`.
- Edges: one fan-in edge per trigger into the first task; none from the "+" slot.
- Save: append generates a unique `trigger_N` and preserves existing triggers;
  replace swaps by name without dropping siblings.
- Unique-name generation against both trigger and task names.
- Position save/restore/remove by name for a non-first trigger.
- `convertTaskToNode` honors the explicit `isTrigger` flag.

Existing duplicate-node-name validation already covers trigger/task name
collisions and needs no change beyond confirming it sees all triggers.

## Risks / watch-items

- **Per-trigger definition resolution** is the main non-mechanical change; if the
  editor does not already load all component definitions in scope, that load must
  be ensured before this works for heterogeneous triggers.
- **Dagre ranking of the "+" slot** — verify the post-dagre constraint keeps it
  aligned with the trigger rank in both directions and does not overlap triggers.
- Any other consumers of `triggers[0]` outside the editor (e.g.
  `AutomationWorkflowProjectWorkflowListItem`, `EmbeddableWorkflowEditor` chat-trigger
  check) should be reviewed but are display-only and likely fine; confirm during
  implementation.