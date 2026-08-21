# Multiple Triggers in the Workflow Editor — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let the workflow editor render, add (via drop *and* click), configure, position, and delete an arbitrary number of triggers, all fanning into the first task.

**Architecture:** The workflow definition already stores `triggers` as an array. The change is entirely client-side in `client/src/pages/platform/workflow-editor`. We make trigger-ness explicit (not positional), render N trigger nodes plus a `triggerPlaceholder` ("+") slot, fan all triggers into the first task with explicit edges, and convert the trigger save path from "replace the array" to "upsert by name." Position/empty-state utilities switch from `triggers[0]` to find-by-name across all triggers.

**Tech Stack:** React 19 + TypeScript, `@xyflow/react` (React Flow), `@dagrejs/dagre` for layout, Zustand stores, Vitest 4 for tests.

## Global Constraints

- All client code: object keys in natural ascending (alphabetical) order (ESLint `sort-keys`, NOT auto-fixed).
- Named imports sorted alphabetically within `{}` (`bytechef/sort-import-destructures`); `type` keyword imports sort by name.
- Interface names end in `I` or `Props`; `useRef` vars end in `Ref`.
- Lucide icons imported with `Icon` suffix (`PlusIcon`, not `Plus`).
- Use `twMerge` for conditional classes (never `cn()`).
- No short/cryptic variable names; no `_`-prefixed private names.
- Run `cd client && npm run check` (lint + typecheck + tests) before every client commit; run `npm run format` first.
- Commit message convention (client): `2898 client - <description>` — replace `2898` with the actual ticket number if known; otherwise use a short descriptive prefix consistent with the branch.
- Do NOT stage unrelated pre-existing changes; stage only files touched by the task.

## Key existing references (read before starting)

- `hooks/useLayout.tsx:202-217` — single-trigger node build; `:411-617` — consecutive-pair edge loop; `:768-769,793-806,816` — effect consuming `allNodes`/`taskEdges`.
- `utils/layoutUtils.tsx:173-203` — `convertTaskToNode` (positional `trigger: index === 0`); `:590-644` — `getLayoutElements` dagre entry; `:685-704` — post-dagre constraint pipeline; `:1417-1440` — `createDefaultNodes`.
- `utils/saveWorkflowDefinition.ts:72-98` — trigger branch (`{triggers: [newTrigger]}`); `:285-365` — `executeWorkflowMutation`.
- `hooks/useHandleDrop.tsx:32-46` — `createWorkflowNodeData` (hardcodes `trigger_1`); `:241-255` — `handleDropOnTriggerNode`.
- `hooks/useWorkflowEditorCanvas.ts:118-130` — `nodeTypes`; `:152-288` — `onDrop`; `:108-110` — `useHandleDrop` destructure.
- `components/WorkflowNodesPopoverMenu.tsx` — picker; `hide*` props already allow trigger-only listing.
- `components/WorkflowNodesPopoverMenuOperationList.tsx:162-193` — `getNodeData` (hardcodes `trigger_1`); `:335-358` — trigger save path → `saveNodeToWorkflow(data, 0)`.
- `nodes/PlaceholderNode.tsx` — mirror for the new node component (it wraps its "+" with `WorkflowNodesPopoverMenu hideTriggerComponents`; the trigger slot does the inverse).
- `utils/extractDefinitionPositions.ts:37-39`, `utils/saveWorkflowNodesPosition.ts:200-215`, `utils/removeWorkflowNodePosition.ts:160-171`, `utils/clearAllNodePositions.ts:150-156` — all index `triggers[0]`.
- `stores/useWorkflowDataStore.ts:348-351` — duplicate-name seed uses `triggers?.[0]`.
- `utils/getFormattedName.ts` — `getFormattedName('trigger')` returns `trigger_1` when none exist, else `trigger_<max+1>`.
- Existing util tests live in `utils/tests/*.test.ts` (e.g. `calculateNodeInsertIndex.test.ts`) — Vitest, plain `describe/it/expect`.

---

### Task 1: Make trigger-ness explicit in `convertTaskToNode`

**Files:**
- Modify: `client/src/pages/platform/workflow-editor/utils/layoutUtils.tsx:173-203`
- Modify (callers): `client/src/pages/platform/workflow-editor/hooks/useLayout.tsx:211,258`
- Test: `client/src/pages/platform/workflow-editor/utils/tests/convertTaskToNode.test.tsx`

**Interfaces:**
- Produces: `convertTaskToNode(task: WorkflowTask, taskDefinition: ComponentDefinitionBasic | TaskDispatcherDefinitionBasic, isTrigger: boolean): Node` — third param is now an explicit boolean; `data.trigger` mirrors it.
- Consumed by: Task 2 (`buildTriggerNodes` passes `true`) and `useLayout` task loop (passes `false`).

- [ ] **Step 1: Write the failing test**

Create `client/src/pages/platform/workflow-editor/utils/tests/convertTaskToNode.test.tsx`:

```tsx
import {ComponentDefinitionBasic, WorkflowTask} from '@/shared/middleware/platform/configuration';
import {describe, expect, it} from 'vitest';

import {convertTaskToNode} from '../layoutUtils';

const definition = {icon: '<svg/>', name: 'webhook', title: 'Webhook'} as ComponentDefinitionBasic;

describe('convertTaskToNode', () => {
    it('marks the node as a trigger when isTrigger is true', () => {
        const task = {name: 'trigger_2', type: 'webhook/v1/onReceive'} as WorkflowTask;

        const node = convertTaskToNode(task, definition, true);

        expect(node.data.trigger).toBe(true);
        expect(node.id).toBe('trigger_2');
    });

    it('marks the node as a non-trigger when isTrigger is false', () => {
        const task = {name: 'action_1', type: 'logger/v1/info'} as WorkflowTask;

        const node = convertTaskToNode(task, definition, false);

        expect(node.data.trigger).toBe(false);
    });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd client && npx vitest run src/pages/platform/workflow-editor/utils/tests/convertTaskToNode.test.tsx`
Expected: FAIL — second test gets `trigger: true` because the current signature uses `index` and any non-zero passes `index === 0` as false, but the call passes a boolean `true`/`false` that does not equal `0`, so `data.trigger` is currently `false` for both — first test FAILS.

- [ ] **Step 3: Change the signature**

In `layoutUtils.tsx`, edit `convertTaskToNode`:

```tsx
export const convertTaskToNode = (
    task: WorkflowTask,
    taskDefinition: ComponentDefinitionBasic | TaskDispatcherDefinitionBasic,
    isTrigger: boolean
): Node => {
    const componentName = task.type.split('/')[0];

    const isTaskDispatcher = TASK_DISPATCHER_NAMES.includes(componentName);

    return {
        data: {
            ...task,
            componentName,
            icon: (
                <InlineSVG
                    className="size-9"
                    loader={<ComponentIcon className="size-9 flex-none text-gray-900" />}
                    src={taskDefinition.icon!}
                />
            ),
            operationName: task.type.split('/')[2],
            taskDispatcher: isTaskDispatcher,
            taskDispatcherId: isTaskDispatcher ? task.name : undefined,
            trigger: isTrigger,
            workflowNodeName: task.name,
        },
        id: task.name,
        position: {x: 0, y: 0},
        type: task.clusterRoot ? 'clusterRoot' : 'workflow',
    };
};
```

- [ ] **Step 4: Update the two callers in `useLayout.tsx`**

