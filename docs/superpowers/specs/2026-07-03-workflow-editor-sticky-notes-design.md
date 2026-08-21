# Workflow Editor Sticky Notes — Design

**Date:** 2026-07-03
**Status:** Approved for implementation (autonomous session — decisions documented with rationale, review welcome)

## Goal

Let users drop free-floating sticky notes onto the workflow editor canvas to annotate workflows
(explain a branch, leave a TODO for a teammate, document a decision). Notes are purely decorative:
they never execute, never participate in auto-layout, and never affect task semantics.

## Persistence — chosen approach

All sticky note data (position, dimensions, content, color) lives under the **workflow-level
`metadata.ui` key in the definition JSON**, mirroring the per-task `metadata.ui.nodePosition`
convention for custom UI data:

```json
{
    "label": "My Workflow",
    "metadata": {
        "ui": {
            "stickyNotes": [
                {
                    "color": "yellow",
                    "content": "Retry logic lives in the loop below",
                    "id": "stickyNote_1a2b3c4d",
                    "position": {"x": 120, "y": 240},
                    "size": {"height": 160, "width": 240}
                }
            ]
        }
    },
    "tasks": [...]
}
```

When the last note is deleted, the empty `stickyNotes`/`ui`/`metadata` containers are pruned
(without touching unrelated keys another feature may have put there).

Why this works with zero server changes:

- The server stores the definition string verbatim; `Workflow.java` parses recognized keys and
  keeps unknown top-level keys (workflow-level `metadata` is one) in its `extensions` map, so
  nothing is stripped. It does not collide with the separate DB-level `Workflow.metadata` field.
- Both client save paths (`executeWorkflowMutation` in `saveWorkflowDefinition.ts` and
  `saveWorkflowNodesPosition.ts`) parse the full definition and spread it back, so the key
  survives every task/trigger/position save.
- Notes travel with the workflow: export, duplicate, templates, and the read-only viewer all see
  them for free.

### Alternatives rejected

1. **Pseudo-tasks** (a `stickyNote/v1` component) — the engine would try to execute them, they
   would pollute the task count, validation, and the Atlas execution model. Server-side filtering
   everywhere is far more invasive than a decorative key.
2. **Separate persistence** (new table + REST endpoint) — heavy server work, and notes would not
   travel with workflow export/import/templates. Notes are part of the workflow document, so they
   belong in the definition.
3. **Top-level `stickyNotes` key** — implemented first, then moved under `metadata.ui` so all
   custom UI data in the definition follows one convention (matching per-task `metadata.ui`).

## Canvas integration

- New ReactFlow node type `stickyNote` → `StickyNoteNode`, registered in the single `nodeTypes`
  map in `useWorkflowEditorCanvas.ts` (shared by the editable and read-only editors).
- Notes are **excluded from dagre**. `useLayout`'s layout effect appends note nodes (built fresh
  from the current definition) to the computed node set after `getLayoutElements` resolves, and
  includes their ids in the prune set so they are not removed as "unknown" nodes.
- Notes follow the same cross-axis-shift contract as pinned task nodes: stored position is
  canonical; rendering adds `savedPositionCrossAxisShift` on the cross axis; drag-stop saving
  subtracts it (mirrors `applySavedPositions` / `handleNodeDragStop`).
- A small reconciler effect (in `useStickyNotes`) watches the `stickyNotes` portion of the store
  definition and syncs note nodes into the canvas. This covers changes that do NOT re-trigger the
  layout effect: undo/redo (zundo history is definition-equality based, so note edits are already
  undoable), and note add/edit/delete from other code paths. It skips work while a node is being
  dragged.
- Notes render with a negative z-index so an overlapping note never blocks interaction with task
  nodes or edges.

## UX

