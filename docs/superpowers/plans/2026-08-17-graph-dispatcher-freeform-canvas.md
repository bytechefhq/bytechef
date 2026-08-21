# Graph Dispatcher Free-Form Canvas Implementation Plan (Plan B — client)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Render a `graph/v1` container as a bordered box in which its task nodes are freely draggable, connectable by dragging handles, with the box auto-sizing and the surrounding flow reflowing — consuming the edge-list DSL shipped by Plan A.

**Architecture:** A `graphFrame` React Flow node sits between the graph's existing top/bottom ghost anchors and is handed to whichever engine is active (ELK or dagre) as a *sized leaf*. A pre-pass (`layoutGraphFrames`) lays out each member's subtree with the active engine, places members from frame-relative `metadata.ui.nodePosition` (auto-placing and persisting the rest), computes the frame size, and re-appends members with `parentId = frame` after the outer engine returns. Transitions are real `graphTransition` edges built from `parameters.transitions`; the first `onConnect` in the app is scoped to graph member handles. The whole lane/overlay/literal-heuristic layer is retired.

**Tech Stack:** React 19, TypeScript 5.9, `@xyflow/react` 12 (`parentId`, `onConnect`, `onConnectEnd`, `NodeResizer` not used), `elkjs` 0.12 (`layered` for auto-place), dagre, Zustand, Vitest 4 + Testing Library, ESLint `sort-keys` / naming rules from CLAUDE.md, `twMerge`, Lucide `*Icon` imports.

**Spec:** `docs/superpowers/specs/2026-08-17-graph-dispatcher-freeform-canvas-design.md` (sections "Client design", "Rendering", "Positions", "Layout pipeline", "Interactions", "Read-only / execution view").

**Depends on:** Plan A (`docs/superpowers/plans/2026-08-17-graph-dispatcher-edge-list-dsl.md`) merged — the client reads `parameters.nodes` as a task list and `parameters.transitions`.

## Global Constraints

- Positions are frame-relative, non-negative, always honored inside a graph (spec "Positions").
- Members are always draggable regardless of the global drag lock (spec "Interactions").
- `nodesConnectable` stays `false` globally; only graph member transition handles are connectable.
- Both engines must produce identical member geometry; no ELK-only behaviour remains for graph.
- Client conventions: `sort-keys` (alphabetical object keys), interface names end in `I`/`Props`, `useRef` vars end in `Ref`, `twMerge` not `cn`, hooks ordered `useState → useRef → stores → custom hooks → memo/callback → useEffect`, Lucide icons with `Icon` suffix, `vi.hoisted` for mock refs.
- Run `cd client && npm run check` before every commit; commit messages `732 client - <description>`.
- Spec deviation recorded here (behaviourally identical): the structural fingerprint hashes each graph's **member positions and transitions** rather than a computed frame size — a same-size drop re-runs the outer layout, but the tween is a no-op because nothing outside moved.

Path prefix used throughout: `EDITOR=client/src/pages/platform/workflow-editor`.

---

### Task 1: Types, collections, and every generic `nodes` walker

**Files:**
- Modify: `client/src/shared/types.ts:143-147` (`GraphDataType`), `:276-280` (`GraphNodeType` → `GraphTransitionType`)
- Modify: `$EDITOR/utils/taskTraversalUtils.ts:53-58`
- Modify: `$EDITOR/utils/getTask.ts:35-36`
- Modify: `$EDITOR/utils/getRecursivelyUpdatedTasks.ts:109-115`
- Modify: `$EDITOR/utils/pasteNode.ts:203-206`
- Modify: `$EDITOR/utils/handleDeleteTask.ts:233-275`
- Modify: `$EDITOR/utils/saveWorkflowNodesPosition.ts:52-175` (add a `nodes` arm)
- Modify: `$EDITOR/utils/taskDispatcherConfig.tsx:424-506`
- Modify: `$EDITOR/utils/layoutUtils.tsx:1257-1268`, `:1566-1572`, `:1795-1815`
- Modify: `$EDITOR/utils/getTaskDispatcherContext.ts:51-55`, `:138-146`
- Modify: `$EDITOR/utils/saveTaskDispatcherSubtaskFieldChange.ts:163-175`
- Delete: `$EDITOR/utils/graphNodeMutations.ts` + test, `$EDITOR/utils/extractNextTargets.ts` + test, `$EDITOR/utils/orderGraphNodeIndexes.ts` + test, `$EDITOR/utils/deriveGraphTransitionEdges.ts` + test
- Tests: `$EDITOR/utils/handleDeleteTask.test.ts`, `$EDITOR/utils/pasteNode.test.ts`, `$EDITOR/utils/getRecursivelyUpdatedTasks.test.ts`, `$EDITOR/utils/getTask.test.ts`, new `$EDITOR/utils/saveWorkflowNodesPosition.test.ts` (or extend existing if present)

**Interfaces:**
- Produces `GraphTransitionType = {condition?: string; from: string; to: string}` and `GraphDataType = {graphId: string; index: number}` (no `nodeIndex`).
- Produces `TASK_DISPATCHER_CONFIG.graph` shaped like `parallel` (plain list `parameters.nodes`), with `getInitialParameters`/`initializeParameters` returning `{maxTransitions: 100, nodes: [], transitions: []}` and `updateTaskParameters` appending to `nodes`.

- [ ] **Step 1: Write failing tests** for the walkers. In `handleDeleteTask.test.ts` add:

```ts
it('removes a graph node task together with every transition naming it and clears startNode', () => {
    // arrange a workflow whose task graph_1 has nodes [a, b, c], transitions a->b, b->c, c->a, startNode b
    // act: delete task b (data.graphData = {graphId: 'graph_1', index: 1})
    // assert: nodes = [a, c]; transitions = [c->a]; parameters.startNode is undefined
});
```
In `pasteNode.test.ts` add a case: pasting a graph with nodes `[x_1, y_1]` and transitions `x_1->y_1` when `x_1`/`y_1` already exist yields renamed nodes `x_2`, `y_2` AND transitions `x_2->y_2` (both `from` and `to` remapped; a dynamic `to` starting with `=` is left untouched).
In `saveWorkflowNodesPosition.test.ts` add: `updateTaskPositions([{name:'graph_1', type:'graph/v1', parameters:{nodes:[{name:'a',type:'t'}]}}], {a: {x: 10, y: 20}})` writes `nodes[0].metadata.ui.nodePosition = {x:10,y:20}`.