Line 211 (inside the soon-to-be-replaced `triggerNode` memo — leave the call shape correct for now):
```tsx
return convertTaskToNode(triggers[0], triggerDefinition, true);
```
Line 258 (task loop):
```tsx
taskNode = convertTaskToNode(task, taskDefinition, false);
```

- [ ] **Step 5: Run test to verify it passes**

Run: `cd client && npx vitest run src/pages/platform/workflow-editor/utils/tests/convertTaskToNode.test.tsx`
Expected: PASS (both tests).

- [ ] **Step 6: Commit**

```bash
cd client && npm run format
git add src/pages/platform/workflow-editor/utils/layoutUtils.tsx \
        src/pages/platform/workflow-editor/hooks/useLayout.tsx \
        src/pages/platform/workflow-editor/utils/tests/convertTaskToNode.test.tsx
git commit -m "<ticket> client - Make trigger-ness explicit in convertTaskToNode"
```

---

### Task 2: `buildTriggerNodes` helper + `TRIGGER_PLACEHOLDER_NODE_ID`

**Files:**
- Modify: `client/src/shared/constants.tsx` (add constant near `FINAL_PLACEHOLDER_NODE_ID` at `:86`)
- Modify: `client/src/pages/platform/workflow-editor/utils/layoutUtils.tsx` (add `buildTriggerNodes` near `convertTaskToNode`)
- Test: `client/src/pages/platform/workflow-editor/utils/tests/buildTriggerNodes.test.tsx`

**Interfaces:**
- Produces:
  - `TRIGGER_PLACEHOLDER_NODE_ID` (string) in `@/shared/constants`.
  - `buildTriggerNodes(triggers: WorkflowTrigger[] | undefined, componentDefinitions: ComponentDefinitionBasic[], canvasWidth: number): {placeholderNode: Node; triggerNodes: Node[]}` — `triggerNodes` are the real trigger nodes (Manual fallback when `triggers` is empty); `placeholderNode` is the "+" slot.
- Consumes: `convertTaskToNode(..., true)` (Task 1), `createDefaultNodes` (existing).
- Consumed by: `useLayout` (Task 4), post-dagre placement (Task 6 keys off `TRIGGER_PLACEHOLDER_NODE_ID`), `TriggerPlaceholderNode` (Task 5 reads the id).

- [ ] **Step 1: Add the constant**

In `client/src/shared/constants.tsx`, immediately after the `FINAL_PLACEHOLDER_NODE_ID` line (`:86`):

```tsx
export const TRIGGER_PLACEHOLDER_NODE_ID = 'trigger-placeholder';
```

(Use a stable literal, NOT `getRandomId()` — Task 5/6/9 reference this id by value to detect the slot.)

- [ ] **Step 2: Write the failing test**

Create `client/src/pages/platform/workflow-editor/utils/tests/buildTriggerNodes.test.tsx`:

```tsx
import {ComponentDefinitionBasic, WorkflowTrigger} from '@/shared/middleware/platform/configuration';
import {TRIGGER_PLACEHOLDER_NODE_ID} from '@/shared/constants';
import {describe, expect, it} from 'vitest';

import {buildTriggerNodes} from '../layoutUtils';

const definitions = [
    {icon: '<svg/>', name: 'webhook', title: 'Webhook'},
    {icon: '<svg/>', name: 'schedule', title: 'Schedule'},
] as ComponentDefinitionBasic[];

describe('buildTriggerNodes', () => {
    it('builds one node per trigger plus a placeholder', () => {
        const triggers = [
            {name: 'trigger_1', type: 'webhook/v1/onReceive'},
            {name: 'trigger_2', type: 'schedule/v1/onInterval'},
        ] as WorkflowTrigger[];

        const {placeholderNode, triggerNodes} = buildTriggerNodes(triggers, definitions, 1200);

        expect(triggerNodes.map((node) => node.id)).toEqual(['trigger_1', 'trigger_2']);
        expect(triggerNodes.every((node) => node.data.trigger === true)).toBe(true);
        expect(placeholderNode.id).toBe(TRIGGER_PLACEHOLDER_NODE_ID);
        expect(placeholderNode.type).toBe('triggerPlaceholder');
    });

    it('falls back to the Manual placeholder node when there are no triggers', () => {
        const {triggerNodes} = buildTriggerNodes([], definitions, 1200);

        expect(triggerNodes).toHaveLength(1);
        expect(triggerNodes[0].id).toBe('trigger_1');
        expect(triggerNodes[0].data.componentName).toBe('manual');
    });

    it('uses a fallback node when a trigger component definition is missing', () => {
        const triggers = [{name: 'trigger_1', type: 'unknown/v1/onThing'}] as WorkflowTrigger[];

        const {triggerNodes} = buildTriggerNodes(triggers, definitions, 1200);

        expect(triggerNodes).toHaveLength(1);
        expect(triggerNodes[0].data.trigger).toBe(true);
        expect(triggerNodes[0].data.componentName).toBe('unknown');
    });
});
```

- [ ] **Step 3: Run test to verify it fails**

Run: `cd client && npx vitest run src/pages/platform/workflow-editor/utils/tests/buildTriggerNodes.test.tsx`
Expected: FAIL with "buildTriggerNodes is not a function".

- [ ] **Step 4: Implement `buildTriggerNodes`**

In `layoutUtils.tsx`, add the import for the constant (keep import group alphabetical) and the helper right after `convertTaskToNode`:

```tsx
export const buildTriggerNodes = (
    triggers: WorkflowTrigger[] | undefined,
    componentDefinitions: ComponentDefinitionBasic[],
    canvasWidth: number
): {placeholderNode: Node; triggerNodes: Node[]} => {
    const placeholderNode: Node = {
        data: {label: '+'},
        id: TRIGGER_PLACEHOLDER_NODE_ID,
        position: {x: 0, y: 0},
        type: 'triggerPlaceholder',
    };

    if (!triggers || triggers.length === 0) {
        return {placeholderNode, triggerNodes: [createDefaultNodes(canvasWidth)[0]]};
    }

    const triggerNodes = triggers.map((trigger) => {
        const componentName = trigger.type.split('/')[0];

        const triggerDefinition = componentDefinitions.find((definition) => definition.name === componentName);

        if (triggerDefinition) {
            return convertTaskToNode(trigger, triggerDefinition, true);
        }

        return {
            data: {
                ...trigger,
                componentName,
                icon: <ComponentIcon className="size-9 flex-none text-gray-900" />,
                operationName: trigger.type.split('/')[2],
                trigger: true,
                workflowNodeName: trigger.name,
            },
            id: trigger.name,
            position: {x: 0, y: 0},
            type: 'workflow',
        } as Node;
    });

    return {placeholderNode, triggerNodes};
};
```

Add `WorkflowTrigger` to the existing `@/shared/middleware/platform/configuration` import and `TRIGGER_PLACEHOLDER_NODE_ID` to the `@/shared/constants` import in `layoutUtils.tsx` (both alphabetised). `ComponentIcon` is already imported in this file.

- [ ] **Step 5: Run test to verify it passes**

Run: `cd client && npx vitest run src/pages/platform/workflow-editor/utils/tests/buildTriggerNodes.test.tsx`
Expected: PASS (3 tests).

- [ ] **Step 6: Commit**

```bash
cd client && npm run format
git add src/shared/constants.tsx \
        src/pages/platform/workflow-editor/utils/layoutUtils.tsx \
        src/pages/platform/workflow-editor/utils/tests/buildTriggerNodes.test.tsx
git commit -m "<ticket> client - Add buildTriggerNodes helper and trigger placeholder id"
```

---

### Task 3: `buildTriggerFanInEdges` helper

