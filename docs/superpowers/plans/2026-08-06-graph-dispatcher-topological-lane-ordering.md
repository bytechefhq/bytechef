# Graph Dispatcher Topological Lane Ordering Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Order a `graph/v1` dispatcher's lanes by transition topology instead of declaration order, so a chain of `next` expressions reads left-to-right instead of as a tangle of back-arcs.

**Architecture:** A new pure util produces a declaration-index permutation from the statically resolvable `next` targets. Three consumers read it: ELK's per-container child sort (`getMemberCaseRank`) for lane x-order, `createGraphEdges` for left/middle/right handle groups, and `deriveGraphTransitionEdges` for each overlay arc's `forward`/`back` kind. Ordering is a presentation-time permutation only — no ids, no persisted structure, and no mutation path changes.

**Tech Stack:** TypeScript, React 19, ReactFlow (`@xyflow/react`), ELK.js, Vitest 4.

## Global Constraints

- Spec: `docs/superpowers/specs/2026-08-06-graph-dispatcher-topological-lane-ordering-design.md`.
- Lane ordering applies to the **ELK engine only**. Do not change dagre lane order (`layoutUtils.tsx`); dagre renders no transition arcs and keeps declaration order.
- Lane ids, placeholder ids, transition edge ids, and chip labels keep using **declaration indexes**. Never key an id off a visual position.
- Transitions stay a paint-time overlay: never fed into layout, never walked by chain traversal.
- A graph with no statically resolvable transitions must render **exactly** as it does today (identity permutation).
- Cycle break is **lowest declaration index**. `startNode` is preferred only among nodes that are already ready (in-degree zero) — it is not the cycle-break anchor.
- Client code style: no short/cryptic variable names; named imports sorted alphabetically within `{}`; interface names end in `I` or `Props`.
- Run `npm run check` (from `client/`) before any commit touching client code.
- Commit message format for client changes: `0 client - <description>`.

---

### Task 1: `orderGraphNodeIndexes` pure util

**Files:**
- Create: `client/src/pages/platform/workflow-editor/utils/orderGraphNodeIndexes.ts`
- Test: `client/src/pages/platform/workflow-editor/utils/orderGraphNodeIndexes.test.ts`

**Interfaces:**
- Consumes: `extractNextTargets(nextExpression, declaredNodeNames) => {dangling, dynamic, targets}` from `./extractNextTargets`; `GraphNodeType = {name: string; next?: string; tasks: Array<WorkflowTask>}` from `@/shared/types`.
- Produces: `export default function orderGraphNodeIndexes(nodes: Array<GraphNodeType>, startNode?: string): number[]` — a permutation of `0..nodes.length-1`. Tasks 2 and 3 both call this.

- [ ] **Step 1: Write the failing test**

Create `client/src/pages/platform/workflow-editor/utils/orderGraphNodeIndexes.test.ts`:

```ts
import {GraphNodeType} from '@/shared/types';
import {describe, expect, it} from 'vitest';

import orderGraphNodeIndexes from './orderGraphNodeIndexes';

function makeNodes(entries: Array<{name: string; next?: string}>): Array<GraphNodeType> {
    return entries.map((entry) => ({name: entry.name, next: entry.next, tasks: []}));
}

describe('orderGraphNodeIndexes', () => {
    it('should return an empty permutation for no nodes', () => {
        expect(orderGraphNodeIndexes([])).toEqual([]);
    });

    it('should keep declaration order when no node declares a transition', () => {
        const nodes = makeNodes([{name: 'node_0'}, {name: 'node_1'}, {name: 'node_2'}]);

        expect(orderGraphNodeIndexes(nodes)).toEqual([0, 1, 2]);
    });

    it('should order a reversed chain so it reads front to back', () => {
        const nodes = makeNodes([
            {name: 'node_c'},
            {name: 'node_b', next: "'node_c'"},
            {name: 'node_a', next: "'node_b'"},
        ]);

        expect(orderGraphNodeIndexes(nodes)).toEqual([2, 1, 0]);
    });

    it('should break a cycle at the lowest declaration index', () => {
        const nodes = makeNodes([
            {name: 'node_0', next: "'node_3'"},
            {name: 'node_1', next: "'node_0'"},
            {name: 'node_2', next: "'node_1'"},
            {name: 'node_3', next: "'node_2'"},
        ]);

        expect(orderGraphNodeIndexes(nodes)).toEqual([0, 3, 2, 1]);
    });

    it('should rank nodes with no statically resolvable transition last', () => {
        const nodes = makeNodes([
            {name: 'node_0', next: 'steps.decision.value'},
            {name: 'node_1', next: "'node_2'"},
            {name: 'node_2'},
        ]);

        expect(orderGraphNodeIndexes(nodes)).toEqual([1, 2, 0]);
    });

    it('should prefer the startNode among ready nodes', () => {
        const nodes = makeNodes([
            {name: 'node_a'},
            {name: 'node_b', next: "'node_a'"},
            {name: 'node_c', next: "'node_a'"},
        ]);

        expect(orderGraphNodeIndexes(nodes)).toEqual([1, 2, 0]);
        expect(orderGraphNodeIndexes(nodes, 'node_c')).toEqual([2, 1, 0]);
    });

    it('should ignore a startNode that does not resolve to a declared node', () => {
        const nodes = makeNodes([
            {name: 'node_a'},
            {name: 'node_b', next: "'node_a'"},
            {name: 'node_c', next: "'node_a'"},
        ]);

        expect(orderGraphNodeIndexes(nodes, 'node_missing')).toEqual([1, 2, 0]);
    });

    it('should treat a self-loop as no transition, ranking that node last', () => {
        const nodes = makeNodes([
            {name: 'node_0', next: "'node_0'"},
            {name: 'node_1', next: "'node_2'"},
            {name: 'node_2'},
        ]);

        expect(orderGraphNodeIndexes(nodes)).toEqual([1, 2, 0]);
    });

    it('should follow both branches of a ternary next expression', () => {
        const nodes = makeNodes([
            {name: 'node_end'},
            {name: 'node_start', next: "steps.check.ok ? 'node_mid' : 'node_end'"},
            {name: 'node_mid', next: "'node_end'"},
        ]);

        expect(orderGraphNodeIndexes(nodes)).toEqual([1, 2, 0]);
    });

    it('should return a permutation containing every declared index exactly once', () => {
        const nodes = makeNodes([
            {name: 'node_0', next: "'node_3'"},
            {name: 'node_1', next: "'node_0'"},
            {name: 'node_2'},
            {name: 'node_3', next: "'node_1'"},
            {name: 'node_4', next: 'dynamic.value'},
        ]);

        const ordered = orderGraphNodeIndexes(nodes);

        expect([...ordered].sort((first, second) => first - second)).toEqual([0, 1, 2, 3, 4]);
    });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd client && npx vitest run src/pages/platform/workflow-editor/utils/orderGraphNodeIndexes.test.ts`