- [ ] **Step 2: Run** `cd client && npx vitest run src/pages/platform/workflow-editor/utils/handleDeleteTask.test.ts src/pages/platform/workflow-editor/utils/pasteNode.test.ts` → FAIL.

- [ ] **Step 3: Implement**

`shared/types.ts`:
```ts
type GraphDataType = {
    graphId: string;
    index: number;
};

export type GraphTransitionType = {
    condition?: string;
    from: string;
    to: string;
};
```
Remove `GraphNodeType`.

`taskTraversalUtils.ts:53-58`:
```ts
if (Array.isArray(parameters.nodes)) {
    callback(parameters.nodes as WorkflowTask[], 'nodes');
}
```
`getTask.ts:35-36`: delete the `nodes` special-case (a plain list needs none). `getRecursivelyUpdatedTasks.ts:109-115`:
```ts
if (task.parameters?.nodes) {
    return updateTaskParameter(
        task,
        'nodes',
        getRecursivelyUpdatedTasks(task.parameters.nodes as WorkflowTask[], taskToReplace)
    );
}
```
`pasteNode.ts:203-206`:
```ts
case 'graph':
    if (Array.isArray(parameters.nodes)) {
        (parameters.nodes as WorkflowTask[]).forEach(renameTask);
    }

    if (Array.isArray(parameters.transitions)) {
        parameters.transitions = (parameters.transitions as GraphTransitionType[]).map((transition) => ({
            ...transition,
            from: renamedTaskNames.get(transition.from) ?? transition.from,
            to: transition.to.startsWith('=') ? transition.to : (renamedTaskNames.get(transition.to) ?? transition.to),
        }));
    }

    if (typeof parameters.startNode === 'string') {
        parameters.startNode = renamedTaskNames.get(parameters.startNode) ?? parameters.startNode;
    }

    break;
```
(`renamedTaskNames: Map<string, string>` — read how `renameTask` records old→new names in this file; if it only mutates in place, add the map alongside it.)

`handleDeleteTask.ts:233-275` — replace the graph branch with:
```ts
} else if (data.graphData) {
    const parentGraphTask = TASK_DISPATCHER_CONFIG.graph.getTask({
        taskDispatcherId: data.graphData.graphId,
        tasks: workflowDefinition.tasks,
    });

    if (!parentGraphTask) {
        return;
    }

    const graphNodes = (parentGraphTask.parameters?.nodes || []) as WorkflowTask[];
    const graphTransitions = (parentGraphTask.parameters?.transitions || []) as GraphTransitionType[];

    parentGraphTask.parameters = {
        ...parentGraphTask.parameters,
        nodes: graphNodes.filter((graphNode) => graphNode.name !== data.name),
        startNode: parentGraphTask.parameters?.startNode === data.name ? undefined : parentGraphTask.parameters?.startNode,
        transitions: graphTransitions.filter(
            (transition) => transition.from !== data.name && transition.to !== data.name
        ),
    };

    updatedTasks = getRecursivelyUpdatedTasks(workflowDefinition.tasks, parentGraphTask);
}
```

`saveWorkflowNodesPosition.ts` — add after the `branches` arm:
```ts
if (updatedTask.parameters?.nodes) {
    updatedTask = {
        ...updatedTask,
        parameters: {
            ...updatedTask.parameters,
            nodes: updateTaskPositions(
                updatedTask.parameters.nodes as WorkflowTask[],
                nodePositions,
                clearPositionNodeIds
            ),
        },
    };
}
```

`taskDispatcherConfig.tsx` graph entry (model on `parallel`; note `nodes` instead of `tasks`, plus `transitions`):
```ts
graph: {
    buildNodeData: ({baseNodeData, taskDispatcherContext, taskDispatcherId}: BuildNodeDataType): NodeDataType =>
        buildGenericNodeData(baseNodeData, taskDispatcherContext, taskDispatcherId, 'graph'),
    contextIdentifier: 'graphId',
    dataKey: 'graphData',
    extractContextFromPlaceholder: (placeholderId: string): TaskDispatcherContextType => {
        // Convention: `<graphId>-graph-placeholder` (the frame's Add-node placeholder). Members are
        // appended, so `index` is resolved by updateTaskParameters, not parsed from the id.
        const parts = placeholderId.split('-');

        return {taskDispatcherId: parts[0]};
    },
    getDispatcherId: (context: TaskDispatcherContextType) => context.graphId,
    getInitialParameters: (properties: Array<PropertyAllType>) => ({
        ...getParametersWithDefaultValues({properties}),
        maxTransitions: 100,
        nodes: [],
        transitions: [],
    }),
    getSubtasks: ({node, task}: {node?: Node; task?: WorkflowTask}): Array<WorkflowTask> => {
        const parameters = (node?.data as NodeDataType)?.parameters || task?.parameters;

        return (parameters?.nodes || []) as Array<WorkflowTask>;
    },
    getTask: getTaskDispatcherTask,
    initializeParameters: () => ({
        maxTransitions: 100,
        nodes: [],
        transitions: [],
    }),
    updateTaskParameters: ({task, updatedSubtasks}: UpdateTaskParametersType): WorkflowTask => ({
        ...task,
        parameters: {
            ...task.parameters,
            nodes: updatedSubtasks,
        },
    }),
},
```