**Files:**
- Modify: `client/src/pages/platform/workflow-editor/utils/layoutUtils.tsx`
- Test: `client/src/pages/platform/workflow-editor/utils/tests/buildTriggerFanInEdges.test.ts`

**Interfaces:**
- Produces: `buildTriggerFanInEdges(triggerNodes: Node[], targetNodeId: string): Edge[]` — one edge per trigger node into `targetNodeId`; never emits an edge for the placeholder (callers pass only real trigger nodes).
- Consumed by: `useLayout` (Task 4).

- [ ] **Step 1: Write the failing test**

Create `client/src/pages/platform/workflow-editor/utils/tests/buildTriggerFanInEdges.test.ts`:

```ts
import {Node} from '@xyflow/react';
import {describe, expect, it} from 'vitest';

import {buildTriggerFanInEdges} from '../layoutUtils';

describe('buildTriggerFanInEdges', () => {
    it('creates one edge from each trigger to the target', () => {
        const triggerNodes = [
            {data: {}, id: 'trigger_1', position: {x: 0, y: 0}},
            {data: {}, id: 'trigger_2', position: {x: 0, y: 0}},
        ] as Node[];

        const edges = buildTriggerFanInEdges(triggerNodes, 'action_1');

        expect(edges).toHaveLength(2);
        expect(edges[0]).toMatchObject({source: 'trigger_1', target: 'action_1'});
        expect(edges[1]).toMatchObject({source: 'trigger_2', target: 'action_1'});
        expect(edges[0].id).toBe('trigger_1=>action_1');
    });

    it('returns an empty array when there are no triggers', () => {
        expect(buildTriggerFanInEdges([], 'action_1')).toEqual([]);
    });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd client && npx vitest run src/pages/platform/workflow-editor/utils/tests/buildTriggerFanInEdges.test.ts`
Expected: FAIL with "buildTriggerFanInEdges is not a function".

- [ ] **Step 3: Implement `buildTriggerFanInEdges`**

In `layoutUtils.tsx`, after `buildTriggerNodes`:

```tsx
export const buildTriggerFanInEdges = (triggerNodes: Node[], targetNodeId: string): Edge[] =>
    triggerNodes.map((triggerNode) => ({
        id: `${triggerNode.id}=>${targetNodeId}`,
        source: triggerNode.id,
        style: EDGE_STYLES,
        target: targetNodeId,
        type: 'workflow',
    }));
```

Add `EDGE_STYLES` to the existing `@/shared/constants` import in `layoutUtils.tsx` if not already present (alphabetised). `Edge` is already imported from `@xyflow/react`.

- [ ] **Step 4: Run test to verify it passes**

Run: `cd client && npx vitest run src/pages/platform/workflow-editor/utils/tests/buildTriggerFanInEdges.test.ts`
Expected: PASS (2 tests).

- [ ] **Step 5: Commit**

```bash
cd client && npm run format
git add src/pages/platform/workflow-editor/utils/layoutUtils.tsx \
        src/pages/platform/workflow-editor/utils/tests/buildTriggerFanInEdges.test.ts
git commit -m "<ticket> client - Add buildTriggerFanInEdges helper"
```

---

### Task 4: Wire multi-trigger nodes + fan-in into `useLayout`

**Files:**
- Modify: `client/src/pages/platform/workflow-editor/hooks/useLayout.tsx:202-217` (node build), `:411-414` (edge loop seed), `:607-616` (no-tasks branch), `:768-806` (effect consumption)

**Interfaces:**
- Consumes: `buildTriggerNodes`, `buildTriggerFanInEdges` (Tasks 2-3).
- Produces: nothing new exported; `allNodes` becomes `[...triggerNodes, placeholderNode, ...taskNodes]` (placeholder omitted in read-only), and `taskEdges` is prefixed with the fan-in edges.

**Note on testing:** This is React Flow + dagre integration. The branchable logic is already unit-tested in Tasks 1-3; this task is verified by `npm run check` (typecheck) plus the manual smoke test in Task 11. Do NOT fabricate a unit test that re-implements the hook.

- [ ] **Step 1: Replace the single-trigger node memo (lines 202-217)**

Delete `triggerComponentName` (`:202`), `triggerDefinition` (`:204-207`), and `triggerNode` (`:209-215`). Replace the `let allNodes` seed (`:217`) so the task loop starts WITHOUT any trigger:

```tsx
const {placeholderNode: triggerPlaceholderNode, triggerNodes} = useMemo(
    () => buildTriggerNodes(triggers, componentDefinitions, canvasWidth),
    [triggers, componentDefinitions, canvasWidth]
);

let allNodes: Array<Node> = [];
```

Update imports: add `buildTriggerNodes` and `buildTriggerFanInEdges` to the existing `../utils/layoutUtils` import block (alphabetised) and remove now-unused `convertTaskToNode`? — it is STILL used at the task loop (`:258`), so keep it. Remove `createDefaultNodes` from the import only if no longer referenced in this file (it now lives inside `buildTriggerNodes`); verify with `grep -n createDefaultNodes useLayout.tsx` and drop the import if unused.

- [ ] **Step 2: Keep the task loop unchanged, but fix the no-tasks branch and final assembly**

The task loop (`:219-400`) is unchanged — it now pushes only task nodes into `allNodes`.

The edge loop (`:414-617`) still runs over `allNodes` (task nodes only). Two changes:

a) Capture the first task node id for fan-in BEFORE the edge loop. Right after the task loop closes (just before `const taskEdges` at `:411`), add:

```tsx
const firstDownstreamNodeId = allNodes[0]?.id ?? FINAL_PLACEHOLDER_NODE_ID;
```

b) The no-tasks case: when there are no tasks, `allNodes` is empty, so the edge loop body never runs and the final placeholder is never added. Handle it explicitly after computing `firstDownstreamNodeId`:

```tsx
if (allNodes.length === 0) {
    allNodes.push(finalPlaceholderNode);
}
```

(`finalPlaceholderNode` is defined at `:402-409`; move its `useMemo` definition ABOVE this block, or convert it to a plain object literal so it is available here. Simplest: change `:402-409` from a `useMemo` to a plain `const finalPlaceholderNode: Node = {data: {label: '+'}, id: FINAL_PLACEHOLDER_NODE_ID, position: {x: 0, y: 0}, type: 'placeholder'};` declared before the `firstDownstreamNodeId` line.)

- [ ] **Step 3: Build the fan-in edges and assemble the layout arrays**

After the edge loop (`:617`), assemble combined nodes/edges. Insert:

```tsx
const triggerFanInEdges = buildTriggerFanInEdges(triggerNodes, firstDownstreamNodeId);

allNodes = [...triggerNodes, triggerPlaceholderNode, ...allNodes];

taskEdges.unshift(...triggerFanInEdges);
```

Because `allNodes` and `taskEdges` are the exact variables the layout effect already consumes (`:768-769`), nothing else in the effect needs structural change — but see Steps 4-5.

- [ ] **Step 4: Exclude the trigger placeholder in read-only mode**

In the read-only branch (`:771-806`), the trigger "+" slot must not render. After `layoutNodes = allNodes.map(...)` and before `layoutNodes.pop()` (`:795`), filter it out:

```tsx
layoutNodes = layoutNodes.filter((node) => node.id !== TRIGGER_PLACEHOLDER_NODE_ID);
```

Add `TRIGGER_PLACEHOLDER_NODE_ID` to the `@/shared/constants` import in `useLayout.tsx` (alphabetised).

- [ ] **Step 5: Verify typecheck + lint**

