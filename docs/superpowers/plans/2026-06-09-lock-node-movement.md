# Lock Node Movement Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a lock toggle to the workflow editor and cluster element editor controls that disables manual node dragging by default (per load), until the user unlocks it.

**Architecture:** A session-only `nodesLocked` boolean lives in each editor's Zustand store (independent per canvas, default `true`). The canvas reads it into React Flow's `nodesDraggable` prop, and a toggle button in each editor's controls flips it. Each editor resets the flag to `true` on mount so every load starts locked.

**Tech Stack:** React 19, TypeScript, `@xyflow/react` (React Flow v12), Zustand, lucide-react, Vitest + @testing-library/react.

---

## File Structure

| File | Responsibility | Change |
| --- | --- | --- |
| `client/src/pages/platform/workflow-editor/stores/useWorkflowEditorStore.ts` | Workflow-editor UI flags | Add `nodesLocked` + `setNodesLocked` |
| `client/src/pages/platform/workflow-editor/components/WorkflowEditor.tsx` | Workflow canvas | Wire `nodesDraggable`, reset-on-mount |
| `client/src/pages/platform/workflow-editor/components/WorkflowEditorToolbar.tsx` | Workflow controls | Add lock toggle button |
| `client/src/pages/platform/cluster-element-editor/stores/useClusterElementsDataStore.ts` | Cluster-canvas state | Add `nodesLocked` + `setNodesLocked`, include in `reset()` |
| `client/src/pages/platform/cluster-element-editor/components/ClusterElementsWorkflowEditor.tsx` | Cluster canvas + controls | Wire `nodesDraggable`, reset-on-mount, add lock `ControlButton` |
| `*.test.ts(x)` for the two stores, the toolbar, and the cluster editor | Tests | Create |

All commands run from `client/`. Single-file test command: `npx vitest run <path>`.

---

## Task 1: Add `nodesLocked` to the workflow editor store

**Files:**
- Modify: `client/src/pages/platform/workflow-editor/stores/useWorkflowEditorStore.ts`
- Test: `client/src/pages/platform/workflow-editor/stores/useWorkflowEditorStore.test.ts` (create)

- [ ] **Step 1: Write the failing test**

Create `client/src/pages/platform/workflow-editor/stores/useWorkflowEditorStore.test.ts`:

```ts
import {beforeEach, describe, expect, it} from 'vitest';

import useWorkflowEditorStore from './useWorkflowEditorStore';

describe('useWorkflowEditorStore - nodesLocked', () => {
    beforeEach(() => {
        useWorkflowEditorStore.setState({nodesLocked: true});
    });

    it('defaults nodesLocked to true', () => {
        expect(useWorkflowEditorStore.getState().nodesLocked).toBe(true);
    });

    it('setNodesLocked updates the value', () => {
        useWorkflowEditorStore.getState().setNodesLocked(false);

        expect(useWorkflowEditorStore.getState().nodesLocked).toBe(false);

        useWorkflowEditorStore.getState().setNodesLocked(true);

        expect(useWorkflowEditorStore.getState().nodesLocked).toBe(true);
    });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd client && npx vitest run src/pages/platform/workflow-editor/stores/useWorkflowEditorStore.test.ts`
Expected: FAIL — `nodesLocked` is `undefined` / `setNodesLocked is not a function`.

- [ ] **Step 3: Add the state and setter to the interface**

In `useWorkflowEditorStore.ts`, inside `interface WorkflowEditorI`, add this block immediately after the `mainClusterRootComponentDefinition` setter (after line 21, before `nestedClusterRootsComponentDefinitions`):

```ts
    nodesLocked: boolean;
    setNodesLocked: (nodesLocked: boolean) => void;
```

- [ ] **Step 4: Add the implementation**

In the store creator object, add this block immediately after the `mainClusterRootComponentDefinition` implementation (after line 90, before `nestedClusterRootsComponentDefinitions`):

```ts
            nodesLocked: true,
            setNodesLocked: (nodesLocked) =>
                set(() => ({
                    nodesLocked,
                })),
```

(The file has `/* eslint-disable sort-keys */` at the top, so key order is not enforced.)

- [ ] **Step 5: Run test to verify it passes**

Run: `cd client && npx vitest run src/pages/platform/workflow-editor/stores/useWorkflowEditorStore.test.ts`
Expected: PASS (2 passing).

- [ ] **Step 6: Commit**