`layoutUtils.tsx:1257-1268` graph case → `parentSubtasks = (parentTaskDispatcher.parameters?.nodes || []) as WorkflowTask[]; break;`. `:1566-1572` → `graphChildTasks[name] = {nodes: Array.isArray(parameters?.nodes) ? parameters.nodes.map((task: WorkflowTask) => task.name) : []};` with `GraphChildTasksType = Record<string, {nodes: string[]}>`. `:1795-1815` →
```ts
if (!isNested) {
    for (const [graphId, graphData] of Object.entries(graphChildTasks)) {
        const taskIndex = graphData.nodes.indexOf(taskName);

        if (taskIndex !== -1) {
            nestingData = {graphData: {graphId, index: taskIndex}};
            isNested = true;

            break;
        }
    }
}
```
`getTaskDispatcherContext.ts:51-55` drop the `nodeIndex` line; `:138-146` graph placeholder branch → `context.graphId = graphId; context.taskDispatcherId = graphId;`. `saveTaskDispatcherSubtaskFieldChange.ts:163-175` drop `nodeIndex`.

Delete the four retired utils + tests; fix imports that referenced them (grep `graphNodeMutations|extractNextTargets|orderGraphNodeIndexes|deriveGraphTransitionEdges`) — the remaining importers (`GraphNodeLabel`, `useGraphNodeLabel`, `GraphStatesPanel`, `GraphNextNodeSuggestions`, `GraphTransitionBadges`, `createGraphEdges`, `elkLayoutUtils`) are rewritten/deleted in Tasks 4–8; to keep `npm run typecheck` green at this commit, delete `GraphNodeLabel.tsx`, `useGraphNodeLabel.ts`, `LabeledGraphNodeEdge.tsx`, `GraphNextNodeSuggestions.tsx`, `GraphTransitionBadges.tsx`, `GraphStatesPanel.tsx` now and stub their call sites (`WorkflowEdge.tsx:293-303` remove the graph chip branch; `WorkflowNodeDetailsPanel.tsx:331` remove the panel usage — Task 8 adds the replacement; edge type registration for `labeledGraphNode` removed in `useWorkflowEditorCanvas`).

- [ ] **Step 4: Run tests + typecheck** — `cd client && npm run typecheck && npx vitest run src/pages/platform/workflow-editor/utils` → PASS.

- [ ] **Step 5: Commit** — `732 client - Treat graph nodes as a plain task list with an explicit transitions list`.

---

### Task 2: Pure graph mutations and geometry

**Files:**
- Create: `$EDITOR/utils/graph/graphTransitionMutations.ts` + `.test.ts`
- Create: `$EDITOR/utils/graph/graphFrameGeometry.ts` + `.test.ts`
- Create: `$EDITOR/utils/graph/saveGraphParameters.ts`

**Interfaces (produced):**
```ts
// graphTransitionMutations.ts
export function isDynamicTransitionTarget(to: string): boolean;                     // contains '${' or starts with '='
export function addTransition(transitions: GraphTransitionType[], from: string, to: string): GraphTransitionType[]; // no-op if same from/to pair exists
export function removeTransition(transitions: GraphTransitionType[], index: number): GraphTransitionType[];
export function updateTransition(transitions: GraphTransitionType[], index: number, patch: Partial<GraphTransitionType>): GraphTransitionType[];
export function moveTransition(transitions: GraphTransitionType[], index: number, direction: -1 | 1): GraphTransitionType[]; // within same `from` group
export function removeTransitionsForNode(transitions: GraphTransitionType[], nodeName: string): GraphTransitionType[];
export function transitionsFrom(transitions: GraphTransitionType[], nodeName: string): Array<{index: number; transition: GraphTransitionType}>;
export function findNodesWithDuplicateDefault(transitions: GraphTransitionType[]): string[]; // node names with >1 unconditional outgoing
export function isUnconditional(transition: GraphTransitionType): boolean;

// graphFrameGeometry.ts
export const GRAPH_FRAME_PADDING = 24;
export const GRAPH_FRAME_HEADER_HEIGHT = 40;
export const GRAPH_FRAME_MIN_WIDTH = 320;
export const GRAPH_FRAME_MIN_HEIGHT = 200;
export const GRAPH_START_SIZE = {height: 32, width: 72};
export const GRAPH_MEMBER_NOMINAL_SIZE = {height: 100, width: 240};
export interface GraphMemberBoxI {height: number; name: string; width: number; x: number; y: number}
export function computeGraphFrameSize(memberBoxes: GraphMemberBoxI[]): {height: number; width: number};
export function findFreeSpot(memberBoxes: GraphMemberBoxI[]): {x: number; y: number}; // right of the rightmost box on the top row, else (GRAPH_START_SIZE.width + 40, 0)
export async function autoPlaceGraphMembers(memberSizes: Array<{height: number; name: string; width: number}>, transitions: GraphTransitionType[], pinned: GraphMemberBoxI[]): Promise<Record<string, {x: number; y: number}>>; // ELK layered RIGHT, offset below pinned bounding box

// saveGraphParameters.ts
export function saveGraphParameters(graphId: string, updater: (parameters: Record<string, unknown>) => Record<string, unknown>, updateWorkflowMutation: UpdateWorkflowMutationType): void;
```

- [ ] **Step 1: Write failing tests** (`graphTransitionMutations.test.ts`):