Expected: FAIL — cannot resolve `./orderGraphNodeIndexes`.

- [ ] **Step 3: Write the implementation**

Create `client/src/pages/platform/workflow-editor/utils/orderGraphNodeIndexes.ts`:

```ts
import {GraphNodeType} from '@/shared/types';

import extractNextTargets from './extractNextTargets';

/**
 * Produces the VISUAL lane order for a `graph/v1` dispatcher as a permutation of declaration
 * indexes, so a chain of `next` expressions renders as adjacent left-to-right hops instead of
 * back-arcs over declaration-ordered lanes.
 *
 * Only statically resolvable targets constrain the order (`extractNextTargets`'s `targets`);
 * dangling literals, fully dynamic expressions, and self-loops do not — they stay badge/overlay
 * only, exactly as they are today. A node touched by no static transition in either direction
 * ranks after every connected node, so a graph with no transitions at all yields the identity
 * permutation and renders exactly as before.
 *
 * Ordering is a stable Kahn topological sort. Among nodes that are ready (in-degree zero),
 * `startNode` wins if it is one of them, otherwise the lowest declaration index does. When a cycle
 * leaves nothing ready, the lowest-declaration-index remaining node is emitted and its incoming
 * transitions become back-arcs — one deterministic rule, so the rendering never depends on
 * traversal accidents.
 *
 * This is presentation only: callers must keep using declaration indexes for ids.
 */
export default function orderGraphNodeIndexes(nodes: Array<GraphNodeType>, startNode?: string): number[] {
    if (!Array.isArray(nodes) || nodes.length === 0) {
        return [];
    }

    const declaredNodeNames = nodes.map((node) => node.name);
    const nodeIndexByName = new Map(declaredNodeNames.map((name, index) => [name, index]));

    const outgoingIndexes: Array<Set<number>> = nodes.map(() => new Set<number>());
    const incomingCounts: number[] = nodes.map(() => 0);
    const hasStaticTransition: boolean[] = nodes.map(() => false);

    nodes.forEach((node, sourceIndex) => {
        const {targets} = extractNextTargets(node.next, declaredNodeNames);

        targets.forEach((target) => {
            const targetIndex = nodeIndexByName.get(target);

            // A self-loop constrains nothing, and a duplicate target would double-count in-degree
            if (targetIndex === undefined || targetIndex === sourceIndex) {
                return;
            }

            if (outgoingIndexes[sourceIndex].has(targetIndex)) {
                return;
            }

            outgoingIndexes[sourceIndex].add(targetIndex);

            incomingCounts[targetIndex] += 1;
            hasStaticTransition[sourceIndex] = true;
            hasStaticTransition[targetIndex] = true;
        });
    });

    const startNodeIndex = startNode === undefined ? undefined : nodeIndexByName.get(startNode);

    const remainingIndexes = new Set<number>();

    nodes.forEach((_, index) => {
        if (hasStaticTransition[index]) {
            remainingIndexes.add(index);
        }
    });

    const orderedIndexes: number[] = [];

    while (remainingIndexes.size > 0) {
        const readyIndexes = Array.from(remainingIndexes).filter((index) => incomingCounts[index] === 0);

        let nextIndex: number;

        if (readyIndexes.length > 0) {
            nextIndex =
                startNodeIndex !== undefined && readyIndexes.includes(startNodeIndex)
                    ? startNodeIndex
                    : Math.min(...readyIndexes);
        } else {
            nextIndex = Math.min(...Array.from(remainingIndexes));
        }

        orderedIndexes.push(nextIndex);

        remainingIndexes.delete(nextIndex);

        outgoingIndexes[nextIndex].forEach((targetIndex) => {
            if (remainingIndexes.has(targetIndex)) {
                incomingCounts[targetIndex] -= 1;
            }
        });
    }

    nodes.forEach((_, index) => {
        if (!hasStaticTransition[index]) {
            orderedIndexes.push(index);
        }
    });

    return orderedIndexes;
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd client && npx vitest run src/pages/platform/workflow-editor/utils/orderGraphNodeIndexes.test.ts`
Expected: PASS, 10 tests.