```bash
git add client/src/pages/platform/workflow-editor/stores/useWorkflowEditorStore.ts client/src/pages/platform/workflow-editor/stores/useWorkflowEditorStore.test.ts
git commit -m "client - Add nodesLocked flag to workflow editor store"
```

---

## Task 2: Wire `nodesDraggable` and reset-on-mount in WorkflowEditor

**Files:**
- Modify: `client/src/pages/platform/workflow-editor/components/WorkflowEditor.tsx`

No new test here — this is wiring covered indirectly by the store test and the toolbar test (Task 3), plus typecheck. Behavior is verified manually at the end.

- [ ] **Step 1: Add imports**

At the top of `WorkflowEditor.tsx`, update the React import and add the store import. Change line 10 area so the imports include `useEffect` and `useWorkflowEditorStore`. Add after the existing `useWorkflowEditorCanvas` import (line 12):

```ts
import {useEffect} from 'react';

import useWorkflowEditorStore from '../stores/useWorkflowEditorStore';
```

- [ ] **Step 2: Read the flag from the store**

Immediately after the `useWorkflowDataStore` selector block (after line 45), add:

```ts
    const {nodesLocked, setNodesLocked} = useWorkflowEditorStore(
        useShallow((state) => ({
            nodesLocked: state.nodesLocked,
            setNodesLocked: state.setNodesLocked,
        }))
    );
```

- [ ] **Step 3: Reset to locked on mount**

Immediately before the `return (` statement (before line 56), add:

```ts
    useEffect(() => {
        setNodesLocked(true);
    }, [setNodesLocked]);
```

- [ ] **Step 4: Compose the `nodesDraggable` condition**

Change line 67 from:

```tsx
                nodesDraggable={!readOnlyWorkflow}
```

to:

```tsx
                nodesDraggable={!readOnlyWorkflow && !nodesLocked}
```

- [ ] **Step 5: Typecheck**

Run: `cd client && npm run typecheck`
Expected: PASS (no type errors in `WorkflowEditor.tsx`).

- [ ] **Step 6: Commit**

```bash
git add client/src/pages/platform/workflow-editor/components/WorkflowEditor.tsx
git commit -m "client - Disable workflow node dragging when locked, reset locked on mount"
```

---

## Task 3: Add the lock toggle button to WorkflowEditorToolbar

**Files:**
- Modify: `client/src/pages/platform/workflow-editor/components/WorkflowEditorToolbar.tsx`
- Test: `client/src/pages/platform/workflow-editor/components/WorkflowEditorToolbar.test.tsx` (create)

- [ ] **Step 1: Write the failing test**

Create `client/src/pages/platform/workflow-editor/components/WorkflowEditorToolbar.test.tsx`:

```tsx
import {ReactFlowProvider} from '@xyflow/react';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {beforeEach, describe, expect, it} from 'vitest';

import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import useWorkflowEditorStore from '../stores/useWorkflowEditorStore';
import WorkflowEditorToolbar from './WorkflowEditorToolbar';

const renderToolbar = (readOnly = false) =>
    render(
        <ReactFlowProvider>
            <WorkflowEditorToolbar readOnly={readOnly} />
        </ReactFlowProvider>
    );

describe('WorkflowEditorToolbar - lock button', () => {
    beforeEach(() => {
        useWorkflowDataStore.setState({edges: [], nodes: []});
        useWorkflowEditorStore.setState({nodesLocked: true});
    });

    it('renders the unlock affordance when locked and not read-only', () => {
        renderToolbar(false);

        expect(screen.getByLabelText('Unlock node movement')).toBeInTheDocument();
    });

    it('hides the lock button in read-only mode', () => {
        renderToolbar(true);

        expect(screen.queryByLabelText('Unlock node movement')).not.toBeInTheDocument();
        expect(screen.queryByLabelText('Lock node movement')).not.toBeInTheDocument();
    });

    it('toggles nodesLocked and the label when clicked', async () => {
        const user = userEvent.setup();

        renderToolbar(false);

        await user.click(screen.getByLabelText('Unlock node movement'));

        expect(useWorkflowEditorStore.getState().nodesLocked).toBe(false);
        expect(screen.getByLabelText('Lock node movement')).toBeInTheDocument();
    });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd client && npx vitest run src/pages/platform/workflow-editor/components/WorkflowEditorToolbar.test.tsx`
Expected: FAIL — no element with label `Unlock node movement`.