```ts
describe('graphTransitionMutations', () => {
    const base: GraphTransitionType[] = [
        {from: 'a', to: 'b'},
        {condition: '=x', from: 'a', to: 'c'},
        {from: 'b', to: 'a'},
    ];

    it('addTransition appends and dedupes', () => {
        expect(addTransition(base, 'c', 'a')).toHaveLength(4);
        expect(addTransition(base, 'a', 'b')).toBe(base);
    });

    it('removeTransitionsForNode drops both directions', () => {
        expect(removeTransitionsForNode(base, 'b')).toEqual([{condition: '=x', from: 'a', to: 'c'}]);
    });

    it('findNodesWithDuplicateDefault flags nodes with two unconditional edges', () => {
        expect(findNodesWithDuplicateDefault([...base, {from: 'a', to: 'd'}])).toEqual(['a']);
        expect(findNodesWithDuplicateDefault(base)).toEqual([]);
    });

    it('isDynamicTransitionTarget detects expressions', () => {
        expect(isDynamicTransitionTarget('=nextNode')).toBe(true);
        expect(isDynamicTransitionTarget('${a.b}')).toBe(true);
        expect(isDynamicTransitionTarget('approve')).toBe(false);
    });

    it('moveTransition swaps within the same from-group only', () => {
        const moved = moveTransition(base, 1, -1);

        expect(moved[0]).toEqual({condition: '=x', from: 'a', to: 'c'});
        expect(moveTransition(base, 2, -1)).toBe(base); // 'b' group has one edge
    });
});
```
`graphFrameGeometry.test.ts`:
```ts
it('computeGraphFrameSize pads the union of member boxes and honours minimums', () => {
    expect(computeGraphFrameSize([])).toEqual({height: GRAPH_FRAME_MIN_HEIGHT, width: GRAPH_FRAME_MIN_WIDTH});
    expect(computeGraphFrameSize([{height: 100, name: 'a', width: 200, x: 500, y: 300}])).toEqual({
        height: 300 + 100 + GRAPH_FRAME_HEADER_HEIGHT + GRAPH_FRAME_PADDING,
        width: 500 + 200 + GRAPH_FRAME_PADDING,
    });
});

it('findFreeSpot places to the right of the rightmost member', () => {
    expect(findFreeSpot([{height: 100, name: 'a', width: 200, x: 0, y: 0}])).toEqual({x: 200 + 60, y: 0});
});

it('autoPlaceGraphMembers lays a chain out left-to-right below pinned members', async () => {
    const positions = await autoPlaceGraphMembers(
        [{height: 100, name: 'a', width: 200}, {height: 100, name: 'b', width: 200}],
        [{from: 'a', to: 'b'}],
        [{height: 100, name: 'p', width: 200, x: 0, y: 0}]
    );

    expect(positions.a.x).toBeLessThan(positions.b.x);
    expect(positions.a.y).toBeGreaterThanOrEqual(100 + GRAPH_FRAME_PADDING);
});
```

- [ ] **Step 2: Run** → FAIL (modules missing).

- [ ] **Step 3: Implement** the three modules per the interfaces. `computeGraphFrameSize`: `width = max(MIN, maxRight + PADDING)`, `height = max(MIN, HEADER + maxBottom + PADDING)`; member coordinates are relative to the content origin `(0, HEADER)`, so the frame node's children get `position = {x, y: y + GRAPH_FRAME_HEADER_HEIGHT}` — encapsulate that in `toFrameChildPosition(position)` / `fromFrameChildPosition(position)` exported helpers. `autoPlaceGraphMembers`: build an elk graph `{id:'graph', layoutOptions:{'elk.algorithm':'layered','elk.direction':'RIGHT','elk.spacing.nodeNode':'40','elk.layered.spacing.nodeNodeBetweenLayers':'80'}, children: sizes, edges: static transitions only (skip dynamic `to` and edges to unknown names)}`; import ELK the same way `elkLayoutUtils.ts` does (`import ELK from 'elkjs/lib/elk.bundled.js'` or the worker path used there — copy its import); offset all results by `{x: 0, y: pinnedBottom + PADDING}` where `pinnedBottom = max(y + height)` over pinned (0 if none). `saveGraphParameters`: parse `useWorkflowDataStore.getState().workflow.definition`, find the graph task via `TASK_DISPATCHER_CONFIG.graph.getTask({taskDispatcherId: graphId, tasks})`, apply `updater` to its `parameters`, `getRecursivelyUpdatedTasks(tasks, updatedGraphTask)`, then `saveWorkflowDefinition({updateWorkflowMutation, updatedWorkflowTasks})`.

- [ ] **Step 4: Run** → PASS. **Commit** — `732 client - Add pure graph transition mutations and frame geometry helpers`.

---

### Task 3: Frame, start and member node construction (`createGraphNode`) and edges (`createGraphEdges`)

**Files:**
- Rewrite: `$EDITOR/utils/createGraphNode.ts` (+ new `.test.ts`)
- Rewrite: `$EDITOR/utils/createGraphEdges.ts` (+ rewrite `createGraphEdges.test.ts` if present, else create)
- Modify: `client/src/shared/constants.tsx` — add `GRAPH_FRAME_NODE_TYPE = 'graphFrame'`, `GRAPH_START_NODE_TYPE = 'graphStart'`, `GRAPH_TRANSITION_EDGE_TYPE = 'graphTransition'`, `GRAPH_START_EDGE_TYPE = 'graphStart'`
- Modify: `client/src/shared/types.ts` — `NodeDataType` gains `graphFrame?: {graphId: string; height: number; width: number}`, `graphStart?: {graphId: string}`

**Interfaces (produced):**
- Ids: frame `${graphId}-graph-frame`, start `${graphId}-graph-start`, add placeholder `${graphId}-graph-placeholder`, top/bottom ghosts unchanged (`-graph-top-ghost` / `-graph-bottom-ghost`).
- `createGraphNode({allNodes, graphId, isNested})` splices, right after the graph dispatcher node: top ghost, `graphFrame` node (`data: {graphFrame: {graphId, height: GRAPH_FRAME_MIN_HEIGHT, width: GRAPH_FRAME_MIN_WIDTH}, graphId, taskDispatcherId: graphId}`, `draggable: false`, `selectable: false`, `width/height` set), the `graphStart` node (`parentId: frameId`, `position: {x: GRAPH_FRAME_PADDING, y: GRAPH_FRAME_HEADER_HEIGHT}`, `draggable: false`), the add placeholder (`type: 'placeholder'`, `parentId: frameId`, `data: {graphId, label: '+', taskDispatcherId: graphId}`, positioned in the header at top-right by the frame component — give it `position: {x: 0, y: 0}` and `hidden: true`; the frame renders its own "Add node" button that opens the popover with `sourceNodeId = placeholderId`), bottom ghost. Member task nodes are NOT touched here (they already sit in `allNodes` from `useLayout` with `graphData`); the pre-pass (Task 5) assigns their `parentId`/positions.
- `createGraphEdges(graphNode)` returns: `graph → topGhost` (type `workflow`, unchanged), `topGhost → frame` (type `smoothstep`, target handle `${frameId}-top`), `frame → bottomGhost` (source `${frameId}-bottom`), `graphStart → startNodeName` (type `graphStart`, `sourceHandle: ${startId}-source`, `targetHandle: ${startNodeName}-graph-transition-target`), and one `graphTransition` edge per `parameters.transitions[i]`: `id: ${graphId}-transition-${i}`, `source: from`, `target: isDynamic ? from : to`, `sourceHandle: ${from}-graph-transition-source`, `targetHandle: isDynamic ? ${from}-graph-transition-dynamic : ${to}-graph-transition-target`, `data: {condition, dynamic, graphId, index: i, to}`, `markerEnd: {type: MarkerType.ArrowClosed}`, `zIndex: 5`. Edges whose `from`/`to` name no static member are still emitted with `data.dangling: true` (rendered warning-styled).

