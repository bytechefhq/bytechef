# Lock node movement in the workflow & cluster element editors

Date: 2026-06-09

## Summary

Add a **lock toggle** to the controls of both the workflow editor and the cluster
element editor. When locked, manual dragging of nodes is disabled while every other
interaction (pan, zoom, fit-view, node selection, opening node details, all toolbar
actions) stays enabled. Both canvases start **locked by default on every load** to
prevent accidental node movement; the user can unlock to reposition nodes manually.

## Goals

- Prevent accidental node movement, which is the common failure mode now that node
  layout is auto-managed.
- Provide an explicit, discoverable unlock affordance in each editor's existing
  controls.

## Non-goals (YAGNI)

- Persisting lock state across reloads (session-only).
- Per-workflow / backend-persisted lock state.
- Blocking structural edits (add / delete / connect nodes) while locked — lock only
  affects position dragging.
- Keyboard shortcut for toggling the lock.

## Decisions

| Decision            | Choice                                                        |
| ------------------- | ------------------------------------------------------------- |
| Default state       | Locked on every editor load                                   |
| Persistence         | Session-only; resets to locked on mount / canvas open         |
| Scope               | Independent lock per canvas (workflow vs. cluster element)    |
| Lock behavior       | Disable node dragging only; all other interactions unaffected |
| State location      | Zustand store flags (matches existing editor UI-flag pattern) |
| Read-only composition | In read-only mode the lock button is hidden (dragging already off) |

## Design

### 1. Behavior

- Both canvases initialise with `nodesLocked = true`.
- Locked means `nodesDraggable = false` on the React Flow canvas. Nothing else changes.
- A lock/unlock toggle button is added to each canvas's controls. Clicking it flips the
  flag. State is session-only and independent per canvas; reloading or reopening the
  editor returns to locked.

### 2. Workflow editor

**Store — `client/src/pages/platform/workflow-editor/stores/useWorkflowEditorStore.ts`**

- Add `nodesLocked: boolean` (default `true`).
- Add `setNodesLocked: (nodesLocked: boolean) => void` following the existing setter
  pattern.

**Canvas — `client/src/pages/platform/workflow-editor/components/WorkflowEditor.tsx`**

- Read `nodesLocked` from the store.
- Change line 67 to: `nodesDraggable={!readOnlyWorkflow && !nodesLocked}` — composes
  with the existing read-only logic.
- Add a `useEffect` that calls `setNodesLocked(true)` on mount (and when the active
  workflow id changes) so navigating between workflows always starts locked.

**Toolbar — `client/src/pages/platform/workflow-editor/components/WorkflowEditorToolbar.tsx`**

- Add a lock toggle button inside the existing `<ButtonGroup>`, using the established
  Tooltip + icon-button pattern (template: the Zoom In button, lines 76-92).
- Icon: `LockIcon` when locked, `LockOpenIcon` when unlocked (from `lucide-react`).
- Tooltip text: "Unlock node movement" when locked, "Lock node movement" when unlocked.
- Hide the button entirely when `readOnly` is true (dragging is already disabled, so the
  toggle would be inert and confusing).

### 3. Cluster element editor

**Store — `client/src/pages/platform/cluster-element-editor/stores/useClusterElementsDataStore.ts`**

- Add `nodesLocked: boolean` (default `true`) and `setNodesLocked` following the existing
  setter pattern.

**Canvas — `client/src/pages/platform/cluster-element-editor/components/ClusterElementsWorkflowEditor.tsx`**

- Read `nodesLocked` from the store.
- Change line 50 to: `nodesDraggable={!nodesLocked}`.
- Reset to locked (`setNodesLocked(true)`) when the cluster element canvas opens.

**Controls**

- Add a lock toggle as a `<ControlButton>` next to the existing reset button inside the
  React Flow `<Controls>` component (matching that pattern and the `size-3` icon style).
- `title` attribute: "Unlock node movement" / "Lock node movement".
- Icon: `LockIcon` / `LockOpenIcon` at `size-3`.

### 4. Component / unit boundaries

- The lock state is a single boolean per store; the toggle button and the canvas both
  read it through the store, so the button and the `nodesDraggable` prop never drift.
- The two locks share no state, satisfying the independence requirement.

## Testing

- **Store unit tests** (both stores): default is `true`; `setNodesLocked` toggles the
  value; reset-on-mount / reset-on-open returns the value to `true`.
- **Component tests:**
  - Workflow toolbar: lock button renders when not read-only and is hidden when
    read-only; clicking it calls `setNodesLocked` and swaps the icon/tooltip.
  - Cluster controls: lock `ControlButton` renders; clicking toggles state and icon.
  - Canvas: `nodesDraggable` reflects the composed condition (`!readOnlyWorkflow &&
    !nodesLocked` for the workflow editor; `!nodesLocked` for the cluster editor).
- Run `npm run check` (lint + typecheck + tests) in `client/` before committing.

## Files touched

- `client/src/pages/platform/workflow-editor/stores/useWorkflowEditorStore.ts`
- `client/src/pages/platform/workflow-editor/components/WorkflowEditor.tsx`
- `client/src/pages/platform/workflow-editor/components/WorkflowEditorToolbar.tsx`
- `client/src/pages/platform/cluster-element-editor/stores/useClusterElementsDataStore.ts`
- `client/src/pages/platform/cluster-element-editor/components/ClusterElementsWorkflowEditor.tsx`
- Corresponding test files.