- [ ] **Step 3: Add icon imports**

Change line 6 of `WorkflowEditorToolbar.tsx` from:

```ts
import {ArrowRightIcon, BrushCleaningIcon, FocusIcon, InfoIcon, ZoomInIcon, ZoomOutIcon} from 'lucide-react';
```

to (alphabetical order per `bytechef/sort-import-destructures`):

```ts
import {
    ArrowRightIcon,
    BrushCleaningIcon,
    FocusIcon,
    InfoIcon,
    LockIcon,
    LockOpenIcon,
    ZoomInIcon,
    ZoomOutIcon,
} from 'lucide-react';
```

- [ ] **Step 4: Read the lock state and add a toggle handler**

Replace the existing `setResetWorkflowLayout` selector (line 27) with a combined selector using `useShallow`:

```ts
    const {nodesLocked, setNodesLocked, setResetWorkflowLayout} = useWorkflowEditorStore(
        useShallow((state) => ({
            nodesLocked: state.nodesLocked,
            setNodesLocked: state.setNodesLocked,
            setResetWorkflowLayout: state.setResetWorkflowLayout,
        }))
    );
```

Then add the handler immediately after `handleClear` (after line 49):

```ts
    const handleToggleLock = useCallback(() => {
        setNodesLocked(!nodesLocked);
    }, [nodesLocked, setNodesLocked]);
```

- [ ] **Step 5: Render the button (hidden in read-only)**

Inside the `<ButtonGroup>`, immediately after the closing `</Tooltip>` of the Reset-layout button (after line 168, before `</ButtonGroup>` on line 169), insert:

```tsx
                    {!readOnly && (
                        <Tooltip>
                            <TooltipTrigger asChild>
                                <button
                                    aria-label={nodesLocked ? 'Unlock node movement' : 'Lock node movement'}
                                    className="flex size-9 items-center justify-center rounded-md border border-stroke-neutral-secondary bg-surface-neutral-primary hover:bg-slate-50 active:bg-surface-neutral-secondary"
                                    onClick={handleToggleLock}
                                >
                                    {nodesLocked ? (
                                        <LockIcon className="size-4 text-content-neutral-primary" />
                                    ) : (
                                        <LockOpenIcon className="size-4 text-content-neutral-primary" />
                                    )}
                                </button>
                            </TooltipTrigger>

                            <TooltipContent
                                className="rounded-lg bg-surface-tooltip text-content-onsurface-primary"
                                side="top"
                            >
                                {nodesLocked ? 'Unlock node movement' : 'Lock node movement'}
                            </TooltipContent>
                        </Tooltip>
                    )}
```

- [ ] **Step 6: Run test to verify it passes**

Run: `cd client && npx vitest run src/pages/platform/workflow-editor/components/WorkflowEditorToolbar.test.tsx`
Expected: PASS (3 passing).

- [ ] **Step 7: Commit**

```bash
git add client/src/pages/platform/workflow-editor/components/WorkflowEditorToolbar.tsx client/src/pages/platform/workflow-editor/components/WorkflowEditorToolbar.test.tsx
git commit -m "client - Add lock node movement toggle to workflow editor toolbar"
```

---

## Task 4: Add `nodesLocked` to the cluster elements data store

**Files:**
- Modify: `client/src/pages/platform/cluster-element-editor/stores/useClusterElementsDataStore.ts`
- Test: `client/src/pages/platform/cluster-element-editor/stores/useClusterElementsDataStore.test.ts` (create)

- [ ] **Step 1: Write the failing test**

Create `client/src/pages/platform/cluster-element-editor/stores/useClusterElementsDataStore.test.ts`:

```ts
import {beforeEach, describe, expect, it} from 'vitest';

import useClusterElementsDataStore from './useClusterElementsDataStore';

describe('useClusterElementsDataStore - nodesLocked', () => {
    beforeEach(() => {
        useClusterElementsDataStore.setState({nodesLocked: true});
    });

    it('defaults nodesLocked to true', () => {
        expect(useClusterElementsDataStore.getState().nodesLocked).toBe(true);
    });

    it('setNodesLocked updates the value', () => {
        useClusterElementsDataStore.getState().setNodesLocked(false);

        expect(useClusterElementsDataStore.getState().nodesLocked).toBe(false);
    });

    it('reset() returns nodesLocked to true', () => {
        useClusterElementsDataStore.getState().setNodesLocked(false);
        useClusterElementsDataStore.getState().reset();

        expect(useClusterElementsDataStore.getState().nodesLocked).toBe(true);
    });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd client && npx vitest run src/pages/platform/cluster-element-editor/stores/useClusterElementsDataStore.test.ts`