- [ ] **Step 1: Write failing tests** (`createGraphNode.test.ts`, `createGraphEdges.test.ts`) asserting the ids/types/parentIds above for a graph with nodes `[a, b]`, `startNode: 'b'`, transitions `[a->b, b->=expr]`.
- [ ] **Step 2: Run** → FAIL. **Step 3: Implement.** **Step 4: Run** → PASS.
- [ ] **Step 5: Commit** — `732 client - Build the graph frame, start pill and transition edges from the edge list`.

---

### Task 4: Node and edge components

**Files:**
- Create: `$EDITOR/nodes/GraphFrameNode.tsx` (+ test), `$EDITOR/nodes/GraphStartNode.tsx`
- Rewrite: `$EDITOR/edges/GraphTransitionEdge.tsx` (+ test), create `$EDITOR/edges/GraphStartEdge.tsx`
- Delete: `$EDITOR/edges/computeGraphTransitionEdgePath.ts` + test (self-loop lobe: port the `self` branch into a small `computeGraphSelfLoopPath.ts` + test)
- Modify: `$EDITOR/nodes/WorkflowNode.tsx:481-502` (transition handles), `$EDITOR/nodes/AiAgentNode.tsx` (same handles), `$EDITOR/nodes/ReadOnlyNode.tsx` (same handles, not connectable), `$EDITOR/hooks/useWorkflowEditorCanvas.ts:132-146` (register `graphFrame`, `graphStart` node types; `graphStart` edge type; drop `labeledGraphNode`)
- Modify: `$EDITOR/nodes/NodeTypes.module.css` if a `handleConnectable` style is needed

**Interfaces:**
- Handles on member nodes (rendered only when `data.graphData` is set): target `id=${id}-graph-transition-target` on the cross-axis start side (`Position.Left` in TB, `Position.Top` in LR), source `id=${id}-graph-transition-source` on the cross-axis end side (`Position.Right` in TB, `Position.Bottom` in LR), `isConnectable={!readOnly}` — pass `readOnly` via `data.readOnly` set in `useLayout`'s read-only conversion; plus a hidden target `id=${id}-graph-transition-dynamic` at the source side used as the dynamic stub's anchor.
- `GraphFrameNode` renders a `div` with `style={{height: data.graphFrame.height, width: data.graphFrame.width}}`, rounded border, header row: label "Graph", right side `Auto-arrange` (`Wand2Icon`) and `Add node` (`PlusIcon`) buttons. `Add node` toggles a local `open` state passed to a `WorkflowNodesPopoverMenu` with `sourceNodeId={`${graphId}-graph-placeholder`}`, `hideTriggerComponents`. Two handles: `type="target" id=${id}-top position=Top` and `type="source" id=${id}-bottom position=Bottom` (mapped for LR via `mapHandlePosition`). Every control carries `nodrag`.
- `GraphStartNode`: pill "Start", one source handle `id=${id}-source` on the cross-axis end side, `nodrag`.
- `GraphTransitionEdge`: uses `getSmoothStepPath` (self-loop → `computeGraphSelfLoopPath`); `strokeDasharray` when `data.dynamic`; warning color when `data.dangling`; `EdgeLabelRenderer` label showing `data.condition` (or "dynamic: <to>") when hovered/selected; when `selected` and not read-only renders `GraphTransitionPopover` (Task 8) anchored at the label position. `markerEnd` arrow.
- `GraphStartEdge`: plain smoothstep, `reconnectable` handled in Task 7.

- [ ] **Step 1: Write tests** — `GraphFrameNode.test.tsx` renders with `data.graphFrame = {graphId:'graph_1', height: 300, width: 400}` and asserts the box has those inline sizes and both buttons exist; `GraphTransitionEdge.test.tsx` renders inside `ReactFlowProvider` with `data.condition='=x > 1'` and asserts the label text appears on hover, and `stroke-dasharray` is set when `data.dynamic`.
- [ ] **Step 2: Run** → FAIL. **Step 3: Implement.** **Step 4: Run** → PASS.
- [ ] **Step 5: Commit** — `732 client - Add graph frame, start pill and transition edge components`.

---

### Task 5: Layout pre-pass — `layoutGraphFrames`