- [ ] **Step 5: Commit**

```bash
cd /Volumes/Data/bytechef/bytechef
git add client/src/pages/platform/workflow-editor/utils/orderGraphNodeIndexes.ts \
        client/src/pages/platform/workflow-editor/utils/orderGraphNodeIndexes.test.ts
git commit -m "0 client - Add a topological lane ordering util for graph dispatchers"
```

---

### Task 2: Order ELK lanes by visual rank (mechanism checkpoint)

This is the load-bearing task. The previous attempt at this feature failed by building on an unverified assumption about which code decides lane order, so this task changes **only** the sort key and verifies it end-to-end before anything else is built on top.

**Files:**
- Modify: `client/src/pages/platform/workflow-editor/utils/elkLayoutUtils.ts` (the `scopeComponentName === 'graph'` branch of `getMemberCaseRank`, currently at line 688-695; add the memo helper just above `getMemberCaseRank` at line 643)
- Test: `client/src/pages/platform/workflow-editor/utils/elkLayoutUtils.test.ts` (add to the existing `describe('getElkLayoutElements with graph', ...)` block at line 2201)

**Interfaces:**
- Consumes: `orderGraphNodeIndexes(nodes, startNode?) => number[]` from Task 1.
- Produces: no new exported symbol. Lane x-order now follows the visual permutation.

- [ ] **Step 1: Write the failing test**

In `client/src/pages/platform/workflow-editor/utils/elkLayoutUtils.test.ts`, inside the existing `describe('getElkLayoutElements with graph', ...)` block, add a helper that attaches graph parameters to the dispatcher node and a new test. Add this immediately after the existing `graphChildTaskNode` helper (line 2243):

```ts
    const graphNodeWithParameters = (id: string, parameterNodes: Array<{name: string; next?: string}>): Node => ({
        data: {
            componentName: 'graph',
            parameters: {maxTransitions: 100, nodes: parameterNodes.map((node) => ({...node, tasks: []}))},
            taskDispatcher: true,
            taskDispatcherId: id,
            workflowNodeName: id,
        },
        id,
        position: {x: 0, y: 0},
        type: 'workflow',
    });

    it('orders graph node lanes by transition topology, keeping the trailing add-node placeholder last', async () => {
        // Declaration order is n0..n3, but the transitions form the chain
        // n0 -> n3 -> n2 -> n1 (with n1 -> n0 closing the cycle), so the lanes
        // should render in that chain order rather than n0, n1, n2, n3.
        const nodes: Node[] = [
            graphNodeWithParameters('graph_1', [
                {name: 'node_0', next: "'node_3'"},
                {name: 'node_1', next: "'node_0'"},
                {name: 'node_2', next: "'node_1'"},
                {name: 'node_3', next: "'node_2'"},
            ]),
            ...graphAuxNodes('graph_1'),
            graphNodePlaceholderNode('graph_1', 4),
            graphChildTaskNode('n0', 'graph_1', 0, 0),
            graphChildTaskNode('n1', 'graph_1', 1, 0),
            graphChildTaskNode('n2', 'graph_1', 2, 0),
            graphChildTaskNode('n3', 'graph_1', 3, 0),
        ];

        const edges: Edge[] = [
            edge('graph_1', 'graph_1-graph-top-ghost'),
            edge('graph_1-graph-top-ghost', 'n0'),
            edge('n0', 'graph_1-graph-bottom-ghost'),
            edge('graph_1-graph-top-ghost', 'n1'),
            edge('n1', 'graph_1-graph-bottom-ghost'),
            edge('graph_1-graph-top-ghost', 'n2'),
            edge('n2', 'graph_1-graph-bottom-ghost'),
            edge('graph_1-graph-top-ghost', 'n3'),
            edge('n3', 'graph_1-graph-bottom-ghost'),
            edge('graph_1-graph-top-ghost', 'graph_1-graph-node-4-placeholder-0'),
            edge('graph_1-graph-node-4-placeholder-0', 'graph_1-graph-bottom-ghost'),
        ];

        expect(collectScopeEdgeViolations(buildElkGraph(nodes, edges, 'TB'))).toEqual([]);

        const result = await getElkLayoutElements({canvasWidth: 2000, direction: 'TB', edges, nodes});

        const laneCenterOf = (id: string): number => positionOf(result.nodes, id).x + 36;

        expect(laneCenterOf('n0')).toBeLessThan(laneCenterOf('n3'));
        expect(laneCenterOf('n3')).toBeLessThan(laneCenterOf('n2'));
        expect(laneCenterOf('n2')).toBeLessThan(laneCenterOf('n1'));
        expect(laneCenterOf('n1')).toBeLessThan(laneCenterOf('graph_1-graph-node-4-placeholder-0'));
    });

    it('keeps declaration lane order when the graph declares no transitions', async () => {
        const nodes: Node[] = [
            graphNodeWithParameters('graph_1', [{name: 'node_0'}, {name: 'node_1'}, {name: 'node_2'}]),
            ...graphAuxNodes('graph_1'),
            graphNodePlaceholderNode('graph_1', 3),
            graphChildTaskNode('n0', 'graph_1', 0, 0),
            graphChildTaskNode('n1', 'graph_1', 1, 0),
            graphChildTaskNode('n2', 'graph_1', 2, 0),
        ];

        const edges: Edge[] = [
            edge('graph_1', 'graph_1-graph-top-ghost'),
            edge('graph_1-graph-top-ghost', 'n0'),
            edge('n0', 'graph_1-graph-bottom-ghost'),
            edge('graph_1-graph-top-ghost', 'n1'),
            edge('n1', 'graph_1-graph-bottom-ghost'),
            edge('graph_1-graph-top-ghost', 'n2'),
            edge('n2', 'graph_1-graph-bottom-ghost'),
            edge('graph_1-graph-top-ghost', 'graph_1-graph-node-3-placeholder-0'),
            edge('graph_1-graph-node-3-placeholder-0', 'graph_1-graph-bottom-ghost'),
        ];

        const result = await getElkLayoutElements({canvasWidth: 2000, direction: 'TB', edges, nodes});

        const laneCenterOf = (id: string): number => positionOf(result.nodes, id).x + 36;

        expect(laneCenterOf('n0')).toBeLessThan(laneCenterOf('n1'));
        expect(laneCenterOf('n1')).toBeLessThan(laneCenterOf('n2'));
        expect(laneCenterOf('n2')).toBeLessThan(laneCenterOf('graph_1-graph-node-3-placeholder-0'));
    });
```