Expected: FAIL — `nodesLocked` undefined / `setNodesLocked is not a function`.

- [ ] **Step 3: Add to the interface**

In `ClusterElementsDataStoreI`, add after the `canvasZoom` setter (after line 27, before `reset`):

```ts
    nodesLocked: boolean;
    setNodesLocked: (nodesLocked: boolean) => void;
```

- [ ] **Step 4: Add the implementation and include it in `reset()`**

In the store creator, add after the `canvasZoom` block (after line 73, before `reset`):

```ts
            nodesLocked: true,
            setNodesLocked: (nodesLocked) => {
                set({nodesLocked});
            },
```

Then in the existing `reset()` body (lines 76-83), add `nodesLocked: true,` to the object so it reads:

```ts
            reset: () => {
                set({
                    canvasZoom: DEFAULT_CLUSTER_ELEMENT_CANVAS_ZOOM,
                    draggingNodeId: null,
                    edges: [],
                    isNodeDragging: false,
                    isPositionSaving: false,
                    nodes: [],
                    nodesLocked: true,
                });
            },
```

- [ ] **Step 5: Run test to verify it passes**

Run: `cd client && npx vitest run src/pages/platform/cluster-element-editor/stores/useClusterElementsDataStore.test.ts`
Expected: PASS (3 passing).

- [ ] **Step 6: Commit**

```bash
git add client/src/pages/platform/cluster-element-editor/stores/useClusterElementsDataStore.ts client/src/pages/platform/cluster-element-editor/stores/useClusterElementsDataStore.test.ts
git commit -m "client - Add nodesLocked flag to cluster elements data store"
```

---

## Task 5: Wire lock into the cluster element editor canvas and controls

**Files:**
- Modify: `client/src/pages/platform/cluster-element-editor/components/ClusterElementsWorkflowEditor.tsx`
- Test: `client/src/pages/platform/cluster-element-editor/components/ClusterElementsWorkflowEditor.test.tsx` (create)

- [ ] **Step 1: Write the failing test**

Create `client/src/pages/platform/cluster-element-editor/components/ClusterElementsWorkflowEditor.test.tsx`. It mocks the editor hook (which has heavy dependencies) so the test focuses on the lock control:

```tsx
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useClusterElementsDataStore from '../stores/useClusterElementsDataStore';
import ClusterElementsWorkflowEditor from './ClusterElementsWorkflowEditor';

vi.mock('../hooks/useClusterElementsWorkflowEditor', () => ({
    default: () => ({
        clusterElementsEdgeTypes: {},
        clusterElementsNodeTypes: {},
        edges: [],
        handleNodesChange: vi.fn(),
        handleResetLayout: vi.fn(),
        nodes: [],
    }),
}));

describe('ClusterElementsWorkflowEditor - lock button', () => {
    beforeEach(() => {
        useClusterElementsDataStore.setState({edges: [], nodes: [], nodesLocked: true});
    });

    it('renders the unlock affordance when locked', () => {
        render(<ClusterElementsWorkflowEditor />);

        expect(screen.getByTitle('Unlock node movement')).toBeInTheDocument();
    });

    it('toggles nodesLocked when clicked', async () => {
        const user = userEvent.setup();

        render(<ClusterElementsWorkflowEditor />);

        await user.click(screen.getByTitle('Unlock node movement'));

        expect(useClusterElementsDataStore.getState().nodesLocked).toBe(false);
        expect(screen.getByTitle('Lock node movement')).toBeInTheDocument();
    });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd client && npx vitest run src/pages/platform/cluster-element-editor/components/ClusterElementsWorkflowEditor.test.tsx`
Expected: FAIL — no element with title `Unlock node movement`.

- [ ] **Step 3: Update imports**

Change the lucide import (line 13) from:

```ts
import {BrushCleaningIcon} from 'lucide-react';
```

to:

```ts
import {BrushCleaningIcon, LockIcon, LockOpenIcon} from 'lucide-react';
```

Change the React import (line 14) from:

```ts
import {useCallback} from 'react';
```

to:

```ts
import {useCallback, useEffect} from 'react';
```