**Files:**
- Create: `$EDITOR/utils/graph/layoutGraphFrames.ts` + `.test.ts`
- Modify: `$EDITOR/hooks/useLayout.tsx:1065-1073` (wrap the engine call), `:120-160` (fingerprint), `:461-467` (unchanged call), `:590-598` (drop `orderLanesByVisualPosition`), `:927-961` (read-only: keep `graphTransition`/`graphStart` edges, mark `data.readOnly`)
- Modify: `$EDITOR/utils/layoutUtils.tsx:184-222` (`getDagreNodeSize` `graphFrame` arm → `{height: data.graphFrame.height, width: data.graphFrame.width}`), `:851-870` (`filterAndDedupeLayoutEdges` — no `graphTransition` special case), delete `realignGraphLaneHandlesToDeclarationOrder` (`:972-990`) and its call, `:1007` (drop the transition filter)
- Modify: `$EDITOR/utils/elkLayoutUtils.ts:496-534` (`getElkNodeSize` `graphFrame` arm), `:760-859` (`getMemberCaseRank` — delete the graph branch), `:700-702` (drop transition filter), `:2158-2161` (`applySavedPositions` skips nodes with `parentId`)
- Modify: `$EDITOR/utils/postDagreConstraints.ts:1805-1930` (`applySavedPositions` skips `parentId` nodes)
- Delete: lane-related dead code flagged by ESLint/ts after the above (`distributeGraphNodeIndexes`, `getGraphNodeSide` graph arms in `createEdgeFromTaskDispatcherBottomGhostNode` if unused)

**Interfaces (produced):**
```ts
export interface LayoutGraphFramesResultI {
    // outer arrays with member/transition nodes+edges removed and frame nodes sized
    outerEdges: Edge[];
    outerNodes: Node[];
    // members (parentId set, frame-relative positions), in parent-first order, to append after the outer layout
    memberNodes: Node[];
    memberEdges: Edge[];
    // members that had no saved position and were auto-placed: name -> position (frame-relative, content-origin)
    autoPlaced: Record<string, Record<string, {x: number; y: number}>>; // graphId -> name -> position
}
export async function layoutGraphFrames(
    nodes: Node[], edges: Edge[], direction: LayoutDirectionType,
    layoutFunction: (props: GetLayoutElementsProps) => Promise<LayoutElementsResultI>
): Promise<LayoutGraphFramesResultI>;
export function getGraphMemberOwner(node: Node, nodes: Node[]): string | undefined; // graphId owning this node (member task or any node of a member's subtree), else undefined
```
Algorithm (spec "Layout pipeline"): find frames; order post-order (a frame whose graph dispatcher node is itself owned by another graph comes first); for each frame: group owned nodes by top-level member name (walk `graphData.graphId === graphId` for direct members; other owned nodes are those whose owning-dispatcher chain — `getOwningDispatcherId` from `elkLayoutUtils`, exported — reaches a direct member); for each group run `layoutFunction({canvasWidth: 0, direction, edges: groupEdges, nodes: groupNodes, savedPositionCrossAxisShift: 0})`, normalise the result to its bbox min, take size; member position = `member.data.metadata?.ui?.nodePosition` else from `autoPlaceGraphMembers` (record in `autoPlaced`); set `parentId = frameId`, `position = toFrameChildPosition(memberPos) + (nodeAbs − bboxMin)`, `draggable: true` on the direct member node only, `extent: [[0, GRAPH_FRAME_HEADER_HEIGHT], [Infinity, Infinity]]` on the direct member; frame node gets `data.graphFrame.{width,height}` and `width/height` from `computeGraphFrameSize`; remove owned nodes and their internal edges plus `graphTransition`/`graphStart` edges from the outer arrays.

`useLayout` wiring:
```ts
const layoutFunction = isElkLayoutActive(layoutEngine, layoutNodes) ? getElkLayoutElements : getLayoutElements;

layoutGraphFrames(layoutNodes, edges, layoutDirection, layoutFunction)
    .then((framed) =>
        layoutFunction({...props, edges: framed.outerEdges, nodes: framed.outerNodes}).then((elements) => ({
            ...elements,
            edges: [...elements.edges, ...framed.memberEdges],
            nodes: [...elements.nodes, ...framed.memberNodes],
            autoPlaced: framed.autoPlaced,
        }))
    )
    .then((elements) => { /* existing body */ });
```
Persist `autoPlaced` lazily: store it in a `useRef` (`autoPlacedGraphPositionsRef`) that Task 7's first interaction with that graph flushes through `saveWorkflowNodesPosition` (spec: written back on first user interaction).

Fingerprint: replace `collectGraphNextExpressions` with `collectGraphLayoutSignature(parameters, sink)` pushing, per graph, `transitions:<from>-><to>[<condition>];…|positions:<name>@x,y;…`.

- [ ] **Step 1: Write failing tests** — `layoutGraphFrames.test.ts` with a stub `layoutFunction` that stacks nodes vertically 100px apart: (a) a graph with two plain members with saved positions → members get `parentId`, frame width/height per `computeGraphFrameSize`, outer arrays exclude members; (b) a member without position lands in `autoPlaced`; (c) a graph nested inside a graph member: inner frame processed first and appears as a leaf inside the outer member group; (d) transition edges are moved to `memberEdges`.
- [ ] **Step 2: Run** → FAIL. **Step 3: Implement** the module and the wiring/size arms/skips listed above. **Step 4: Run** `npx vitest run src/pages/platform/workflow-editor` and `npm run typecheck` → PASS (fix the existing `elkLayoutUtils.test.ts` / `layoutUtils.test.tsx` graph-lane expectations by deleting the lane cases).
- [ ] **Step 5: Manual check** — start the dev server (`npm run dev`, server on 9555 running), open a workflow with a graph of three nodes with positions, switch between engines: box renders identically, outer chain flows around it.
- [ ] **Step 6: Commit** — `732 client - Lay graph members out inside a sized frame and reflow the outer flow around it`.

---

### Task 6: Dragging members — live frame resize, persist on drop, always draggable

**Files:**
- Modify: `$EDITOR/hooks/useWorkflowEditorCanvas.ts:315-354` (drag start: detect member), `:356-449` (`handleNodesChange`: live frame resize), `:451-531` (`handleNodeDragStop`: persist frame-relative, flush auto-placed)
- Modify: `$EDITOR/components/WorkflowEditor.tsx:94` (`nodesDraggable` stays; members override via node `draggable: true` — verify React Flow honours per-node `draggable` when the global flag is false; if not, set `nodesDraggable` to `!readOnlyWorkflow` and set `draggable: false` explicitly on every non-member node in `useLayout` when `nodesLocked`)
- Modify: `$EDITOR/nodes/WorkflowNode.tsx:619-620` (hide the unpin button when `data.graphData`)
- Test: `$EDITOR/hooks/tests/useWorkflowEditorCanvas.graphDrag.test.ts` (new)