- **Add:** a "Add note" button (`StickyNoteIcon`) in the bottom-left `WorkflowEditorToolbar`,
  hidden in read-only mode. New notes appear at the viewport center (offset a few px per existing
  note so consecutive adds don't stack exactly).
- **Move:** notes set per-node `draggable: true`, which overrides the canvas-level lock — the lock
  exists to protect auto-layouted task nodes; free-floating notes are exempt by design. Dragging is
  disabled in read-only mode.
- **Edit:** double-click enters edit mode (textarea with ReactFlow's `nodrag` class); blur or
  Escape commits. Placeholder text invites typing on a fresh note.
- **Color:** 5 pastel colors (yellow, blue, green, pink, purple); a small palette appears in the
  note's hover/selection toolbar. Default yellow.
- **Resize:** `NodeResizer` (from `@xyflow/react`), visible when selected; min 150×100, default
  240×160; size persisted on resize end.
- **Delete:** an X button in the note's hover toolbar (canvas `deleteKeyCode` is null, so no
  keyboard delete path exists to intercept).
- **Read-only viewer:** notes render with content and color but no drag/edit/resize/delete.

## Data flow

All note mutations funnel through one utility, `saveStickyNotes` (modeled on
`saveWorkflowNodesPosition`): parse store definition → apply updater to the `stickyNotes` array
(key removed when the array empties) → write definition back to the store (recorded as an undo
step by the existing zundo equality on `definition`) → fire the update mutation, honoring the
`workflowMutationGuard` pending-definition queue so overlapping saves are never lost; revert
without history on error.

## Components & files

| File | Change |
| --- | --- |
| `client/src/shared/types.ts` | `WorkflowStickyNoteType`; `stickyNotes?` on `WorkflowDefinitionType` |
| `client/src/pages/platform/workflow-editor/utils/stickyNoteUtils.ts` | new — extract/build/save/position helpers |
| `client/src/pages/platform/workflow-editor/nodes/StickyNoteNode.tsx` | new — the node component |
| `client/src/pages/platform/workflow-editor/hooks/useStickyNotes.ts` | new — reconciler + add handler |
| `client/src/pages/platform/workflow-editor/hooks/useWorkflowEditorCanvas.ts` | register node type; drag-stop branch for notes |
| `client/src/pages/platform/workflow-editor/hooks/useLayout.tsx` | append note nodes post-layout; include in prune set |
| `client/src/pages/platform/workflow-editor/components/WorkflowEditorToolbar.tsx` | "Add note" button |

No server changes.

## Testing

- Unit tests for `stickyNoteUtils` (extract from definition, node building incl. cross-axis shift
  and read-only flags, updater semantics incl. key removal when empty, position compensation).
- Component test for `StickyNoteNode` (render content, edit commit on blur, read-only guards).
- Existing editor tests must stay green (`npm run check`).

## Out of scope

- Notes in the website's read-only workflow graph renderer (separate repo/renderer).
- Attaching notes to specific task nodes (anchored annotations).

## v2 — n8n feature parity (2026-07-04)

Reference: n8n "Add notes and documentation" docs. Gaps closed in v2:

- **Markdown rendering** (was plain text): display mode renders CommonMark + GFM via the
  existing `react-markdown` + `remark-gfm` dependencies, styled with `@tailwindcss/typography`
  (`prose prose-sm`). Raw HTML is not rendered (react-markdown default — safe against injection);
  links open in a new tab with `rel="noopener noreferrer"`. Edit mode stays a plain textarea over
  the raw markdown, exactly like n8n. Long content scrolls inside the note (`nowheel` class so
  ReactFlow doesn't pan instead).
- **Colors**: preset palette widened from 5 to 7 (n8n has 7): yellow, orange, green, blue,
  purple, pink, gray. New custom color option: a popover with a native color input plus a hex
  text field. Up to 8 recently used custom colors are kept in a persisted zustand store
  (localStorage), matching n8n's "8 recently used custom colors". The `color` field in the
  definition widens from a preset union to `string` — preset name or `#rrggbb`. Custom colors
  render via inline `backgroundColor`; the note computes text/prose inversion from relative
  luminance so dark custom colors stay readable.
- **YouTube embeds**: n8n's `@[youtube](video-id)` marker syntax is supported (added 2026-07-05).
  The marker argument may be a raw video id or a common YouTube URL (watch/youtu.be/embed/shorts);
  it renders as a `youtube-nocookie.com` iframe between markdown segments. The video id is
  validated against a strict charset before interpolation into the embed URL; unresolvable
  markers stay as plain text. n8n's `#full-width` image suffix remains unported (a proprietary
  markdown-it plugin; standard markdown images work).
- n8n adds notes through its nodes panel; ByteChef's panel is server-driven component
  definitions, so the toolbar "Add note" button remains the entry point (deliberate deviation).