Run: `cd client && npm run typecheck && npm run lint -- src/pages/platform/workflow-editor/hooks/useLayout.tsx`
Expected: no type errors; no lint errors for the file. (Existing util tests still green — run `npx vitest run src/pages/platform/workflow-editor/utils/tests` to confirm.)

- [ ] **Step 6: Commit**

```bash
cd client && npm run format
git add src/pages/platform/workflow-editor/hooks/useLayout.tsx
git commit -m "<ticket> client - Render multiple trigger nodes with fan-in edges in useLayout"
```

---

### Task 5: `TriggerPlaceholderNode` component + node-type registration

**Files:**
- Create: `client/src/pages/platform/workflow-editor/nodes/TriggerPlaceholderNode.tsx`
- Modify: `client/src/pages/platform/workflow-editor/hooks/useWorkflowEditorCanvas.ts:32-39` (import), `:118-130` (`nodeTypes`)
- Test: `client/src/pages/platform/workflow-editor/nodes/tests/TriggerPlaceholderNode.test.tsx`

**Interfaces:**
- Produces: default-exported `TriggerPlaceholderNode` React component registered under `nodeTypes.triggerPlaceholder`. Clicking it opens `WorkflowNodesPopoverMenu` configured to show ONLY triggers (`hideActionComponents hideTaskDispatchers hideClusterElementComponents`, `hideTriggerComponents={false}`), whose selection appends a trigger (append wiring lands in Task 8 via `getNodeData`).
- Consumes: `TRIGGER_PLACEHOLDER_NODE_ID` (Task 2).

**Note:** The click→append path works once Task 8 makes `getNodeData` generate a unique name for the trigger slot. This task delivers the rendered "+" slot and its picker; Task 8 makes append produce a new name; Task 9 wires the drop path. The component is independently reviewable (it renders, opens the picker filtered to triggers).

- [ ] **Step 1: Write the failing render test**

Create `client/src/pages/platform/workflow-editor/nodes/tests/TriggerPlaceholderNode.test.tsx`:

```tsx
import {TRIGGER_PLACEHOLDER_NODE_ID} from '@/shared/constants';
import {ReactFlowProvider} from '@xyflow/react';
import {render, screen} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';

import TriggerPlaceholderNode from '../TriggerPlaceholderNode';

vi.mock('../../components/WorkflowNodesPopoverMenu', () => ({
    default: ({children}: {children: React.ReactNode}) => <div data-testid="trigger-picker">{children}</div>,
}));

vi.mock('../../providers/workflowEditorProvider', () => ({
    useWorkflowEditor: () => ({updateWorkflowMutation: undefined}),
}));

describe('TriggerPlaceholderNode', () => {
    it('renders a + slot wrapped in the trigger picker', () => {
        render(
            <ReactFlowProvider>
                <TriggerPlaceholderNode data={{label: '+'}} id={TRIGGER_PLACEHOLDER_NODE_ID} />
            </ReactFlowProvider>
        );

        expect(screen.getByTestId('trigger-picker')).toBeInTheDocument();
        expect(screen.getByTitle('Click to add a trigger')).toBeInTheDocument();
    });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd client && npx vitest run src/pages/platform/workflow-editor/nodes/tests/TriggerPlaceholderNode.test.tsx`
Expected: FAIL — module `../TriggerPlaceholderNode` does not exist.

- [ ] **Step 3: Implement the component**

Create `client/src/pages/platform/workflow-editor/nodes/TriggerPlaceholderNode.tsx` (mirrors `PlaceholderNode.tsx` but trigger-only and without the paste context menu):

```tsx
import {NodeDataType} from '@/shared/types';
import {Handle, Position} from '@xyflow/react';
import {memo} from 'react';
import {twMerge} from 'tailwind-merge';

import WorkflowNodesPopoverMenu from '../components/WorkflowNodesPopoverMenu';
import useLayoutDirectionStore from '../stores/useLayoutDirectionStore';
import {mapHandlePosition} from '../utils/directionUtils';
import styles from './NodeTypes.module.css';

const TriggerPlaceholderNode = ({data, id}: {data: NodeDataType; id: string}) => {
    const layoutDirection = useLayoutDirectionStore((state) => state.layoutDirection);

    return (
        <WorkflowNodesPopoverMenu
            hideActionComponents
            hideClusterElementComponents
            hideTaskDispatchers
            sourceNodeId={id}
        >
            <div
                className={twMerge(
                    'nodrag relative mx-[22px] flex size-7 cursor-pointer items-center justify-center rounded-md bg-gray-300 text-lg text-content-neutral-secondary shadow-none hover:scale-110 hover:bg-gray-500 hover:text-white'
                )}
                title="Click to add a trigger"
            >
                {data.label}

                <Handle
                    className={twMerge(styles.handle, 'invisible')}
                    position={mapHandlePosition(Position.Bottom, layoutDirection)}
                    type="source"
                />
            </div>
        </WorkflowNodesPopoverMenu>
    );
};

export default memo(TriggerPlaceholderNode);
```

Notes: `hideTriggerComponents` defaults to `false`, so triggers show. The "+" slot has no incoming edge and no outgoing edge in the graph; the `source` Handle is rendered but hidden (`invisible`) to satisfy React Flow without drawing a connector.

- [ ] **Step 4: Register the node type**

In `useWorkflowEditorCanvas.ts`, add the import (alphabetised among the `../nodes/*` imports, `:32-39`):

```tsx
import TriggerPlaceholderNode from '../nodes/TriggerPlaceholderNode';
```

Add to the `nodeTypes` map (`:118-130`, keep keys alphabetical):

```tsx
const nodeTypes = useMemo(
    () => ({
        clusterRoot: AiAgentNode,
        placeholder: PlaceholderNode,
        readonly: ReadOnlyNode,
        readonlyPlaceholder: ReadOnlyPlaceholderNode,
        taskDispatcherBottomGhostNode: TaskDispatcherBottomGhostNode,
        taskDispatcherLeftGhostNode: TaskDispatcherLeftGhostNode,
        taskDispatcherTopGhostNode: TaskDispatcherTopGhostNode,
        triggerPlaceholder: TriggerPlaceholderNode,
        workflow: WorkflowNode,
    }),
    []
);
```

- [ ] **Step 5: Run test + typecheck**

Run: `cd client && npx vitest run src/pages/platform/workflow-editor/nodes/tests/TriggerPlaceholderNode.test.tsx && npm run typecheck`
Expected: PASS; no type errors.

- [ ] **Step 6: Commit**

```bash
cd client && npm run format
git add src/pages/platform/workflow-editor/nodes/TriggerPlaceholderNode.tsx \
        src/pages/platform/workflow-editor/nodes/tests/TriggerPlaceholderNode.test.tsx \
        src/pages/platform/workflow-editor/hooks/useWorkflowEditorCanvas.ts
git commit -m "<ticket> client - Add TriggerPlaceholderNode and register triggerPlaceholder node type"
```

---

### Task 6: Post-dagre placement of the trigger "+" slot

**Files:**
- Modify: `client/src/pages/platform/workflow-editor/utils/layoutUtils.tsx` (add `positionTriggerPlaceholder` helper; call it in the post-dagre pipeline `:685-707`)
- Test: `client/src/pages/platform/workflow-editor/utils/tests/positionTriggerPlaceholder.test.ts`