- [ ] **Step 2: Run test to verify the topology test fails**

Run: `cd client && npx vitest run src/pages/platform/workflow-editor/utils/elkLayoutUtils.test.ts -t "orders graph node lanes by transition topology"`
Expected: FAIL — lanes still come out in declaration order, so `laneCenterOf('n0') < laneCenterOf('n3')` holds but `laneCenterOf('n3') < laneCenterOf('n2')` fails.

The second new test ("keeps declaration lane order") should already PASS — it pins the no-transition behavior so the next step cannot regress it. Confirm with:

Run: `cd client && npx vitest run src/pages/platform/workflow-editor/utils/elkLayoutUtils.test.ts -t "keeps declaration lane order"`
Expected: PASS.

- [ ] **Step 3: Add the import and the memo helper**

In `client/src/pages/platform/workflow-editor/utils/elkLayoutUtils.ts`, add the import alongside the other local util imports (keep import order as the linter expects):

```ts
import orderGraphNodeIndexes from './orderGraphNodeIndexes';
```

Then, immediately BEFORE the `const getMemberCaseRank = ...` declaration (line 643), add:

```ts
    // orderGraphNodeIndexes is pure but runs a topological sort, and getMemberCaseRank is called
    // once per container member — memoize per dispatcher so an n-lane graph sorts once, not n+1
    // times. Scoped to this buildElkGraph call, so it never outlives one layout pass.
    const graphVisualOrderByDispatcherId = new Map<string, number[]>();

    const getGraphVisualOrder = (dispatcherNode: Node): number[] => {
        const cachedOrder = graphVisualOrderByDispatcherId.get(dispatcherNode.id);

        if (cachedOrder) {
            return cachedOrder;
        }

        const dispatcherData = dispatcherNode.data as NodeDataType;
        const graphNodes = (dispatcherData.parameters?.nodes ?? []) as Array<GraphNodeType>;
        const visualOrder = orderGraphNodeIndexes(
            graphNodes,
            dispatcherData.parameters?.startNode as string | undefined
        );

        graphVisualOrderByDispatcherId.set(dispatcherNode.id, visualOrder);

        return visualOrder;
    };
```

If `GraphNodeType` is not already imported in this file, add it to the existing `@/shared/types` import.

- [ ] **Step 4: Replace the graph branch of `getMemberCaseRank`**

Replace lines 688-695 (the whole `if (scopeComponentName === 'graph') { ... }` block) with:

```ts
        if (scopeComponentName === 'graph') {
            // Both children (graphData) and placeholders (top-level field) carry an explicit
            // declaration nodeIndex. Lanes rank by their VISUAL position so the canvas reads in
            // transition order; the trailing add-node placeholder carries nodes.length, which is
            // outside the permutation and so keeps ranking last. With no node metadata at all the
            // permutation is empty and every member falls back to its declaration index, which is
            // exactly the pre-topological behaviour.
            const declaredNodeIndex = memberData.graphData?.nodeIndex ?? memberData.nodeIndex;

            if (declaredNodeIndex === undefined) {
                return Number.MAX_SAFE_INTEGER;
            }

            if (!scopeDispatcherNode) {
                return declaredNodeIndex;
            }

            const visualOrder = getGraphVisualOrder(scopeDispatcherNode);

            if (declaredNodeIndex >= visualOrder.length) {
                return declaredNodeIndex;
            }

            const visualPosition = visualOrder.indexOf(declaredNodeIndex);

            return visualPosition === -1 ? declaredNodeIndex : visualPosition;
        }
```

- [ ] **Step 5: Run the full ELK test file**

Run: `cd client && npx vitest run src/pages/platform/workflow-editor/utils/elkLayoutUtils.test.ts`
Expected: PASS, including the pre-existing test `orders graph node lanes by declared nodeIndex, keeping the empty lane in place and the trailing add-node placeholder last` (line 2245) — its dispatcher node carries no `parameters`, so the permutation is empty and every member falls back to its declaration index.

If that pre-existing test fails, the fallback in Step 4 is wrong — fix the fallback, do not change the pre-existing test.

- [ ] **Step 6: Verify the mechanism on the real canvas**

This is the checkpoint the whole plan rests on. Do not proceed to Task 3 until it passes.

1. Ensure the backend is running on port 9555 and the client dev server is running.
2. Open the `graph-topo-repro` workflow: project 1081, project-workflow 3001 (login `admin@localhost.com` / `admin`).
3. Confirm the experimental (ELK) layout engine is active — the toggle button in the canvas is labelled "Switch to standard layout engine" when ELK is active.
4. Read the lane headers left to right.

Expected: `node_0`, `node_3`, `node_2`, `node_1`, `node_4` — three adjacent forward arcs plus one return arc from `node_1` back to `node_0`.
Before this change it read `node_0`, `node_1`, `node_2`, `node_3`, `node_4` with three back-arcs.

Capture a screenshot for comparison against `1-elk-today-tangle.png`.

- [ ] **Step 7: Run the full client check**

Run: `cd client && npm run check`
Expected: PASS (lint, typecheck, tests).

- [ ] **Step 8: Commit**

```bash
cd /Volumes/Data/bytechef/bytechef
git add client/src/pages/platform/workflow-editor/utils/elkLayoutUtils.ts \
        client/src/pages/platform/workflow-editor/utils/elkLayoutUtils.test.ts
git commit -m "0 client - Order graph dispatcher lanes by transition topology"
```

---

### Task 3: Align arc kinds and nested-dispatcher sides with the visual order

With lanes reordered, the left/middle/right handle assignment and each arc's `forward`/`back` classification still read declaration indexes, so they now disagree with what is on screen. This task brings the remaining two into agreement.

**Revised 2026-08-06, after Task 2's fix round.** Task 2's canvas checkpoint found an empty column mid-container. It was not an extra node: `distributeGraphNodeIndexes` still grouped each lane's shared ghost handle by declaration index while ELK placed lanes in visual order, so a lane's edge left the wrong shared handle and jogged across the frame in a staircase that reads as an empty box. Fixing it required the handle-group half of this task, so that part is already done — in commit `46f88a58187`, via an `orderLanesByVisualPosition` option on `createGraphEdges` gated by `isElkLayoutActive`.

That gating is a **correction to this plan, not just an early delivery**. This task originally said to split the ordered array unconditionally, but `createGraphEdges` runs for both engines, so an ungated change would have reordered dagre's handle sides and violated the ELK-only constraint.

The same trap applies to what remains:

- `deriveGraphTransitionEdges` — safe to change unconditionally. Transition edges are already emitted only under `includeTransitionEdges`, which is ELK-gated, so dagre never sees them.
- `getGraphNodeSide` — **must be gated.** It is called by `createEdgeFromTaskDispatcherBottomGhostNode` (`layoutUtils.tsx:989`), which `useLayout.tsx:609` calls on the SHARED edge path used by both engines. Changing it unconditionally would move dagre's nested-dispatcher target handles.

**Files:**
- Modify: `client/src/pages/platform/workflow-editor/utils/deriveGraphTransitionEdges.ts` (whole file, 63 lines)
- Modify: `client/src/pages/platform/workflow-editor/utils/createGraphEdges.ts` (`createGraphTransitionEdges` and `getGraphNodeSide`; `distributeGraphNodeIndexes` and `createGraphEdges` already carry the lane-order permutation from commit `46f88a58187` — read them before editing rather than assuming the line numbers below)
- Test: `client/src/pages/platform/workflow-editor/utils/deriveGraphTransitionEdges.test.ts`
- Test: `client/src/pages/platform/workflow-editor/tests/createGraphEdges.test.ts`

**Already done by commit `46f88a58187` — do NOT redo:** `distributeGraphNodeIndexes` taking the ordered lane array, the `orderLanesByVisualPosition` option on `createGraphEdges`, and its wiring in `useLayout.tsx`. Steps 5-7 below are superseded; only the transition-arc-kind and `getGraphNodeSide` work remains.