**Interfaces:**
- Consumes `computeGraphFrameSize`, `fromFrameChildPosition`, `saveWorkflowNodesPosition`.
- Produces `resizeGraphFrameForMembers(nodes: Node[], graphId: string): Node[]` (pure, exported from `layoutGraphFrames.ts` or a sibling `graphFrameResize.ts`): recomputes the frame node's `data.graphFrame.{width,height}` + `width/height` from its direct members' `position` and `measured` (fallback `GRAPH_MEMBER_NOMINAL_SIZE`) sizes.

- [ ] **Step 1: Test** `resizeGraphFrameForMembers` grows the frame when a member sits past the current right edge, and never shrinks below the minimum.
- [ ] **Step 2: Implement**: in `handleNodesChange`, after applying changes, if the changed node has `parentId` ending in `-graph-frame` call `resizeGraphFrameForMembers` on the result. In `handleNodeDragStop`, when `draggedNode.parentId` is a graph frame: `nodePositions[draggedNode.id] = fromFrameChildPosition(clampNonNegative(draggedNode.position))`, merge any pending `autoPlaced` positions for that graph, call `saveWorkflowNodesPosition({draggedNodeId, nodePositions, updateWorkflowMutation})` and `return` before the cross-axis compensation branch. A dragged member that is a dispatcher still carries its subtree via the existing descendant delta code — assert in the test that descendants keep their relative offset.
- [ ] **Step 3: Run tests + manual drag** (box grows live, outer flow re-flows on drop, positions survive reload). **Commit** — `732 client - Drag graph members freely with live frame resize and persisted positions`.

---

### Task 7: Connecting, adding, start node, auto-arrange

**Files:**
- Modify: `$EDITOR/components/WorkflowEditor.tsx:85-107` — add `onConnect`, `onConnectEnd`, `onReconnect`, `isValidConnection`, `connectionMode={ConnectionMode.Strict}`
- Create: `$EDITOR/hooks/useGraphConnections.ts` (+ test) exporting `{handleConnect, handleConnectEnd, handleReconnect, isValidConnection}`
- Modify: `$EDITOR/stores/useWorkflowEditorStore.ts` — add `graphPendingConnection: {dropPosition: {x: number; y: number}; from: string; graphId: string} | undefined` + setter
- Modify: `$EDITOR/nodes/GraphFrameNode.tsx` — Add node opens popover; Auto-arrange calls `autoPlaceGraphMembers` for ALL members then `saveWorkflowNodesPosition`; listens to `graphPendingConnection` for its graph to open the popover
- Modify: `$EDITOR/utils/taskDispatcherConfig.tsx` graph `updateTaskParameters` — when appending a new task, stamp `metadata.ui.nodePosition` = pending drop position (from the store) or `findFreeSpot(existing positions)`, and if `graphPendingConnection.from` is set append `{from, to: newTask.name}` to `transitions` and clear the pending state
- Modify: `$EDITOR/hooks/useHandleDrop.tsx` — dropping a sidebar component onto a `graphFrame` node inserts into that graph at the drop position (reuse the placeholder path with `sourceNodeId = ${graphId}-graph-placeholder` after setting `graphPendingConnection = {dropPosition, from: '', graphId}` — treat empty `from` as "no edge")

**Interfaces:**
- `isValidConnection(connection)`: source and target nodes both have `data.graphData` with the same `graphId`, or source is the graph's `graphStart` node; `sourceHandle` ends with `-graph-transition-source` (or `-source` for start) and `targetHandle` ends with `-graph-transition-target`; source ≠ target allowed (self-loop).
- `handleConnect(connection)`: start pill → `saveGraphParameters(graphId, p => ({...p, startNode: target}))`; member → `saveGraphParameters(graphId, p => ({...p, transitions: addTransition(p.transitions ?? [], source, target)}))`.
- `handleConnectEnd(event, connectionState)`: if `!connectionState.isValid && connectionState.fromNode?.data.graphData` and the pointer is inside that graph's frame element (`document.elementFromPoint` → closest `[data-graph-frame-id]`), set `graphPendingConnection = {dropPosition: fromFrameChildPosition(screenToFlowPosition(pointer) − frameAbsolutePosition), from, graphId}` → the frame opens the popover.
- `handleReconnect(oldEdge, newConnection)`: only for `graphStart` edges → set `startNode`.

- [ ] **Step 1: Tests** for `isValidConnection` (cross-graph rejected, wrong handle rejected, self-loop accepted, start pill accepted) and for `useGraphConnections.handleConnect` calling `saveGraphParameters` with an appended transition (mock `saveGraphParameters` with `vi.hoisted`).
- [ ] **Step 2: Implement.** **Step 3: Manual**: drag from a node's right handle to another → edge appears and persists; drag to empty space in the box → popover, pick a component → new node at the drop point, connected; Add node → unconnected node at a free spot; drag the Start edge to another node → `startNode` updates; Auto-arrange → tidy left-to-right layout persisted.
- [ ] **Step 4: Commit** — `732 client - Connect graph nodes by dragging, add nodes into the box, set start node and auto-arrange`.

---

### Task 8: Transition editing — edge popover and Transitions panel

**Files:**
- Create: `$EDITOR/components/properties/graph/GraphTransitionPopover.tsx` (+ test)
- Create: `$EDITOR/components/properties/graph/GraphTransitionsPanel.tsx` (+ test)
- Modify: `$EDITOR/components/WorkflowNodeDetailsPanel.tsx:331` — render `GraphTransitionsPanel` for `componentName === 'graph'` where `GraphStatesPanel` used to be
- Modify: `$EDITOR/edges/GraphTransitionEdge.tsx` — render the popover when `selected && !readOnly`
- Modify: `$EDITOR/hooks/useWorkflowEditorCanvas.ts` — `deleteKeyCode` stays `null`; add an `onEdgesDelete`-equivalent: Backspace/Delete with a selected `graphTransition` edge removes it (register a keydown listener in `WorkflowEditor` guarded by `document.activeElement` not being an input)