**Interfaces:**
- Produces: `positionTriggerPlaceholder(nodes: Node[], direction: LayoutDirectionType): void` — mutates the node whose id === `TRIGGER_PLACEHOLDER_NODE_ID`, placing it after the last real trigger node (to the right in `TB`, below in `LR`). No-op if the placeholder or trigger nodes are absent.
- Consumes: `TRIGGER_PLACEHOLDER_NODE_ID`.

- [ ] **Step 1: Write the failing test**

Create `client/src/pages/platform/workflow-editor/utils/tests/positionTriggerPlaceholder.test.ts`:

```ts
import {TRIGGER_PLACEHOLDER_NODE_ID} from '@/shared/constants';
import {Node} from '@xyflow/react';
import {describe, expect, it} from 'vitest';

import {positionTriggerPlaceholder} from '../layoutUtils';

const triggerNode = (id: string, x: number): Node =>
    ({data: {trigger: true}, id, position: {x, y: 50}}) as Node;

describe('positionTriggerPlaceholder', () => {
    it('places the slot to the right of the rightmost trigger in TB', () => {
        const nodes = [
            triggerNode('trigger_1', 100),
            triggerNode('trigger_2', 300),
            {data: {label: '+'}, id: TRIGGER_PLACEHOLDER_NODE_ID, position: {x: 0, y: 0}} as Node,
        ];

        positionTriggerPlaceholder(nodes, 'TB');

        const slot = nodes.find((node) => node.id === TRIGGER_PLACEHOLDER_NODE_ID)!;
        expect(slot.position.x).toBeGreaterThan(300);
        expect(slot.position.y).toBe(50);
    });

    it('is a no-op when there is no placeholder', () => {
        const nodes = [triggerNode('trigger_1', 100)];

        expect(() => positionTriggerPlaceholder(nodes, 'TB')).not.toThrow();
    });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd client && npx vitest run src/pages/platform/workflow-editor/utils/tests/positionTriggerPlaceholder.test.ts`
Expected: FAIL with "positionTriggerPlaceholder is not a function".

- [ ] **Step 3: Implement the helper**

In `layoutUtils.tsx`, add (use the existing `NODE_WIDTH` / `NODE_HEIGHT` constants already defined in this file; `TRIGGER_PLACEHOLDER_GAP` is a new local constant):

```tsx
const TRIGGER_PLACEHOLDER_GAP = 40;

export const positionTriggerPlaceholder = (nodes: Node[], direction: LayoutDirectionType): void => {
    const placeholderNode = nodes.find((node) => node.id === TRIGGER_PLACEHOLDER_NODE_ID);

    if (!placeholderNode) {
        return;
    }

    const triggerNodes = nodes.filter((node) => node.data?.trigger === true && node.id !== TRIGGER_PLACEHOLDER_NODE_ID);

    if (triggerNodes.length === 0) {
        return;
    }

    if (direction === 'LR') {
        const lowestTrigger = triggerNodes.reduce((lowest, node) =>
            node.position.y > lowest.position.y ? node : lowest
        );

        placeholderNode.position = {
            x: lowestTrigger.position.x,
            y: lowestTrigger.position.y + NODE_HEIGHT + TRIGGER_PLACEHOLDER_GAP,
        };
    } else {
        const rightmostTrigger = triggerNodes.reduce((rightmost, node) =>
            node.position.x > rightmost.position.x ? node : rightmost
        );

        placeholderNode.position = {
            x: rightmostTrigger.position.x + NODE_WIDTH + TRIGGER_PLACEHOLDER_GAP,
            y: rightmostTrigger.position.y,
        };
    }
};
```

- [ ] **Step 4: Call it in the post-dagre pipeline**

In `getLayoutElements`, after `constrainLeftGhostPositions(...)` (`:703`) and the `if (direction === 'LR')` block, add:

```tsx
positionTriggerPlaceholder(allNodes, direction);
```

(Place it after `applySavedPositions`/chain alignment is fine too, but before the final node return; putting it right after `constrainLeftGhostPositions` is simplest and unaffected by saved positions since the slot is never user-positioned.)

- [ ] **Step 5: Run test + typecheck**

Run: `cd client && npx vitest run src/pages/platform/workflow-editor/utils/tests/positionTriggerPlaceholder.test.ts && npm run typecheck`
Expected: PASS; no type errors.

- [ ] **Step 6: Commit**

```bash
cd client && npm run format
git add src/pages/platform/workflow-editor/utils/layoutUtils.tsx \
        src/pages/platform/workflow-editor/utils/tests/positionTriggerPlaceholder.test.ts
git commit -m "<ticket> client - Position trigger + slot beside the rightmost trigger after dagre"
```

---

### Task 7: Trigger save becomes an upsert-by-name

**Files:**
- Create: `client/src/pages/platform/workflow-editor/utils/upsertTrigger.ts`
- Modify: `client/src/pages/platform/workflow-editor/utils/saveWorkflowDefinition.ts:72-98`
- Test: `client/src/pages/platform/workflow-editor/utils/tests/upsertTrigger.test.ts`