- [ ] **Step 4: Read the lock state from the store**

Update the `useClusterElementsDataStore` selector block (lines 21-26) to include the lock state and setter:

```ts
    const {nodesLocked, onEdgesChange, setCanvasZoom, setNodesLocked} = useClusterElementsDataStore(
        useShallow((state) => ({
            nodesLocked: state.nodesLocked,
            onEdgesChange: state.onEdgesChange,
            setCanvasZoom: state.setCanvasZoom,
            setNodesLocked: state.setNodesLocked,
        }))
    );
```

- [ ] **Step 5: Add the toggle handler and reset-on-mount effect**

Immediately after the `handleViewportChange` callback (after line 36), add:

```ts
    const handleToggleLock = useCallback(() => {
        setNodesLocked(!nodesLocked);
    }, [nodesLocked, setNodesLocked]);

    useEffect(() => {
        setNodesLocked(true);
    }, [setNodesLocked]);
```

- [ ] **Step 6: Compose `nodesDraggable`**

Change line 50 from:

```tsx
                    nodesDraggable
```

to:

```tsx
                    nodesDraggable={!nodesLocked}
```

- [ ] **Step 7: Add the lock ControlButton**

Inside `<Controls>`, immediately after the existing Reset-layout `<ControlButton>` (after line 68, before `</Controls>` on line 69), insert:

```tsx
                        <ControlButton
                            onClick={handleToggleLock}
                            title={nodesLocked ? 'Unlock node movement' : 'Lock node movement'}
                        >
                            {nodesLocked ? (
                                <LockIcon className="size-3" />
                            ) : (
                                <LockOpenIcon className="size-3" />
                            )}
                        </ControlButton>
```

- [ ] **Step 8: Run test to verify it passes**

Run: `cd client && npx vitest run src/pages/platform/cluster-element-editor/components/ClusterElementsWorkflowEditor.test.tsx`
Expected: PASS (2 passing).

- [ ] **Step 9: Commit**

```bash
git add client/src/pages/platform/cluster-element-editor/components/ClusterElementsWorkflowEditor.tsx client/src/pages/platform/cluster-element-editor/components/ClusterElementsWorkflowEditor.test.tsx
git commit -m "client - Add lock node movement toggle to cluster element editor"
```

---

## Task 6: Full check and manual verification

**Files:** none (verification only).

- [ ] **Step 1: Run the full client check**

Run: `cd client && npm run check`
Expected: PASS (lint + typecheck + all tests green). Fix any lint issues — particularly `sort-keys` (manual fix required), import-destructure ordering, and interface naming (`...I`/`...Props`).

- [ ] **Step 2: Manual smoke test (dev server)**

Run the client (`npm run dev`) and the server, then:
- Open a workflow in the editor. Confirm nodes **cannot** be dragged on load and the toolbar shows a closed-lock icon with tooltip "Unlock node movement".
- Click the lock button. Confirm the icon switches to open-lock ("Lock node movement") and nodes **can** be dragged.
- Reload the page. Confirm it starts **locked** again.
- Open a read-only workflow (e.g. via the read-only workflow sheet). Confirm the lock button is **not** shown and nodes are not draggable.
- Open the cluster element canvas. Confirm the same locked-by-default behavior and that the lock `ControlButton` toggles dragging. Confirm it resets to locked when the cluster canvas is reopened.

- [ ] **Step 3: Final commit (if Step 1 required fixes)**

```bash
git add -A
git commit -m "client - Lint/format fixes for lock node movement"
```

---

## Self-Review Notes

- **Spec coverage:** Default-locked (Tasks 1/4 default `true` + reset effects in Tasks 2/5); session-only reset (mount effects + store `reset()`); independent locks (two separate stores); dragging-only (`nodesDraggable` is the only prop touched); store-flag state location; read-only composition (`!readOnlyWorkflow && !nodesLocked` + button hidden when `readOnly`). All covered.
- **Naming consistency:** `nodesLocked` / `setNodesLocked` used identically across both stores and all consumers.
- **Read-only behavior:** Per design, the workflow toolbar lock button is hidden when `readOnly` (Task 3, Step 5). The cluster editor has no read-only mode, so no guard is needed there.
- **Reset trigger:** Both editors reset on mount. If either editor is later changed to stay mounted across workflow/canvas switches, key the reset `useEffect` on the workflow/canvas id instead of `[]`-equivalent deps.