**Interfaces:**
- `GraphTransitionPopover({graphId, index})`: reads the live graph task (`flattenDefinitionTasks` like the old `GraphStatesPanel:60-70`), shows `from → to`; a `to` field: `Select` of member names + "Expression…" that switches to a text input (dynamic); a `condition` field rendered through `<Property path={`transitions[${index}].condition`} …/>` using the sub-property from `taskDispatcherDefinition.properties` (find `transitions` → `items.properties.condition`) — the same reuse `GraphStatesPanel` did for `nodes[i].next` (`saveProperty` with `type: 'STRING'`); Delete button → `saveGraphParameters(graphId, p => ({...p, transitions: removeTransition(p.transitions, index)}))`.
- `GraphTransitionsPanel({graphId})`: groups `transitionsFrom` per member in declaration order; each row: `→ to` (or the expression), condition summary, up/down (`moveTransition`), delete; a warning row per name from `findNodesWithDuplicateDefault` reading "More than one unconditional transition — the first declared is taken"; nodes with zero outgoing edges labelled "terminal".

- [ ] **Step 1: Tests** — panel groups edges by source and shows the duplicate-default warning; popover delete calls `saveGraphParameters` with the edge removed.
- [ ] **Step 2: Implement.** **Step 3: Run tests + manual** (click an edge → popover; set condition; delete via key). **Commit** — `732 client - Edit graph transitions from the edge popover and the Transitions panel`.

---

### Task 9: Read-only / execution view

**Files:**
- Modify: `$EDITOR/hooks/useLayout.tsx:77-93` (`toReadOnlyLayoutEdges` keeps `graphTransition`/`graphStart` edges, still rewrites others to `smoothstep`), `:927-961` (set `data.readOnly = true` on converted nodes so member handles render non-connectable)
- Modify: `$EDITOR/nodes/ReadOnlyNode.tsx` — transition handles (Task 4) — done there; verify
- Modify: `$EDITOR/edges/getExecutedEdgeStatus.ts` (+ test) — `graphTransition` arm: executed when, in the graph's child task executions ordered by `startDate`, some execution of `to` immediately follows one of `from`; `useExecutedEdgeStatus.ts` passes the graph's executions
- Modify: `client/src/shared/components/workflow-executions/WorkflowExecutionsAccordionItem.tsx:50-60, 221-260` — one row per node visit (each child execution is a visit; keep the "N node visits" summary, drop the inner per-task list)

- [ ] **Step 1: Tests** — `getExecutedEdgeStatus` graph arm (a→b executed, b→c not); accordion renders "3 node visits" for three child executions.
- [ ] **Step 2: Implement.** **Step 3: Manual**: run a graph workflow, open the execution: box + members + edges shown, taken edges highlighted. **Commit** — `732 client - Show graph transitions and visited nodes in the execution view`.

---

### Task 10: Retire the lane machinery and finish

**Files:**
- Delete anything still unreferenced: `edges/GraphNodeLabel.tsx`, `edges/useGraphNodeLabel.ts`, `edges/LabeledGraphNodeEdge.tsx` (if not already), `utils/createGraphEdges.ts` lane helpers, `elkLayoutUtils.ts` graph-lane comments/constants, `WorkflowEdge.tsx` graph chip branch, `useLayout.tsx` `usesElkGraphTransitionOverlay`
- Modify: `client/src/shared/constants.tsx` — remove any lane-only constants
- Docs: `docs/content/docs/platform/automation/build/workflows/flow-controls.mdx` "In the editor" paragraph (if Plan A left it describing lanes, finalize here)
- Modify: `CLAUDE.md` — add a short "Graph dispatcher canvas" note under the workflow-editor conventions (frame-as-leaf pre-pass, positions frame-relative, `onConnect` scoped to graph handles) — keep it to ~8 lines

- [ ] **Step 1:** `cd client && npm run check` → clean (lint, typecheck, tests). `grep -rn "nodeIndex\|GraphNodeType\|extractNextTargets\|orderGraphNodeIndexes\|labeledGraphNode" client/src` → no hits.
- [ ] **Step 2:** Manual regression sweep with both engines and TB/LR: loop/condition/fork-join unaffected; graph nested in a condition branch; condition nested as a graph member; graph inside graph; copy/paste a graph; undo after a drag; read-only sheet.
- [ ] **Step 3: Commit** — `732 client - Retire the graph lane rendering and document the free-form canvas`.

---

## Self-review

- Spec coverage: rendering (T3/T4), positions + persistence (T1 `nodes` arm, T5 auto-place, T6 persist), layout pipeline both engines + fingerprint (T5), drag/live resize/lock override (T6), connect + connect-to-empty + add node + drop from sidebar + start node + auto-arrange (T7), edge popover + panel + delete cascade (T8 + T1), read-only/execution + accordion (T9), retirement (T1/T5/T10), copy/paste remap (T1), duplicate-default warning (T2/T8). Nested graphs: T5 post-order.
- Placeholders: every task states files, interfaces, and the concrete behaviour/tests; where a step says "implement per the interfaces" the interfaces are fully typed in that task. Manual checkpoints are explicit.
- Type consistency: `GraphTransitionType` (T1) used by T2/T3/T7/T8; `computeGraphFrameSize`/`findFreeSpot`/`autoPlaceGraphMembers`/`toFrameChildPosition`/`fromFrameChildPosition` (T2) used by T5/T6/T7; `saveGraphParameters(graphId, updater, updateWorkflowMutation)` (T2) used by T7/T8; ids `${graphId}-graph-frame|-graph-start|-graph-placeholder` (T3) used by T4/T5/T7; handle ids `-graph-transition-source|-target|-dynamic` (T4) used by T3/T7; `graphPendingConnection` shape (T7) used by T7's config change and `useHandleDrop`.