**Interfaces:**
- Consumes: `orderGraphNodeIndexes(nodes, startNode?) => number[]` from Task 1.
- Produces: `deriveGraphTransitionEdges(nodes: Array<GraphNodeType>, visualPositionByIndex?: Map<number, number>) => Array<GraphTransitionEdgeI>` — second parameter optional, identity when omitted. `GraphTransitionEdgeI` keeps its existing shape `{kind, sourceIndex, targetIndex}` with `sourceIndex`/`targetIndex` still being **declaration** indexes.

- [ ] **Step 1: Write the failing test for `deriveGraphTransitionEdges`**

Add to `client/src/pages/platform/workflow-editor/utils/deriveGraphTransitionEdges.test.ts`:

```ts
    it('should derive edge kind from visual positions when a permutation is supplied', () => {
        const nodes: Array<GraphNodeType> = [
            {name: 'node_0', next: "'node_3'", tasks: []},
            {name: 'node_1', next: "'node_0'", tasks: []},
            {name: 'node_2', next: "'node_1'", tasks: []},
            {name: 'node_3', next: "'node_2'", tasks: []},
        ];

        // Visual order is node_0, node_3, node_2, node_1
        const visualPositionByIndex = new Map([
            [0, 0],
            [3, 1],
            [2, 2],
            [1, 3],
        ]);

        const edges = deriveGraphTransitionEdges(nodes, visualPositionByIndex);

        const kindOf = (sourceIndex: number, targetIndex: number): string =>
            edges.find((edge) => edge.sourceIndex === sourceIndex && edge.targetIndex === targetIndex)!.kind;

        // Three adjacent forward hops along the visual order, one return arc
        expect(kindOf(0, 3)).toBe('forward');
        expect(kindOf(3, 2)).toBe('forward');
        expect(kindOf(2, 1)).toBe('forward');
        expect(kindOf(1, 0)).toBe('back');
    });

    it('should keep declaration-index kinds when no permutation is supplied', () => {
        const nodes: Array<GraphNodeType> = [
            {name: 'node_0', next: "'node_3'", tasks: []},
            {name: 'node_1', next: "'node_0'", tasks: []},
            {name: 'node_2', next: "'node_1'", tasks: []},
            {name: 'node_3', next: "'node_2'", tasks: []},
        ];

        const edges = deriveGraphTransitionEdges(nodes);

        const kindOf = (sourceIndex: number, targetIndex: number): string =>
            edges.find((edge) => edge.sourceIndex === sourceIndex && edge.targetIndex === targetIndex)!.kind;

        expect(kindOf(0, 3)).toBe('forward');
        expect(kindOf(3, 2)).toBe('back');
        expect(kindOf(2, 1)).toBe('back');
        expect(kindOf(1, 0)).toBe('back');
    });
```

Ensure `GraphNodeType` is imported in that test file.

- [ ] **Step 2: Run test to verify it fails**

Run: `cd client && npx vitest run src/pages/platform/workflow-editor/utils/deriveGraphTransitionEdges.test.ts`
Expected: FAIL on the first new test — `kindOf(3, 2)` is `'back'` because kind still comes from declaration indexes.

- [ ] **Step 3: Add the optional permutation parameter**

In `client/src/pages/platform/workflow-editor/utils/deriveGraphTransitionEdges.ts`, replace the exported function (lines 30-55) with:

```ts
export default function deriveGraphTransitionEdges(
    nodes: Array<GraphNodeType>,
    visualPositionByIndex?: Map<number, number>
): Array<GraphTransitionEdgeI> {
    const declaredNodeNames = nodes.map((node) => node.name);
    const nodeIndexByName = new Map(nodes.map((node, index) => [node.name, index]));

    const edges: Array<GraphTransitionEdgeI> = [];

    nodes.forEach((node, sourceIndex) => {
        const {targets} = extractNextTargets(node.next, declaredNodeNames);

        targets.forEach((target) => {
            const targetIndex = nodeIndexByName.get(target);

            if (targetIndex === undefined) {
                return;
            }

            // sourceIndex/targetIndex stay DECLARATION indexes (ids and lane lookups depend on
            // them); only the arc's kind is judged against where the lanes actually sit.
            const sourcePosition = visualPositionByIndex?.get(sourceIndex) ?? sourceIndex;
            const targetPosition = visualPositionByIndex?.get(targetIndex) ?? targetIndex;

            edges.push({
                kind: deriveTransitionKind(sourcePosition, targetPosition),
                sourceIndex,
                targetIndex,
            });
        });
    });

    return edges;
}
```

Also update the JSDoc paragraph above it that currently says `kind` is derived from "declaration order", to say it is derived from visual lane position, falling back to declaration order when no permutation is supplied.

- [ ] **Step 4: Run test to verify it passes**

Run: `cd client && npx vitest run src/pages/platform/workflow-editor/utils/deriveGraphTransitionEdges.test.ts`
Expected: PASS.

- [ ] **Step 5: Write the failing test for `createGraphEdges` handle groups**

Add to `client/src/pages/platform/workflow-editor/tests/createGraphEdges.test.ts`, inside the top-level `describe('createGraphEdges', ...)`:

```ts
    describe('topological lane ordering', () => {
        const chainNodes: Array<GraphNodeType> = [
            {name: 'node_0', next: "'node_3'", tasks: [{name: 'var_0', type: 'var/v1/set'}]},
            {name: 'node_1', next: "'node_0'", tasks: [{name: 'var_1', type: 'var/v1/set'}]},
            {name: 'node_2', next: "'node_1'", tasks: [{name: 'var_2', type: 'var/v1/set'}]},
            {name: 'node_3', next: "'node_2'", tasks: [{name: 'var_3', type: 'var/v1/set'}]},
        ];

        it('should assign handle sides by visual order, not declaration order', () => {
            const graphNode = makeGraphNode('graph_1', chainNodes);

            const edges = createGraphEdges(graphNode);

            const entryHandleOf = (taskName: string): string =>
                edges.find((edge) => edge.source === 'graph_1-graph-top-ghost' && edge.target === taskName)!
                    .sourceHandle as string;

            // Visual order is node_0, node_3, node_2, node_1. With four lanes plus the reserved
            // trailing placeholder slot the split is left [0, 3], middle 2, right [1] — so the
            // third lane in visual order takes the centre 'bottom' handle, not a side handle.
            expect(entryHandleOf('var_0')).toBe('graph_1-graph-top-ghost-left');
            expect(entryHandleOf('var_3')).toBe('graph_1-graph-top-ghost-left');
            expect(entryHandleOf('var_2')).toBe('graph_1-graph-top-ghost-bottom');
            expect(entryHandleOf('var_1')).toBe('graph_1-graph-top-ghost-right');
        });

        it('should classify transition arcs against the visual order', () => {
            const graphNode = makeGraphNode('graph_1', chainNodes);

            const edges = createGraphEdges(graphNode, {includeTransitionEdges: true});

            const kindOf = (sourceIndex: number, targetIndex: number): string =>
                edges.find((edge) => edge.id === `graph_1-transition-${sourceIndex}-${targetIndex}`)!.data!
                    .kind as string;

            expect(kindOf(0, 3)).toBe('forward');
            expect(kindOf(3, 2)).toBe('forward');
            expect(kindOf(2, 1)).toBe('forward');
            expect(kindOf(1, 0)).toBe('back');
        });
    });
```

- [ ] **Step 6: Run test to verify it fails**

Run: `cd client && npx vitest run src/pages/platform/workflow-editor/tests/createGraphEdges.test.ts -t "topological lane ordering"`
Expected: FAIL — handle sides and arc kinds still follow declaration order. In declaration order the split is left `[0, 1]`, middle `2`, right `[3]`, so `var_3` comes back as `-right` (expected `-left`) and `var_1` as `-left` (expected `-right`). `var_2` is `-bottom` under both orders, so that assertion passes either way.

- [ ] **Step 7: Thread the permutation through `createGraphEdges`**

In `client/src/pages/platform/workflow-editor/utils/createGraphEdges.ts`:

Add the import:

```ts
import orderGraphNodeIndexes from './orderGraphNodeIndexes';
```

Replace `distributeGraphNodeIndexes` (lines 213-238) with a version that takes the already-ordered indexes:

```ts
/**
 * Distributes graph node lanes into left, middle, and right handle groups (copied from
 * fork-join's distributeBranches). Takes the indexes in VISUAL order, so handle sides match the
 * lane order the layout engine produces. The `+1` mirrors fork-join: it reserves room in the
 * split for the always-present trailing add-node placeholder, which itself is excluded from the
 * distribution and rendered fixed to the right (see `createEdgesForTrailingPlaceholder`).
 */
function distributeGraphNodeIndexes(orderedNodeIndexes: number[]): {
    leftIndexes: number[];
    middleIndex: number | null;
    rightIndexes: number[];
} {
    const nodeCount = orderedNodeIndexes.length;
    const isEvenCount = (nodeCount + 1) % 2 === 0;

    if (isEvenCount) {
        const halfPoint = (nodeCount + 1) / 2;

        return {
            leftIndexes: orderedNodeIndexes.slice(0, halfPoint),
            middleIndex: null,
            rightIndexes: orderedNodeIndexes.slice(halfPoint),
        };
    } else {
        const middlePosition = Math.floor((nodeCount + 1) / 2);

        return {
            leftIndexes: orderedNodeIndexes.slice(0, middlePosition),
            middleIndex: orderedNodeIndexes[middlePosition] ?? null,
            rightIndexes: orderedNodeIndexes.slice(middlePosition + 1),
        };
    }
}
```

Change `createGraphTransitionEdges` (line 265) to accept and forward the permutation:

```ts
function createGraphTransitionEdges(
    graphId: string,
    nodes: Array<GraphNodeType>,
    visualPositionByIndex: Map<number, number>
): Edge[] {
    const transitions = deriveGraphTransitionEdges(nodes, visualPositionByIndex);
```

(The rest of that function body is unchanged.)

In `createGraphEdges` (line 315), compute the permutation once and use it for both:

```ts
    const orderedNodeIndexes = orderGraphNodeIndexes(nodes, nodeData.parameters?.startNode as string | undefined);
    const visualPositionByIndex = new Map(
        orderedNodeIndexes.map((declaredNodeIndex, visualPosition) => [declaredNodeIndex, visualPosition])
    );

    const {leftIndexes, middleIndex, rightIndexes} = distributeGraphNodeIndexes(orderedNodeIndexes);
```

replacing the existing line 330, and change the transition call at line 365 to:

```ts
        edges.push(...createGraphTransitionEdges(graphId, nodes, visualPositionByIndex));
```

- [ ] **Step 8: Resolve nested-dispatcher handle sides by visual position — ELK-gated**

`getGraphNodeSide` runs on the shared edge path (`createEdgeFromTaskDispatcherBottomGhostNode` → `useLayout.tsx:609`), so it must take the visual order as an OPTIONAL argument and keep declaration order when it is absent. Do not compute the permutation inside the function unconditionally — that would move dagre's handles.

Add a fourth parameter and use it only when supplied:

```ts
export function getGraphNodeSide(
    taskDispatcherId: string,
    tasks: WorkflowTask[],
    parentGraphId: string,
    // Supplied only on the ELK path (see orderLanesByVisualPosition in createGraphEdges). Absent
    // means dagre, which keeps declaration lane order, so sides must stay declaration-based.
    orderLanesByVisualPosition = false
): 'left' | 'right' | 'bottom' {
```

then, replacing the body from `const totalNodes = nodes.length;` through the end of the function:

```ts
    const totalNodes = nodes.length;

    if (totalNodes === 1) {
        return 'right';
    }

    let lanePosition = nodeIndex;

    if (orderLanesByVisualPosition) {
        const orderedNodeIndexes = orderGraphNodeIndexes(
            nodes,
            parentGraphTask.parameters?.startNode as string | undefined
        );
        const visualPosition = orderedNodeIndexes.indexOf(nodeIndex);

        lanePosition = visualPosition === -1 ? nodeIndex : visualPosition;
    }

    if (lanePosition === 0) {
        return 'left';
    } else if (lanePosition === totalNodes - 1) {
        return 'right';
    } else {
        return 'bottom';
    }
```

Then thread the flag from the one call site. In `layoutUtils.tsx`, `createEdgeFromTaskDispatcherBottomGhostNode` must accept and forward it, and `useLayout.tsx:609` must pass the same `isElkLayoutActive(...)` value it already computes for `orderLanesByVisualPosition`. Add a test asserting that WITHOUT the flag a permuted graph still returns declaration-order sides, so the dagre contract is pinned.

- [ ] **Step 9: Run both test files**

Run: `cd client && npx vitest run src/pages/platform/workflow-editor/tests/createGraphEdges.test.ts src/pages/platform/workflow-editor/utils/deriveGraphTransitionEdges.test.ts`
Expected: PASS, including all pre-existing tests in both files.

Pre-existing `createGraphEdges` tests use graphs with no `next` expressions, so the permutation is the identity and their handle-side expectations are unchanged. If one fails, the identity fallback is wrong — fix the code, not the test.

- [ ] **Step 10: Run the full client check**

Run: `cd client && npm run check`
Expected: PASS.

- [ ] **Step 11: Verify on the real canvas**

Reopen the `graph-topo-repro` workflow (project 1081, project-workflow 3001) with the ELK engine active.

Expected: lanes read `node_0`, `node_3`, `node_2`, `node_1`, `node_4`; the three chain arcs are short hops between adjacent lanes; one longer return arc runs from `node_1` back to `node_0`. No arc should skip more than one lane except that return arc.

Capture a screenshot.

- [ ] **Step 12: Commit**

```bash
cd /Volumes/Data/bytechef/bytechef
git add client/src/pages/platform/workflow-editor/utils/createGraphEdges.ts \
        client/src/pages/platform/workflow-editor/utils/deriveGraphTransitionEdges.ts \
        client/src/pages/platform/workflow-editor/utils/deriveGraphTransitionEdges.test.ts \
        client/src/pages/platform/workflow-editor/tests/createGraphEdges.test.ts
git commit -m "0 client - Align graph handle sides and transition arcs with visual lane order"
```

---

## Stage 1 complete — checkpoint before Stages 2 and 3

Tasks 1-3 deliver the whole of Stage 1 from the spec, and Stage 1 stands on its own: lanes read in transition order and every arc is classified against what is on screen.

Stages 2 and 3 are **deliberately not planned here**:

- **Stage 2** (moving the transition handles off the label band) is scoped only after Stage 1 is on screen. Three short adjacent hops may sit in the existing gap perfectly well, in which case the handle change is unnecessary; specifying it now would be guessing at both the need and the magnitude.
- **Stage 3** (the trailing add-node placeholder sitting on the container border) needs a root cause first. The placeholder is already a child of the graph frame (pinned by `elkLayoutUtils.test.ts:2277`), so the defect is in frame padding or in the placeholder column's vertical placement, not in membership — and which of those it is determines the fix.

At the checkpoint, compare the new screenshot against `1-elk-today-tangle.png` and `3-elk-zoom-label-collisions.png`, then plan Stages 2 and 3 with what the reordered canvas actually shows.