**Interfaces:**
- Produces: `upsertTrigger(triggers: WorkflowTrigger[], newTrigger: WorkflowTrigger): WorkflowTrigger[]` — if a trigger with the same `name` exists, replace it in place (preserving the existing trigger's `metadata` so a swap keeps its saved position); otherwise append.
- Consumed by: `saveWorkflowDefinition` trigger branch.

- [ ] **Step 1: Write the failing test**

Create `client/src/pages/platform/workflow-editor/utils/tests/upsertTrigger.test.ts`:

```ts
import {WorkflowTrigger} from '@/shared/middleware/platform/configuration';
import {describe, expect, it} from 'vitest';

import upsertTrigger from '../upsertTrigger';

describe('upsertTrigger', () => {
    it('appends when the trigger name is new', () => {
        const existing = [{name: 'trigger_1', type: 'webhook/v1/onReceive'}] as WorkflowTrigger[];
        const next = {name: 'trigger_2', type: 'schedule/v1/onInterval'} as WorkflowTrigger;

        const result = upsertTrigger(existing, next);

        expect(result.map((trigger) => trigger.name)).toEqual(['trigger_1', 'trigger_2']);
    });

    it('replaces in place when the name already exists, preserving existing metadata', () => {
        const existing = [
            {metadata: {ui: {nodePosition: {x: 10, y: 20}}}, name: 'trigger_1', type: 'webhook/v1/onReceive'},
            {name: 'trigger_2', type: 'schedule/v1/onInterval'},
        ] as WorkflowTrigger[];
        const next = {name: 'trigger_1', type: 'manual/v1/manual'} as WorkflowTrigger;

        const result = upsertTrigger(existing, next);

        expect(result).toHaveLength(2);
        expect(result[0].type).toBe('manual/v1/manual');
        expect(result[0].metadata).toEqual({ui: {nodePosition: {x: 10, y: 20}}});
        expect(result[1].name).toBe('trigger_2');
    });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd client && npx vitest run src/pages/platform/workflow-editor/utils/tests/upsertTrigger.test.ts`
Expected: FAIL — module not found.

- [ ] **Step 3: Implement `upsertTrigger`**

Create `client/src/pages/platform/workflow-editor/utils/upsertTrigger.ts`:

```ts
import {WorkflowTrigger} from '@/shared/middleware/platform/configuration';

export default function upsertTrigger(
    triggers: WorkflowTrigger[],
    newTrigger: WorkflowTrigger
): WorkflowTrigger[] {
    const existingIndex = triggers.findIndex((trigger) => trigger.name === newTrigger.name);

    if (existingIndex === -1) {
        return [...triggers, newTrigger];
    }

    const mergedTrigger: WorkflowTrigger = {
        ...newTrigger,
        metadata: newTrigger.metadata ?? triggers[existingIndex].metadata,
    };

    return [...triggers.slice(0, existingIndex), mergedTrigger, ...triggers.slice(existingIndex + 1)];
}
```

- [ ] **Step 4: Use it in `saveWorkflowDefinition` (lines 72-98)**

Replace the trigger branch body so it reads existing triggers and upserts:

```tsx
if (trigger) {
    if (!type) {
        type = `${componentName}/v${version}/${operationName}`;
    }

    const newTrigger: WorkflowTrigger = {
        description,
        label,
        metadata,
        name: name!,
        parameters,
        type,
    };

    const existingTriggers: Array<WorkflowTrigger> = workflowDefinition.triggers ?? [];

    executeWorkflowMutation({
        definitionUpdate: {triggers: upsertTrigger(existingTriggers, newTrigger)},
        onSuccess: () => {
            if (onSuccess) {
                onSuccess();
            }
        },
        updateWorkflowMutation,
        workflow,
        workflowDefinition,
    });

    return;
}
```

Add `import upsertTrigger from './upsertTrigger';` (alphabetised in the `./` import group). Note `WorkflowDefinitionType.triggers` exists; if TypeScript reports `triggers` missing on `workflowDefinition`, confirm `WorkflowDefinitionType` in `@/shared/types` includes `triggers?: Array<WorkflowTriggerType>` (it does per `shared/types.ts`).

- [ ] **Step 5: Run test + typecheck**

Run: `cd client && npx vitest run src/pages/platform/workflow-editor/utils/tests/upsertTrigger.test.ts && npm run typecheck`
Expected: PASS; no type errors.

- [ ] **Step 6: Commit**

```bash
cd client && npm run format
git add src/pages/platform/workflow-editor/utils/upsertTrigger.ts \
        src/pages/platform/workflow-editor/utils/tests/upsertTrigger.test.ts \
        src/pages/platform/workflow-editor/utils/saveWorkflowDefinition.ts
git commit -m "<ticket> client - Upsert triggers by name instead of replacing the array"
```

---

### Task 8: Generate unique names for appended triggers (click + drag node data)

**Files:**
- Modify: `client/src/pages/platform/workflow-editor/components/WorkflowNodesPopoverMenuOperationList.tsx:162-193` (`getNodeData`)
- Modify: `client/src/pages/platform/workflow-editor/hooks/useHandleDrop.tsx:32-46` (`createWorkflowNodeData`), `:241-255` (`handleDropOnTriggerNode` + new append handler)

**Interfaces:**
- Produces:
  - `getNodeData` returns a trigger node whose `name`/`workflowNodeName` is `sourceNodeName ?? getFormattedName('trigger')` (replace target name when provided; otherwise a fresh unique name).
  - `createWorkflowNodeData(droppedNode, queryClient, taskDispatcherDefinitions, targetTriggerName?)` — when `droppedNode.trigger`, names the node `targetTriggerName ?? getFormattedName('trigger')`.
  - `useHandleDrop` returns a 4th handler `handleDropOnTriggerPlaceholder(droppedNode)` (append) alongside the existing `handleDropOnTriggerNode(droppedNode, targetTriggerName)` (replace).
- Consumes: `getFormattedName` (existing), `upsertTrigger` indirectly (via save).
- Consumed by: `onDrop` dispatch (Task 9), `TriggerPlaceholderNode`'s picker (Task 5, click path).

**Why `getFormattedName('trigger')` works:** it scans existing node names containing `"trigger"` and returns `trigger_<max+1>` (or `trigger_1` when none). The Manual placeholder's `data.name` is `'manual'`, and the "+" slot is `'trigger-placeholder'`, so neither inflates the counter.

- [ ] **Step 1: Update `getNodeData` (operation list)**

The component already receives `sourceNodeName?: string`. Change the two hardcoded `'trigger_1'` occurrences (`:181`, `:189`) to:

```tsx
name: trigger ? (sourceNodeName ?? getFormattedName('trigger')) : getFormattedName(componentName),
```
and
```tsx
workflowNodeName: trigger ? (sourceNodeName ?? getFormattedName('trigger')) : getFormattedName(componentName),
```

Add `sourceNodeName` to `getNodeData`'s `useCallback` dependency array (`:192`). For the "+" slot, `TriggerPlaceholderNode` passes no `sourceNodeName`, so the append branch generates a fresh name. For an existing-trigger swap popover (future/other callers), passing `sourceNodeName` yields a replace.

- [ ] **Step 2: Update `createWorkflowNodeData` + handlers (drag path)**

In `useHandleDrop.tsx`, add an optional `targetTriggerName` param to `createWorkflowNodeData` and use it for trigger naming. Change the signature (`:32-36`) and the `baseNodeData.name`/`workflowNodeName` (`:40`, `:45`):

```tsx
async function createWorkflowNodeData(
    droppedNode: ClickedDefinitionType,
    queryClient: QueryClient,
    taskDispatcherDefinitions: TaskDispatcherDefinition[],
    targetTriggerName?: string
): Promise<{nodeData: NodeDataType; operationName?: string}> {
    const triggerName = droppedNode.trigger ? (targetTriggerName ?? getFormattedName('trigger')) : undefined;

    const baseNodeData: NodeDataType = {
        componentName: droppedNode.name!,
        label: droppedNode.title,
        name: droppedNode.trigger ? triggerName! : getFormattedName(droppedNode.name!),
        taskDispatcher: droppedNode.taskDispatcher,
        title: droppedNode?.title,
        trigger: droppedNode.trigger,
        version: droppedNode.version,
        workflowNodeName: droppedNode.trigger ? triggerName! : getFormattedName(droppedNode.name!),
    };
    // ...unchanged...
```

Replace the existing `handleDropOnTriggerNode` (`:241-255`) and add the append handler:

```tsx
async function handleDropOnTriggerNode(droppedNode: ClickedDefinitionType, targetTriggerName: string) {
    const {nodeData, operationName} = await createWorkflowNodeData(
        droppedNode,
        queryClient,
        taskDispatcherDefinitions,
        targetTriggerName
    );

    await saveDroppedNode({
        captureComponentUsed,
        nodeData,
        operationName,
        queryClient,
        updateWorkflowMutation: updateWorkflowMutation!,
    });
}

async function handleDropOnTriggerPlaceholder(droppedNode: ClickedDefinitionType) {
    const {nodeData, operationName} = await createWorkflowNodeData(
        droppedNode,
        queryClient,
        taskDispatcherDefinitions
    );

    await saveDroppedNode({
        captureComponentUsed,
        nodeData,
        operationName,
        queryClient,
        updateWorkflowMutation: updateWorkflowMutation!,
    });
}
```

Update the return tuple and its type (`:189-193`, `:257`):

```tsx
): [
    (targetNode: Node, droppedNode: ClickedDefinitionType) => void,
    (targetEdge: Edge, droppedNode: ClickedDefinitionType) => void,
    (droppedNode: ClickedDefinitionType, targetTriggerName: string) => void,
    (droppedNode: ClickedDefinitionType) => void,
] {
    // ...
    return [
        handleDropOnPlaceholderNode,
        handleDropOnWorkflowEdge,
        handleDropOnTriggerNode,
        handleDropOnTriggerPlaceholder,
    ];
}
```

- [ ] **Step 3: Typecheck (callers updated in Task 9)**

Run: `cd client && npm run typecheck`
Expected: a type error at `useWorkflowEditorCanvas.ts:108-110` (the destructure now has 4 elements and `handleDropOnTriggerNode` takes 2 args) — this is fixed in Task 9. If you want a green typecheck before committing, do Task 9 Step 1-2 first, then commit both together. Otherwise commit and proceed immediately to Task 9.

- [ ] **Step 4: Commit (with Task 9 if you prefer a green typecheck)**

```bash
cd client && npm run format
git add src/pages/platform/workflow-editor/components/WorkflowNodesPopoverMenuOperationList.tsx \
        src/pages/platform/workflow-editor/hooks/useHandleDrop.tsx
git commit -m "<ticket> client - Generate unique names for appended triggers"
```

---

### Task 9: Dispatch drops to append (on "+") vs replace (on a trigger)

**Files:**
- Modify: `client/src/pages/platform/workflow-editor/hooks/useWorkflowEditorCanvas.ts:108-110` (destructure), `:181-210` (trigger drop dispatch)

**Interfaces:**
- Consumes: the 4-tuple from `useHandleDrop` (Task 8).

**Note:** Verified by typecheck + the Task 11 manual smoke test (drop is DOM-driven, not unit-tested).

- [ ] **Step 1: Update the `useHandleDrop` destructure (lines 108-110)**

```tsx
const [
    handleDropOnPlaceholderNode,
    handleDropOnWorkflowEdge,
    handleDropOnTriggerNode,
    handleDropOnTriggerPlaceholder,
] = useHandleDrop({
    taskDispatcherDefinitions,
});
```

- [ ] **Step 2: Branch the trigger drop on the target (lines 181-210)**

Inside `if (droppedNodeType === 'trigger') { ... }`, after computing `targetNodeType` and `targetNodeElement`, replace the existing single `if (targetNodeType === 'trigger' ...)` block with logic that distinguishes the "+" slot from an existing trigger node:

```tsx
if (targetNodeType === 'trigger' && targetNodeElement instanceof HTMLElement) {
    const targetNodeId = targetNodeElement.dataset.id;

    if (!targetNodeId) {
        return;
    }

    const targetNode = useWorkflowDataStore.getState().nodes.find((node) => node.id === targetNodeId);

    if (targetNode) {
        const targetNodeName = (targetNode.data as NodeDataType).name;

        if (targetNodeName) {
            handleDropOnTriggerNode(droppedNode, targetNodeName);
        }
    }

    return;
}

const droppedOnTriggerPlaceholder = (event.target as HTMLElement)
    .closest('.react-flow__node')
    ?.getAttribute('data-id') === TRIGGER_PLACEHOLDER_NODE_ID;

if (droppedOnTriggerPlaceholder) {
    handleDropOnTriggerPlaceholder(droppedNode);

    return;
}

handleDropOnTriggerPlaceholder(droppedNode);
```

Rationale: a trigger dropped anywhere on the canvas that is NOT an existing trigger node appends (the final fallthrough `handleDropOnTriggerPlaceholder`). Dropping directly on the "+" slot also appends. Dropping on an existing trigger replaces by its name.

For the replace path to read the dropped trigger's chosen name, note that `targetNodeName` is the existing trigger's `name` (e.g. `trigger_2`); passing it makes `upsertTrigger` replace that slot. Confirm the manual-placeholder case: dropping on the synthetic Manual node (id `trigger_1`, `data.name` = `'manual'`) passes `targetNodeName = 'manual'`. Since no real trigger named `'manual'` exists, `upsertTrigger` would APPEND a trigger named `'manual'` — wrong. Guard it:

```tsx
const targetNodeData = targetNode.data as NodeDataType;
const isManualPlaceholder = targetNodeData.componentName === 'manual' && targetNodeData.operationName === 'manual';
const targetNodeName = isManualPlaceholder ? 'trigger_1' : targetNodeData.name;
```

This makes a drop on the empty-state Manual node create `trigger_1`, matching today's behavior.

Add `TRIGGER_PLACEHOLDER_NODE_ID` to the `@/shared/constants` import in `useWorkflowEditorCanvas.ts` and ensure `NodeDataType` is imported (it already is, `:21`).

- [ ] **Step 3: Typecheck + existing tests**

Run: `cd client && npm run typecheck && npx vitest run src/pages/platform/workflow-editor`
Expected: no type errors; all existing + new tests pass.

- [ ] **Step 4: Commit**

```bash
cd client && npm run format
git add src/pages/platform/workflow-editor/hooks/useWorkflowEditorCanvas.ts
git commit -m "<ticket> client - Dispatch trigger drops to append or replace by target"
```

---

### Task 10: Position & duplicate-name utilities across all triggers

**Files:**
- Modify: `client/src/pages/platform/workflow-editor/utils/extractDefinitionPositions.ts:37-39`
- Modify: `client/src/pages/platform/workflow-editor/utils/saveWorkflowNodesPosition.ts:199-215`
- Modify: `client/src/pages/platform/workflow-editor/utils/removeWorkflowNodePosition.ts:160-171`
- Modify: `client/src/pages/platform/workflow-editor/utils/clearAllNodePositions.ts:150-156`
- Modify: `client/src/pages/platform/workflow-editor/stores/useWorkflowDataStore.ts:348-351`
- Test: `client/src/pages/platform/workflow-editor/utils/tests/extractDefinitionPositions.test.ts`

**Interfaces:**
- `extractDefinitionPositions(definition: string): Map<string, {x, y}>` now includes saved positions for EVERY trigger, keyed by trigger name.
- The save/remove/clear utilities update the matching trigger by name instead of `triggers[0]`.

- [ ] **Step 1: Write the failing test (extract path)**

Create `client/src/pages/platform/workflow-editor/utils/tests/extractDefinitionPositions.test.ts`:

```ts
import {describe, expect, it} from 'vitest';

import extractDefinitionPositions from '../extractDefinitionPositions';

describe('extractDefinitionPositions', () => {
    it('collects saved positions for every trigger by name', () => {
        const definition = JSON.stringify({
            tasks: [],
            triggers: [
                {metadata: {ui: {nodePosition: {x: 1, y: 2}}}, name: 'trigger_1', type: 'webhook/v1/onReceive'},
                {metadata: {ui: {nodePosition: {x: 3, y: 4}}}, name: 'trigger_2', type: 'schedule/v1/onInterval'},
            ],
        });

        const positions = extractDefinitionPositions(definition);

        expect(positions.get('trigger_1')).toEqual({x: 1, y: 2});
        expect(positions.get('trigger_2')).toEqual({x: 3, y: 4});
    });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd client && npx vitest run src/pages/platform/workflow-editor/utils/tests/extractDefinitionPositions.test.ts`
Expected: FAIL — only `trigger_1` is collected.

- [ ] **Step 3: Update `extractDefinitionPositions` (lines 37-39)**

```ts
if (Array.isArray(parsed.triggers)) {
    for (const trigger of parsed.triggers) {
        if (trigger?.metadata?.ui?.nodePosition) {
            positionMap.set(trigger.name, trigger.metadata.ui.nodePosition);
        }
    }
}
```

- [ ] **Step 4: Update `saveWorkflowNodesPosition` (lines 199-215)**

Replace the `triggers?.[0]` block with a find-by-name across all triggers:

```ts
// Update trigger position if a trigger was the dragged node
if (Array.isArray(workflowDefinition.triggers)) {
    const draggedTriggerIndex = workflowDefinition.triggers.findIndex(
        (trigger: {name: string}) => trigger.name === draggedNodeId
    );

    const triggerPosition = nodePositions[draggedNodeId];

    if (draggedTriggerIndex !== -1 && triggerPosition) {
        const draggedTrigger = workflowDefinition.triggers[draggedTriggerIndex];

        workflowDefinition.triggers[draggedTriggerIndex] = {
            ...draggedTrigger,
            metadata: {
                ...draggedTrigger.metadata,
                ui: {
                    ...draggedTrigger.metadata?.ui,
                    nodePosition: triggerPosition,
                },
            },
        };
    }
}
```

- [ ] **Step 5: Update `removeWorkflowNodePosition` (lines 160-171)**

```ts
if (Array.isArray(workflowDefinition.triggers)) {
    const triggerIndex = workflowDefinition.triggers.findIndex(
        (trigger: {name: string}) => trigger.name === nodeName
    );

    if (triggerIndex !== -1) {
        const matchedTrigger = workflowDefinition.triggers[triggerIndex];

        workflowDefinition.triggers[triggerIndex] = {
            ...matchedTrigger,
            metadata: {
                ...matchedTrigger.metadata,
                ui: {
                    ...matchedTrigger.metadata?.ui,
                    nodePosition: undefined,
                },
            },
        };
    }
}
```

- [ ] **Step 6: Update `clearAllNodePositions` (lines 150-156)**

Read the current block first (`grep -n triggers src/pages/platform/workflow-editor/utils/clearAllNodePositions.ts`). Replace the single `triggers[0]` clear with a loop:

```ts
if (Array.isArray(workflowDefinition.triggers)) {
    workflowDefinition.triggers = workflowDefinition.triggers.map((trigger: {metadata?: {ui?: {nodePosition?: unknown}}}) => {
        if (trigger?.metadata?.ui?.nodePosition) {
            return {
                ...trigger,
                metadata: {
                    ...trigger.metadata,
                    ui: {
                        ...trigger.metadata.ui,
                        nodePosition: undefined,
                    },
                },
            };
        }

        return trigger;
    });
}
```

- [ ] **Step 7: Update duplicate-name seed in `useWorkflowDataStore.ts` (lines 348-351)**

So duplicate-name detection sees ALL triggers, not just the first:

```ts
const workflowNodes: Array<{name: string; type: string}> = [
    ...((workflow.triggers && workflow.triggers.length > 0
        ? workflow.triggers
        : [createDefaultNodes(1200)[0].data]) as Array<{name: string; type: string}>),
    ...(workflow?.tasks || []),
];
```

- [ ] **Step 8: Run test + typecheck + full editor tests**

Run: `cd client && npx vitest run src/pages/platform/workflow-editor/utils/tests/extractDefinitionPositions.test.ts && npm run typecheck`
Expected: PASS; no type errors.

- [ ] **Step 9: Commit**

```bash
cd client && npm run format
git add src/pages/platform/workflow-editor/utils/extractDefinitionPositions.ts \
        src/pages/platform/workflow-editor/utils/saveWorkflowNodesPosition.ts \
        src/pages/platform/workflow-editor/utils/removeWorkflowNodePosition.ts \
        src/pages/platform/workflow-editor/utils/clearAllNodePositions.ts \
        src/pages/platform/workflow-editor/stores/useWorkflowDataStore.ts \
        src/pages/platform/workflow-editor/utils/tests/extractDefinitionPositions.test.ts
git commit -m "<ticket> client - Handle node positions and duplicate names across all triggers"
```

---

### Task 11: Full verification + manual multi-trigger smoke test

**Files:** none (verification only).

- [ ] **Step 1: Run the full client check**

Run: `cd client && npm run check`
Expected: lint, typecheck, and all tests pass. Fix any `sort-keys` / import-order issues manually (ESLint does not auto-fix `sort-keys`).

- [ ] **Step 2: Manual smoke test (dev server)**

Run the client (`cd client && npm run dev`) against a running server, open a workflow, and verify:
1. A workflow with one trigger renders as before, with a "+" slot to the right of it (below it in LR layout — toggle layout direction).
2. Clicking the "+" slot opens a picker showing ONLY triggers; selecting one appends a second trigger named `trigger_2`; both triggers fan into the first task.
3. Dragging a trigger component onto the "+" slot also appends.
4. Dragging a trigger onto an existing trigger node REPLACES it (name preserved).
5. Each trigger node can be dragged to a custom position and it persists after reload (position saved by name).
6. Deleting triggers one by one works; deleting the last real trigger restores the synthetic "Manual" placeholder + "+" slot.
7. Read-only workflow views (e.g. project deployment view) do NOT show the "+" slot.
8. Configuring each trigger (open details panel, edit params) saves to the correct trigger.

- [ ] **Step 3: Record results**

If all checks pass, the feature is complete. If a manual step fails, capture the exact symptom and use `superpowers:systematic-debugging` before patching.

- [ ] **Step 4: Final commit (only if Step 1 required fixups)**

```bash
cd client && npm run format
git add -p   # stage only the fixup hunks
git commit -m "<ticket> client - Fix lint/format issues for multiple-trigger editor"
```

---

## Self-Review

**Spec coverage:**
- §1 explicit trigger-ness → Task 1.
- §2 build N trigger nodes / Manual fallback → Tasks 2, 4.
- §3 `triggerPlaceholder` "+" slot (drop + click) → Tasks 2 (node), 5 (component+register), 8/9 (drop+click append).
- §3 no outgoing edge for "+" + post-dagre placement → Tasks 3 (fan-in excludes it), 6.
- §4 fan-in edges → Tasks 3, 4.
- §5 save append-vs-replace (upsert by name) + unique naming → Tasks 7, 8, 9.
- §6 position persistence by name → Task 10.
- §7 deletion → existing delete path removes by name; empty-state restores Manual via Task 2's fallback (verified Task 11 step 6). No new code required; covered by smoke test.
- §8 both layout directions → Task 6 (direction-aware) + Task 11 manual.
- Testing section → unit tests in Tasks 1-3, 5-7, 10; integration via typecheck + Task 11 manual.

**Placeholder scan:** No "TBD"/"handle edge cases" left; every code step shows code. The `<ticket>` token in commit messages is intentional per the Global Constraints (replace with the real ticket number).

**Type consistency:** `convertTaskToNode(..., isTrigger: boolean)` (Task 1) is consumed consistently by `buildTriggerNodes` (Task 2) and the `useLayout` task loop (Task 4). `buildTriggerNodes`/`buildTriggerFanInEdges`/`positionTriggerPlaceholder`/`upsertTrigger` signatures match across producer and consumer tasks. `useHandleDrop`'s 4-tuple (Task 8) matches the destructure in Task 9. `TRIGGER_PLACEHOLDER_NODE_ID` (Task 2) is referenced by Tasks 4, 5, 6, 9.

**Risk watch-items (from the spec):**
- Per-trigger definition resolution: `useLayout` already receives the full `componentDefinitions` array as a prop, and `buildTriggerNodes` looks each trigger up in it — no N-hooks problem. ✓
- dagre ranking of the "+" slot: it has no edges, so dagre places it loosely; Task 6 repositions it deterministically post-dagre. Verify alignment in both directions during Task 11.
- `getLayoutElements` centers on `nodes[0]` (`:653`); with multiple triggers this centers on the first trigger, which is acceptable for v1. If the trigger row looks off-center in Task 11, that line is the place to revisit (out of scope to change now).
- Other `triggers[0]` consumers outside the editor (`AutomationWorkflowProjectWorkflowListItem`, `EmbeddableWorkflowEditor` chat-trigger check) are display/detection only — confirm untouched behavior during Task 11; no change planned.
