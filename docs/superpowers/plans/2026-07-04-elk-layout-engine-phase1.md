# ELK Layout Engine Phase 1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a parallel elkjs-based layout engine to the workflow editor (toolbar switch button), supporting plain task chains and endlessly nested condition dispatchers with uniform spacing at every depth, in TB and LR modes.

**Architecture:** A new `getElkLayoutElements()` sits beside dagre's `getLayoutElements()` with the identical signature and return contract, selected by a persisted `useLayoutEngineStore` value at the single call site in `useLayout.tsx`. The ELK path builds a hierarchical graph — each condition becomes a synthetic compound "frame" node containing its ghosts, placeholders, and branch chains; the condition task node stays *outside* its frame as a sibling, so no ELK edge ever crosses a hierarchy boundary (default `SEPARATE_CHILDREN` handling, no ports). Node/edge creation upstream is untouched; ELK computes positions only.

**Tech Stack:** React 19 + TypeScript, `elkjs` (^0.11, dynamically imported like dagre), Zustand, Vitest, `@xyflow/react`.

**Spec:** `docs/superpowers/specs/2026-07-04-elk-layout-engine-phase1-design.md`

## Global Constraints

- All work is in `client/`; run all npm commands from `client/`.
- ESLint `sort-keys`: object literal keys MUST be in ascending alphabetical order (test data, props, mocks included). `--fix` does NOT fix this.
- Interface names end in `I` or `Props`; type aliases end in `Type`.
- Lucide icons imported with `Icon` suffix (`NetworkIcon`, not `Network`).
- Named imports sorted alphabetically inside `{}`.
- No `useRef` here, but any ref variable must end in `Ref`.
- Descriptive variable names — no `n`, `e`, `acc` abbreviations.
- Prefer `||` over `??` for JSX fallbacks; `twMerge` for conditional classes (never `cn()`).
- Spacing constant is `50` and node sizes come from existing constants (`NODE_WIDTH` 240, `NODE_HEIGHT` 100, `PLACEHOLDER_NODE_HEIGHT/WIDTH` 28) — no new spacing values.
- Commit message convention: `client - <description>` (client-side change, no ticket number).
- Before the final commit of the feature: `npm run check` must pass (lint + typecheck + tests).
- Run a single test file with: `npx vitest run <path relative to client/>`.

---

### Task 1: `isElkLayoutSupported` helper

**Files:**
- Create: `client/src/pages/platform/workflow-editor/utils/isElkLayoutSupported.ts`
- Test: `client/src/pages/platform/workflow-editor/utils/isElkLayoutSupported.test.ts`

**Interfaces:**
- Consumes: `NodeDataType` from `@/shared/types`, `Node` from `@xyflow/react`.
- Produces: `export default function isElkLayoutSupported(nodes: Node[]): boolean` — used by Task 6 (`useLayout`) and Task 7 (toolbar). Operates on ReactFlow **nodes** (not `WorkflowTask[]`) because nested dispatcher children are already flattened into the node array, so one flat scan covers arbitrary nesting.

- [ ] **Step 1: Write the failing test**

```ts
// client/src/pages/platform/workflow-editor/utils/isElkLayoutSupported.test.ts
import {Node} from '@xyflow/react';
import {describe, expect, it} from 'vitest';

import isElkLayoutSupported from './isElkLayoutSupported';

const taskNode = (id: string): Node => ({
    data: {componentName: 'mailchimp', workflowNodeName: id},
    id,
    position: {x: 0, y: 0},
    type: 'workflow',
});

const dispatcherNode = (id: string, componentName: string): Node => ({
    data: {componentName, taskDispatcher: true, taskDispatcherId: id, workflowNodeName: id},
    id,
    position: {x: 0, y: 0},
    type: 'workflow',
});

describe('isElkLayoutSupported', () => {
    it('supports plain task chains', () => {
        expect(isElkLayoutSupported([taskNode('task1'), taskNode('task2')])).toBe(true);
    });

    it('supports condition dispatchers, including nested ones', () => {
        const nodes = [
            taskNode('task1'),
            dispatcherNode('condition_1', 'condition'),
            dispatcherNode('condition_2', 'condition'),
            taskNode('task2'),
        ];

        expect(isElkLayoutSupported(nodes)).toBe(true);
    });

    it('rejects any non-condition dispatcher', () => {
        for (const componentName of ['branch', 'each', 'forkJoin', 'loop', 'map', 'parallel']) {
            expect(isElkLayoutSupported([taskNode('task1'), dispatcherNode('dispatcher_1', componentName)])).toBe(
                false
            );
        }
    });

    it('rejects AI agent cluster roots', () => {
        const clusterRootNode: Node = {
            data: {clusterRoot: true, componentName: 'aiAgent', workflowNodeName: 'aiAgent_1'},
            id: 'aiAgent_1',
            position: {x: 0, y: 0},
            type: 'clusterRoot',
        };

        expect(isElkLayoutSupported([taskNode('task1'), clusterRootNode])).toBe(false);
    });

    it('supports an empty node list', () => {
        expect(isElkLayoutSupported([])).toBe(true);
    });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npx vitest run src/pages/platform/workflow-editor/utils/isElkLayoutSupported.test.ts`
Expected: FAIL — cannot resolve `./isElkLayoutSupported`.

- [ ] **Step 3: Write the implementation**

```ts
// client/src/pages/platform/workflow-editor/utils/isElkLayoutSupported.ts
import {NodeDataType} from '@/shared/types';
import {Node} from '@xyflow/react';

/**
 * Phase 1 of the experimental ELK layout engine supports plain task nodes and the
 * condition task dispatcher only. Any other dispatcher (branch, each, fork-join,
 * loop, map, parallel, on-error) or an AI-agent cluster root makes the workflow
 * unsupported: layout falls back to dagre and the toolbar switch is disabled.
 *
 * Operates on ReactFlow nodes rather than workflow tasks because dispatcher
 * children are flattened into the node array, so a single scan covers nesting.
 */
export default function isElkLayoutSupported(nodes: Node[]): boolean {
    return nodes.every((node) => {
        if (node.type === 'clusterRoot') {
            return false;
        }

        const nodeData = node.data as NodeDataType;

        if (nodeData.taskDispatcher && nodeData.componentName !== 'condition') {
            return false;
        }

        return true;
    });
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npx vitest run src/pages/platform/workflow-editor/utils/isElkLayoutSupported.test.ts`
Expected: PASS (5 tests).

- [ ] **Step 5: Commit**

```bash
git add client/src/pages/platform/workflow-editor/utils/isElkLayoutSupported.ts client/src/pages/platform/workflow-editor/utils/isElkLayoutSupported.test.ts
git commit -m "client - Add ELK layout support detection helper"
```

---

### Task 2: `useLayoutEngineStore`

**Files:**
- Create: `client/src/pages/platform/workflow-editor/stores/useLayoutEngineStore.ts`
- Test: `client/src/pages/platform/workflow-editor/stores/tests/useLayoutEngineStore.test.ts`

**Interfaces:**
- Produces: default export `useLayoutEngineStore` (Zustand hook) with state `{layoutEngine: LayoutEngineType; setLayoutEngine: (layoutEngine: LayoutEngineType) => void}` and `export type LayoutEngineType = 'dagre' | 'elk'`. Used by Tasks 6 and 7. Persisted to localStorage key `bytechef.layout-engine`; global (not per-workflow) by design — it is a development comparison switch.

- [ ] **Step 1: Write the failing test**

```ts
// client/src/pages/platform/workflow-editor/stores/tests/useLayoutEngineStore.test.ts
import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it} from 'vitest';

import useLayoutEngineStore from '../useLayoutEngineStore';

describe('useLayoutEngineStore', () => {
    beforeEach(() => {
        useLayoutEngineStore.setState({layoutEngine: 'dagre'});
    });

    it('should initialize with the dagre engine', () => {
        const {result} = renderHook(() => useLayoutEngineStore());

        expect(result.current.layoutEngine).toBe('dagre');
    });

    it('should switch to the elk engine', () => {
        const {result} = renderHook(() => useLayoutEngineStore());

        act(() => {
            result.current.setLayoutEngine('elk');
        });

        expect(result.current.layoutEngine).toBe('elk');
    });

    it('should switch back to the dagre engine', () => {
        const {result} = renderHook(() => useLayoutEngineStore());

        act(() => {
            result.current.setLayoutEngine('elk');
        });

        act(() => {
            result.current.setLayoutEngine('dagre');
        });

        expect(result.current.layoutEngine).toBe('dagre');
    });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npx vitest run src/pages/platform/workflow-editor/stores/tests/useLayoutEngineStore.test.ts`
Expected: FAIL — cannot resolve `../useLayoutEngineStore`.

- [ ] **Step 3: Write the implementation**

```ts
// client/src/pages/platform/workflow-editor/stores/useLayoutEngineStore.ts
import {create} from 'zustand';
import {devtools, persist} from 'zustand/middleware';

export type LayoutEngineType = 'dagre' | 'elk';

interface LayoutEngineStateI {
    layoutEngine: LayoutEngineType;
    setLayoutEngine: (layoutEngine: LayoutEngineType) => void;
}

/**
 * Selects the workflow editor layout engine. 'elk' is the experimental engine
 * (phase 1: plain tasks + condition dispatchers only); 'dagre' is the default.
 * Global (not per-workflow) on purpose — this is a development comparison switch.
 */
const useLayoutEngineStore = create<LayoutEngineStateI>()(
    devtools(
        persist(
            (set) => ({
                layoutEngine: 'dagre',

                setLayoutEngine: (layoutEngine) => set({layoutEngine}),
            }),
            {
                name: 'bytechef.layout-engine',
            }
        )
    )
);

export default useLayoutEngineStore;
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npx vitest run src/pages/platform/workflow-editor/stores/tests/useLayoutEngineStore.test.ts`
Expected: PASS (3 tests).

- [ ] **Step 5: Commit**

```bash
git add client/src/pages/platform/workflow-editor/stores/useLayoutEngineStore.ts client/src/pages/platform/workflow-editor/stores/tests/useLayoutEngineStore.test.ts
git commit -m "client - Add layout engine store for dagre/elk switch"
```

---

### Task 3: Add elkjs dependency; extract shared edge filtering from `layoutUtils`

**Files:**
- Modify: `client/package.json` (add `elkjs`)
- Modify: `client/src/pages/platform/workflow-editor/utils/layoutUtils.tsx` (export `GetLayoutElementsProps`; extract `filterAndDedupeLayoutEdges`)

**Interfaces:**
- Produces:
  - `export interface GetLayoutElementsProps {canvasHeight?: number; canvasWidth: number; direction?: LayoutDirectionType; edges: Edge[]; nodes: Node[]; savedPositionCrossAxisShift?: number}` (currently a non-exported interface at layoutUtils.tsx:297 — just add `export`).
  - `export function filterAndDedupeLayoutEdges(allNodes: Node[], edges: Edge[]): Edge[]` — the edge sort/group/filter/dedupe/prune logic currently inlined at layoutUtils.tsx:831-919, behavior-identical. Used by Task 5.
- This is a behavior-preserving refactor: no new tests, existing suite must stay green.

- [ ] **Step 1: Install elkjs**

Run from `client/`:

```bash
npm install elkjs
```

Expected: `elkjs` (^0.11.x) added to `dependencies` in `client/package.json`.

- [ ] **Step 2: Export `GetLayoutElementsProps`**

In `client/src/pages/platform/workflow-editor/utils/layoutUtils.tsx` line 297, change:

```ts
interface GetLayoutElementsProps {
```

to:

```ts
export interface GetLayoutElementsProps {
```

- [ ] **Step 3: Extract `filterAndDedupeLayoutEdges`**

In `layoutUtils.tsx`, the tail of `getLayoutElements` (after the `centerDispatcherChildrenOnMainAxis` call, currently lines 831-919) builds `sourceEdgeMap`, sorts edges, filters to one-edge-per-source (with a multi-edge allowlist), dedupes by `source=>target-handles` key, and prunes edges referencing missing nodes. Move that entire block verbatim into a new exported function placed immediately above `getLayoutElements`:

```ts
/**
 * Engine-independent edge post-processing shared by the dagre and ELK layout
 * paths: prioritizes task edges over ghost/placeholder edges per source, keeps
 * a single edge per source unless the source legitimately fans out (ghosts,
 * cluster roots, branch, fork-join), dedupes by endpoint+handle key, and drops
 * edges referencing nodes that no longer exist.
 */
export function filterAndDedupeLayoutEdges(allNodes: Node[], edges: Edge[]): Edge[] {
    const sourceEdgeMap = new Map<string, Edge[]>();

    // Sort edges to prioritize task connections over ghost connections
    const sortedEdges = [...edges].sort((firstEdge, secondEdge) => {
        const isFirstEdgeToAuxiliaryNode =
            firstEdge.target.includes('ghost') || firstEdge.target.includes('placeholder');

        const isSecondEdgeToAuxiliaryNode =
            secondEdge.target.includes('ghost') || secondEdge.target.includes('placeholder');

        if (isFirstEdgeToAuxiliaryNode && !isSecondEdgeToAuxiliaryNode) {
            return 1;
        }

        if (!isFirstEdgeToAuxiliaryNode && isSecondEdgeToAuxiliaryNode) {
            return -1;
        }

        return 0;
    });

    // Group edges by source
    sortedEdges.forEach((edge) => {
        if (!sourceEdgeMap.has(edge.source)) {
            sourceEdgeMap.set(edge.source, []);
        }

        sourceEdgeMap.get(edge.source)?.push(edge);
    });

    const filteredEdges: Edge[] = [];

    // Filter edges so that only one edge is kept for each source node
    sourceEdgeMap.forEach((sourceEdges, source) => {
        const sourceNode = allNodes.find((node) => node.id === source);

        if (sourceEdges.length === 0 || !sourceNode) {
            return;
        }

        const multipleEdgesAllowed = [
            {
                condition: sourceNode.type === 'taskDispatcherTopGhostNode',
            },
            {
                condition: sourceNode.type === 'taskDispatcherBottomGhostNode',
            },
            {
                condition: sourceNode.data.clusterRoot,
            },
            {
                condition: sourceNode.data.componentName === 'branch',
            },
            {
                condition: sourceNode.data.componentName === 'fork-join',
            },
        ];

        if (multipleEdgesAllowed.some(({condition}) => condition)) {
            filteredEdges.push(...sourceEdges);
        } else {
            filteredEdges.push(sourceEdges[0]);
        }
    });

    const dedupedEdges = filteredEdges.reduce(
        (uniqueEdges: {edges: Edge[]; map: Map<string, boolean>}, edge: Edge) => {
            const {source, target} = edge;

            const targetHandle = edge.targetHandle ? `-${edge.targetHandle}` : '';
            const sourceHandle = edge.sourceHandle ? `-${edge.sourceHandle}` : '';

            const edgeKey = `${source}=>${target}${targetHandle}${sourceHandle}`;

            if (!uniqueEdges.map.has(edgeKey)) {
                uniqueEdges.map.set(edgeKey, true);

                uniqueEdges.edges.push(edge);
            }

            return uniqueEdges;
        },
        {edges: [], map: new Map<string, boolean>()}
    ).edges;

    // Remove edges that reference non-existent nodes
    const nodeIds = new Set(allNodes.map((node) => node.id));

    return dedupedEdges.filter((edge) => nodeIds.has(edge.source) && nodeIds.has(edge.target));
}
```

Then replace the moved block inside `getLayoutElements` with:

```ts
    edges = filterAndDedupeLayoutEdges(allNodes, edges);

    return {edges, nodes: allNodes};
```

- [ ] **Step 4: Verify no behavior change**

Run: `npx vitest run src/pages/platform/workflow-editor` and `npm run typecheck`
Expected: all existing workflow-editor tests PASS; typecheck clean.

- [ ] **Step 5: Commit**

```bash
git add client/package.json client/package-lock.json client/src/pages/platform/workflow-editor/utils/layoutUtils.tsx
git commit -m "client - Add elkjs dependency and extract shared layout edge filtering"
```

---

### Task 4: ELK hierarchy builder (`buildElkGraph`)

**Files:**
- Create: `client/src/pages/platform/workflow-editor/utils/elkLayoutUtils.ts`
- Test: `client/src/pages/platform/workflow-editor/utils/elkLayoutUtils.test.ts`

**Interfaces:**
- Consumes: `getDagreNodeSize(node, direction)` from `./layoutUtils` (already exported); `NodeDataType`; `LayoutDirectionType`.
- Produces (all used by Task 5):
  - `export function buildElkGraph(nodes: Node[], edges: Edge[], direction: LayoutDirectionType): ElkNode` — pure, synchronous, no elkjs import needed at runtime (types only).
  - `export function getFrameId(conditionId: string): string` — returns `` `${conditionId}__frame` ``.
  - `export const ELK_ROOT_ID = '__root__'`.
- Structure produced: every condition contributes a compound frame node (in the same scope as the condition task node) containing the condition's top ghost, bottom ghost, case placeholders, direct child tasks, and — recursively — nested condition nodes plus their frames. Edges are remapped so no edge crosses a hierarchy boundary: an endpoint inside a frame is represented by that frame at the deepest scope common to both endpoints.

- [ ] **Step 1: Write the failing tests**

```ts
// client/src/pages/platform/workflow-editor/utils/elkLayoutUtils.test.ts
import {Edge, Node} from '@xyflow/react';
import type {ElkNode} from 'elkjs/lib/elk-api';
import {describe, expect, it} from 'vitest';

import {buildElkGraph, getFrameId} from './elkLayoutUtils';

const taskNode = (id: string, conditionParent?: {conditionCase: 'caseTrue' | 'caseFalse'; conditionId: string}): Node => ({
    data: {
        componentName: 'mailchimp',
        ...(conditionParent
            ? {
                  conditionData: {
                      conditionCase: conditionParent.conditionCase,
                      conditionId: conditionParent.conditionId,
                      index: 0,
                  },
              }
            : {}),
        workflowNodeName: id,
    },
    id,
    position: {x: 0, y: 0},
    type: 'workflow',
});

const conditionNode = (
    id: string,
    conditionParent?: {conditionCase: 'caseTrue' | 'caseFalse'; conditionId: string}
): Node => ({
    data: {
        componentName: 'condition',
        ...(conditionParent
            ? {
                  conditionData: {
                      conditionCase: conditionParent.conditionCase,
                      conditionId: conditionParent.conditionId,
                      index: 0,
                  },
              }
            : {}),
        taskDispatcher: true,
        taskDispatcherId: id,
        workflowNodeName: id,
    },
    id,
    position: {x: 0, y: 0},
    type: 'workflow',
});

const conditionGhostNodes = (conditionId: string): Node[] => [
    {
        data: {conditionId, taskDispatcherId: conditionId},
        id: `${conditionId}-condition-top-ghost`,
        position: {x: 0, y: 0},
        type: 'taskDispatcherTopGhostNode',
    },
    {
        data: {conditionId, taskDispatcherId: conditionId},
        id: `${conditionId}-condition-bottom-ghost`,
        position: {x: 0, y: 0},
        type: 'taskDispatcherBottomGhostNode',
    },
];

const conditionPlaceholderNode = (conditionId: string, side: 'left' | 'right'): Node => ({
    data: {
        conditionCase: side === 'left' ? 'caseTrue' : 'caseFalse',
        conditionId,
        label: '+',
        taskDispatcherId: conditionId,
    },
    id: `${conditionId}-condition-${side}-placeholder-0`,
    position: {x: 0, y: 0},
    type: 'placeholder',
});

const edge = (source: string, target: string): Edge => ({id: `${source}=>${target}`, source, target});

const childIds = (elkNode: ElkNode | undefined): string[] =>
    (elkNode?.children ?? []).map((child) => child.id).sort();

const findChild = (elkNode: ElkNode, id: string): ElkNode | undefined =>
    (elkNode.children ?? []).find((child) => child.id === id);

const collectScopeEdgeViolations = (elkNode: ElkNode): string[] => {
    const violations: string[] = [];

    const memberIds = new Set((elkNode.children ?? []).map((child) => child.id));

    (elkNode.edges ?? []).forEach((scopeEdge) => {
        [...(scopeEdge.sources ?? []), ...(scopeEdge.targets ?? [])].forEach((endpointId) => {
            if (!memberIds.has(endpointId)) {
                violations.push(`${scopeEdge.id}: ${endpointId} not in scope ${elkNode.id}`);
            }
        });
    });

    (elkNode.children ?? []).forEach((child) => violations.push(...collectScopeEdgeViolations(child)));

    return violations;
};

const singleConditionFixture = () => {
    const nodes: Node[] = [
        taskNode('task1'),
        conditionNode('condition_1'),
        ...conditionGhostNodes('condition_1'),
        taskNode('childTrue1', {conditionCase: 'caseTrue', conditionId: 'condition_1'}),
        taskNode('childFalse1', {conditionCase: 'caseFalse', conditionId: 'condition_1'}),
        taskNode('task2'),
    ];

    const edges: Edge[] = [
        edge('task1', 'condition_1'),
        edge('condition_1', 'condition_1-condition-top-ghost'),
        edge('condition_1-condition-top-ghost', 'childTrue1'),
        edge('condition_1-condition-top-ghost', 'childFalse1'),
        edge('childTrue1', 'condition_1-condition-bottom-ghost'),
        edge('childFalse1', 'condition_1-condition-bottom-ghost'),
        edge('condition_1-condition-bottom-ghost', 'task2'),
    ];

    return {edges, nodes};
};

describe('buildElkGraph', () => {
    it('lays a linear chain flat in the root scope', () => {
        const nodes = [taskNode('task1'), taskNode('task2'), taskNode('task3')];
        const edges = [edge('task1', 'task2'), edge('task2', 'task3')];

        const graph = buildElkGraph(nodes, edges, 'TB');

        expect(childIds(graph)).toEqual(['task1', 'task2', 'task3']);
        expect(graph.edges).toHaveLength(2);
    });

    it('wraps condition members in a frame and keeps the condition node outside it', () => {
        const {edges, nodes} = singleConditionFixture();

        const graph = buildElkGraph(nodes, edges, 'TB');

        expect(childIds(graph)).toEqual(['condition_1', getFrameId('condition_1'), 'task1', 'task2']);

        const frame = findChild(graph, getFrameId('condition_1'));

        expect(childIds(frame)).toEqual([
            'childFalse1',
            'childTrue1',
            'condition_1-condition-bottom-ghost',
            'condition_1-condition-top-ghost',
        ]);
    });

    it('remaps root edges onto the frame', () => {
        const {edges, nodes} = singleConditionFixture();

        const graph = buildElkGraph(nodes, edges, 'TB');

        const rootEdgePairs = (graph.edges ?? []).map((scopeEdge) => `${scopeEdge.sources[0]}=>${scopeEdge.targets[0]}`);

        expect(rootEdgePairs.sort()).toEqual([
            `${getFrameId('condition_1')}=>task2`,
            `condition_1=>${getFrameId('condition_1')}`,
            'task1=>condition_1',
        ]);
    });

    it('produces no cross-hierarchy edges anywhere', () => {
        const {edges, nodes} = singleConditionFixture();

        expect(collectScopeEdgeViolations(buildElkGraph(nodes, edges, 'TB'))).toEqual([]);
    });

    it('places empty-branch placeholders inside the frame', () => {
        const nodes: Node[] = [
            conditionNode('condition_1'),
            ...conditionGhostNodes('condition_1'),
            conditionPlaceholderNode('condition_1', 'left'),
            conditionPlaceholderNode('condition_1', 'right'),
        ];

        const edges: Edge[] = [
            edge('condition_1', 'condition_1-condition-top-ghost'),
            edge('condition_1-condition-top-ghost', 'condition_1-condition-left-placeholder-0'),
            edge('condition_1-condition-top-ghost', 'condition_1-condition-right-placeholder-0'),
            edge('condition_1-condition-left-placeholder-0', 'condition_1-condition-bottom-ghost'),
            edge('condition_1-condition-right-placeholder-0', 'condition_1-condition-bottom-ghost'),
        ];

        const frame = findChild(buildElkGraph(nodes, edges, 'TB'), getFrameId('condition_1'));

        expect(childIds(frame)).toContain('condition_1-condition-left-placeholder-0');
        expect(childIds(frame)).toContain('condition_1-condition-right-placeholder-0');
    });

    it('nests a condition frame inside its parent frame', () => {
        const nodes: Node[] = [
            conditionNode('condition_1'),
            ...conditionGhostNodes('condition_1'),
            conditionNode('condition_2', {conditionCase: 'caseTrue', conditionId: 'condition_1'}),
            ...conditionGhostNodes('condition_2'),
            taskNode('innerChild', {conditionCase: 'caseTrue', conditionId: 'condition_2'}),
            taskNode('childFalse1', {conditionCase: 'caseFalse', conditionId: 'condition_1'}),
        ];

        const edges: Edge[] = [
            edge('condition_1', 'condition_1-condition-top-ghost'),
            edge('condition_1-condition-top-ghost', 'condition_2'),
            edge('condition_2', 'condition_2-condition-top-ghost'),
            edge('condition_2-condition-top-ghost', 'innerChild'),
            edge('innerChild', 'condition_2-condition-bottom-ghost'),
            edge('condition_2-condition-bottom-ghost', 'condition_1-condition-bottom-ghost'),
            edge('condition_1-condition-top-ghost', 'childFalse1'),
            edge('childFalse1', 'condition_1-condition-bottom-ghost'),
        ];

        const graph = buildElkGraph(nodes, edges, 'TB');

        const outerFrame = findChild(graph, getFrameId('condition_1'));

        expect(childIds(outerFrame)).toEqual([
            'childFalse1',
            'condition_1-condition-bottom-ghost',
            'condition_1-condition-top-ghost',
            'condition_2',
            getFrameId('condition_2'),
        ]);

        const innerFrame = findChild(outerFrame!, getFrameId('condition_2'));

        expect(childIds(innerFrame)).toEqual([
            'condition_2-condition-bottom-ghost',
            'condition_2-condition-top-ghost',
            'innerChild',
        ]);

        expect(collectScopeEdgeViolations(graph)).toEqual([]);
    });
});
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `npx vitest run src/pages/platform/workflow-editor/utils/elkLayoutUtils.test.ts`
Expected: FAIL — cannot resolve `./elkLayoutUtils`.

- [ ] **Step 3: Write the implementation**

```ts
// client/src/pages/platform/workflow-editor/utils/elkLayoutUtils.ts
import {LayoutDirectionType} from '@/shared/constants';
import {NodeDataType} from '@/shared/types';
import {Edge, Node} from '@xyflow/react';
import type {ElkExtendedEdge, ElkNode} from 'elkjs/lib/elk-api';

import {getDagreNodeSize} from './layoutUtils';

export const ELK_ROOT_ID = '__root__';

const ELK_SPACING = 50;

const FRAME_ID_SUFFIX = '__frame';

export function getFrameId(conditionId: string): string {
    return `${conditionId}${FRAME_ID_SUFFIX}`;
}

const getElkLayoutOptions = (direction: LayoutDirectionType): Record<string, string> => ({
    'elk.algorithm': 'layered',
    'elk.direction': direction === 'TB' ? 'DOWN' : 'RIGHT',
    'elk.layered.spacing.nodeNodeBetweenLayers': String(ELK_SPACING),
    'elk.padding': '[top=0,left=0,bottom=0,right=0]',
    'elk.spacing.nodeNode': String(ELK_SPACING),
});

/**
 * Returns the id of the condition that owns this node inside its frame, or
 * undefined for root-scope nodes. Auxiliary nodes (top/bottom ghosts, case
 * placeholders) reference their condition via conditionId + taskDispatcherId;
 * the condition task node itself also carries taskDispatcherId (its own name)
 * but must stay OUTSIDE its frame, hence the node.id check. Task children —
 * including nested condition nodes — carry conditionData.conditionId.
 */
function getOwningConditionId(node: Node): string | undefined {
    const nodeData = node.data as NodeDataType;

    if (
        nodeData.conditionId &&
        nodeData.taskDispatcherId === nodeData.conditionId &&
        node.id !== nodeData.conditionId
    ) {
        return nodeData.conditionId;
    }

    return nodeData.conditionData?.conditionId;
}

/**
 * Builds a hierarchical ELK graph from the flat ReactFlow node/edge lists.
 * Each condition contributes a compound frame node (sibling of the condition
 * task node) containing the condition's ghosts, placeholders, child tasks and,
 * recursively, nested condition frames. Edges are remapped so that an endpoint
 * living inside a frame is represented by that frame at the deepest scope
 * common to both endpoints — no edge ever crosses a hierarchy boundary, so
 * ELK's default SEPARATE_CHILDREN handling lays out every frame interior as an
 * independent sub-graph with identical spacing options.
 */
export function buildElkGraph(nodes: Node[], edges: Edge[], direction: LayoutDirectionType): ElkNode {
    const nodesById = new Map(nodes.map((node) => [node.id, node]));

    const conditionIds = nodes
        .filter((node) => {
            const nodeData = node.data as NodeDataType;

            return nodeData.taskDispatcher === true && nodeData.componentName === 'condition';
        })
        .map((node) => node.id);

    const getScope = (nodeId: string): string => {
        const node = nodesById.get(nodeId);

        if (!node) {
            return ELK_ROOT_ID;
        }

        return getOwningConditionId(node) || ELK_ROOT_ID;
    };

    // Scope chain from a scope up to the root, e.g. ['condition_2', 'condition_1', '__root__']
    const getScopeChain = (scope: string): string[] => {
        const chain = [scope];

        let currentScope = scope;

        while (currentScope !== ELK_ROOT_ID) {
            currentScope = getScope(currentScope);

            chain.push(currentScope);
        }

        return chain;
    };

    const getCommonScope = (sourceScope: string, targetScope: string): string => {
        const targetChainScopes = new Set(getScopeChain(targetScope));

        return getScopeChain(sourceScope).find((scope) => targetChainScopes.has(scope)) || ELK_ROOT_ID;
    };

    // Representative of a node at a given (ancestor) scope: the node itself when it
    // lives directly in that scope, otherwise the frame of its topmost enclosing
    // condition below that scope.
    const getRepresentativeInScope = (nodeId: string, scope: string): string => {
        if (getScope(nodeId) === scope) {
            return nodeId;
        }

        let enclosingConditionId = getScope(nodeId);

        while (getScope(enclosingConditionId) !== scope) {
            enclosingConditionId = getScope(enclosingConditionId);
        }

        return getFrameId(enclosingConditionId);
    };

    const elkEdgesByScope = new Map<string, ElkExtendedEdge[]>();
    const seenEdgeKeys = new Set<string>();

    edges.forEach((currentEdge) => {
        if (!nodesById.has(currentEdge.source) || !nodesById.has(currentEdge.target)) {
            return;
        }

        const commonScope = getCommonScope(getScope(currentEdge.source), getScope(currentEdge.target));

        const sourceRepresentative = getRepresentativeInScope(currentEdge.source, commonScope);
        const targetRepresentative = getRepresentativeInScope(currentEdge.target, commonScope);

        if (sourceRepresentative === targetRepresentative) {
            return;
        }

        const edgeKey = `${sourceRepresentative}=>${targetRepresentative}`;

        if (seenEdgeKeys.has(edgeKey)) {
            return;
        }

        seenEdgeKeys.add(edgeKey);

        const scopeEdges = elkEdgesByScope.get(commonScope) || [];

        scopeEdges.push({id: `elk-edge-${edgeKey}`, sources: [sourceRepresentative], targets: [targetRepresentative]});

        elkEdgesByScope.set(commonScope, scopeEdges);
    });

    const buildScopeChildren = (scope: string): ElkNode[] => {
        const scopeChildren: ElkNode[] = [];

        nodes.forEach((node) => {
            if (getScope(node.id) !== scope) {
                return;
            }

            const {height, width} = getDagreNodeSize(node, direction);

            scopeChildren.push({height, id: node.id, width});
        });

        conditionIds.forEach((conditionId) => {
            if (getScope(conditionId) !== scope) {
                return;
            }

            scopeChildren.push({
                children: buildScopeChildren(conditionId),
                edges: elkEdgesByScope.get(conditionId) || [],
                id: getFrameId(conditionId),
                layoutOptions: getElkLayoutOptions(direction),
            });
        });

        return scopeChildren;
    };

    return {
        children: buildScopeChildren(ELK_ROOT_ID),
        edges: elkEdgesByScope.get(ELK_ROOT_ID) || [],
        id: ELK_ROOT_ID,
        layoutOptions: getElkLayoutOptions(direction),
    };
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `npx vitest run src/pages/platform/workflow-editor/utils/elkLayoutUtils.test.ts`
Expected: PASS (6 tests).

Note: if TypeScript cannot resolve types from `elkjs/lib/elk-api`, add a `client/src/types/elkjs.d.ts` re-export shim — but try the direct type import first; elkjs ships `lib/elk-api.d.ts`.

- [ ] **Step 5: Commit**

```bash
git add client/src/pages/platform/workflow-editor/utils/elkLayoutUtils.ts client/src/pages/platform/workflow-editor/utils/elkLayoutUtils.test.ts
git commit -m "client - Add ELK hierarchical graph builder for workflow layout"
```

---

### Task 5: `getElkLayoutElements` — layout execution and coordinate mapping

**Files:**
- Modify: `client/src/pages/platform/workflow-editor/utils/elkLayoutUtils.ts`
- Test: `client/src/pages/platform/workflow-editor/utils/elkLayoutUtils.test.ts` (append a `describe` block)

**Interfaces:**
- Consumes: `buildElkGraph`/`getFrameId` (Task 4); `GetLayoutElementsProps`, `filterAndDedupeLayoutEdges`, `getLayoutElements`, `positionTriggerPlaceholder` from `./layoutUtils` (Task 3); `applySavedPositions` from `./postDagreConstraints`; `getCrossAxis` from `./directionUtils`; constants `NODE_HEIGHT`, `NODE_WIDTH`, `PLACEHOLDER_NODE_HEIGHT`, `PLACEHOLDER_NODE_WIDTH`, `TRIGGER_PLACEHOLDER_NODE_ID` from `@/shared/constants`.
- Produces: `export const getElkLayoutElements = async (props: GetLayoutElementsProps): Promise<{edges: Edge[]; nodes: Node[]}>` — drop-in alternative to `getLayoutElements`, used by Task 6. On any ELK failure it logs and falls back to `getLayoutElements` with the same props.
- Coordinate convention: ELK boxes are *footprints* (`getDagreNodeSize`); ELK returns parent-relative top-left coordinates which are flattened to absolute; the final ReactFlow position centers the *rendered* box inside its footprint box on both axes. Synthetic frame nodes are dropped after flattening.

- [ ] **Step 1: Write the failing tests (append to `elkLayoutUtils.test.ts`)**

Add to the imports: `import {NODE_HEIGHT} from '@/shared/constants';` and `import {getElkLayoutElements} from './elkLayoutUtils';` (merge into the existing import line). Then append:

```ts
const positionOf = (layoutedNodes: Node[], id: string): {x: number; y: number} => {
    const layoutedNode = layoutedNodes.find((node) => node.id === id);

    if (!layoutedNode) {
        throw new Error(`Node ${id} missing from layout result`);
    }

    return layoutedNode.position;
};

describe('getElkLayoutElements', () => {
    it('spaces a TB chain uniformly (footprint gap = 50)', async () => {
        const nodes = [taskNode('task1'), taskNode('task2'), taskNode('task3')];
        const edges = [edge('task1', 'task2'), edge('task2', 'task3')];

        const result = await getElkLayoutElements({canvasWidth: 1000, direction: 'TB', edges, nodes});

        const firstGap = positionOf(result.nodes, 'task2').y - positionOf(result.nodes, 'task1').y;
        const secondGap = positionOf(result.nodes, 'task3').y - positionOf(result.nodes, 'task2').y;

        expect(firstGap).toBe(NODE_HEIGHT + 50);
        expect(secondGap).toBe(NODE_HEIGHT + 50);
    });

    it('spaces an LR chain uniformly on the x axis', async () => {
        const nodes = [taskNode('task1'), taskNode('task2'), taskNode('task3')];
        const edges = [edge('task1', 'task2'), edge('task2', 'task3')];

        const result = await getElkLayoutElements({
            canvasHeight: 800,
            canvasWidth: 1000,
            direction: 'LR',
            edges,
            nodes,
        });

        const firstGap = positionOf(result.nodes, 'task2').x - positionOf(result.nodes, 'task1').x;
        const secondGap = positionOf(result.nodes, 'task3').x - positionOf(result.nodes, 'task2').x;

        // LR footprint width is 120 (see getDagreNodeSize) + 50 spacing
        expect(firstGap).toBe(170);
        expect(secondGap).toBe(170);
    });

    it('uses the same gap inside a nested condition branch as at the root', async () => {
        const nodes: Node[] = [
            taskNode('task1'),
            conditionNode('condition_1'),
            ...conditionGhostNodes('condition_1'),
            taskNode('childTrue1', {conditionCase: 'caseTrue', conditionId: 'condition_1'}),
            taskNode('childTrue2', {conditionCase: 'caseTrue', conditionId: 'condition_1'}),
            taskNode('childFalse1', {conditionCase: 'caseFalse', conditionId: 'condition_1'}),
            taskNode('task2'),
        ];

        const edges: Edge[] = [
            edge('task1', 'condition_1'),
            edge('condition_1', 'condition_1-condition-top-ghost'),
            edge('condition_1-condition-top-ghost', 'childTrue1'),
            edge('childTrue1', 'childTrue2'),
            edge('childTrue2', 'condition_1-condition-bottom-ghost'),
            edge('condition_1-condition-top-ghost', 'childFalse1'),
            edge('childFalse1', 'condition_1-condition-bottom-ghost'),
            edge('condition_1-condition-bottom-ghost', 'task2'),
        ];

        const result = await getElkLayoutElements({canvasWidth: 1000, direction: 'TB', edges, nodes});

        const rootGap = positionOf(result.nodes, 'condition_1').y - positionOf(result.nodes, 'task1').y;
        const branchGap = positionOf(result.nodes, 'childTrue2').y - positionOf(result.nodes, 'childTrue1').y;

        expect(branchGap).toBe(NODE_HEIGHT + 50);
        expect(rootGap).toBe(NODE_HEIGHT + 50);
    });

    it('drops synthetic frame nodes from the result', async () => {
        const {edges, nodes} = singleConditionFixture();

        const result = await getElkLayoutElements({canvasWidth: 1000, direction: 'TB', edges, nodes});

        expect(result.nodes.map((node) => node.id)).not.toContain(getFrameId('condition_1'));
        expect(result.nodes).toHaveLength(nodes.length);
    });

    it('centers the condition node and its ghosts on the same cross-axis line', async () => {
        const {edges, nodes} = singleConditionFixture();

        const result = await getElkLayoutElements({canvasWidth: 1000, direction: 'TB', edges, nodes});

        const conditionPosition = positionOf(result.nodes, 'condition_1');
        const topGhostPosition = positionOf(result.nodes, 'condition_1-condition-top-ghost');
        const bottomGhostPosition = positionOf(result.nodes, 'condition_1-condition-bottom-ghost');

        // rendered widths: condition 240, ghosts 24 (PLACEHOLDER_NODE_HEIGHT-ish bar)
        const conditionCenter = conditionPosition.x + 120;
        const topGhostCenter = topGhostPosition.x + 12;
        const bottomGhostCenter = bottomGhostPosition.x + 12;

        expect(Math.abs(topGhostCenter - conditionCenter)).toBeLessThanOrEqual(1);
        expect(Math.abs(bottomGhostCenter - conditionCenter)).toBeLessThanOrEqual(1);
    });

    it('honors saved node positions', async () => {
        const nodes = [taskNode('task1'), taskNode('task2')];

        (nodes[1].data as Record<string, unknown>).metadata = {ui: {nodePosition: {x: 400, y: 900}}};

        const result = await getElkLayoutElements({
            canvasWidth: 1000,
            direction: 'TB',
            edges: [edge('task1', 'task2')],
            nodes,
        });

        expect(positionOf(result.nodes, 'task2')).toEqual({x: 400, y: 900});
    });
});
```

If the cross-axis centering assertion fails, treat it as a layout-quality bug: tune ELK node placement (e.g. add `'elk.layered.nodePlacement.favorStraightEdges': 'true'` to `getElkLayoutOptions`) or extend the deterministic ghost fixup — do NOT loosen the test beyond the 1px tolerance.

- [ ] **Step 2: Run tests to verify the new block fails**

Run: `npx vitest run src/pages/platform/workflow-editor/utils/elkLayoutUtils.test.ts`
Expected: Task 4 tests PASS; new `getElkLayoutElements` tests FAIL (export missing).

- [ ] **Step 3: Write the implementation (append to `elkLayoutUtils.ts`)**

Extend the import block at the top of the file:

```ts
import {
    NODE_HEIGHT,
    NODE_WIDTH,
    PLACEHOLDER_NODE_HEIGHT,
    PLACEHOLDER_NODE_WIDTH,
    TRIGGER_PLACEHOLDER_NODE_ID,
} from '@/shared/constants';

import {getCrossAxis} from './directionUtils';
import {
    GetLayoutElementsProps,
    filterAndDedupeLayoutEdges,
    getDagreNodeSize,
    getLayoutElements,
    positionTriggerPlaceholder,
} from './layoutUtils';
import {applySavedPositions} from './postDagreConstraints';
```

(Merge with the existing `LayoutDirectionType` / `getDagreNodeSize` imports; keep alphabetical order within braces.)

Then append:

```ts
type ElkInstanceType = {layout: (graph: ElkNode) => Promise<ElkNode>};

let elkInstance: ElkInstanceType | null = null;

const loadElk = async (): Promise<ElkInstanceType> => {
    if (!elkInstance) {
        const {default: ELK} = await import('elkjs/lib/elk.bundled.js');

        elkInstance = new ELK() as ElkInstanceType;
    }

    return elkInstance;
};

/**
 * Approximate rendered box of a node. ELK is fed footprint sizes (shared with
 * dagre via getDagreNodeSize); the rendered box is centered inside the footprint
 * on both axes when converting to ReactFlow's top-left positions.
 */
function getRenderedNodeSize(node: Node, direction: LayoutDirectionType): {height: number; width: number} {
    const isGhostNode = node.type === 'taskDispatcherTopGhostNode' || node.type === 'taskDispatcherBottomGhostNode';
    const isSmallNode = node.type === 'placeholder' || node.type === 'triggerPlaceholder';
    const isTrigger = (node.data as NodeDataType).trigger === true && node.id !== TRIGGER_PLACEHOLDER_NODE_ID;

    if (direction === 'LR') {
        if (isGhostNode) {
            return {height: PLACEHOLDER_NODE_HEIGHT, width: 2};
        }

        if (isSmallNode) {
            return {height: PLACEHOLDER_NODE_HEIGHT, width: PLACEHOLDER_NODE_WIDTH};
        }

        return {height: NODE_HEIGHT, width: 72};
    }

    if (isGhostNode) {
        return {height: 2, width: PLACEHOLDER_NODE_HEIGHT};
    }

    if (isSmallNode) {
        return {height: PLACEHOLDER_NODE_HEIGHT, width: PLACEHOLDER_NODE_WIDTH};
    }

    if (isTrigger) {
        return {height: NODE_HEIGHT, width: 72};
    }

    return {height: NODE_HEIGHT, width: NODE_WIDTH};
}

type AbsoluteBoxType = {height: number; width: number; x: number; y: number};

/**
 * Drop-in alternative to getLayoutElements() backed by ELK's hierarchical
 * layered layout. Positions only — node/edge creation is untouched. Falls back
 * to the dagre path if ELK fails for any reason.
 */
export const getElkLayoutElements = async ({
    canvasHeight,
    canvasWidth,
    direction = 'TB',
    edges,
    nodes,
    savedPositionCrossAxisShift = 0,
}: GetLayoutElementsProps): Promise<{edges: Edge[]; nodes: Node[]}> => {
    try {
        const elk = await loadElk();

        const layoutedGraph = await elk.layout(buildElkGraph(nodes, edges, direction));

        // Flatten ELK's parent-relative coordinates to absolute footprint boxes
        const absoluteBoxes = new Map<string, AbsoluteBoxType>();

        const flattenElkNode = (elkNode: ElkNode, offsetX: number, offsetY: number): void => {
            (elkNode.children || []).forEach((child) => {
                const absoluteX = offsetX + (child.x || 0);
                const absoluteY = offsetY + (child.y || 0);

                absoluteBoxes.set(child.id, {
                    height: child.height || 0,
                    width: child.width || 0,
                    x: absoluteX,
                    y: absoluteY,
                });

                flattenElkNode(child, absoluteX, absoluteY);
            });
        };

        flattenElkNode(layoutedGraph, 0, 0);

        // Deterministic fixup: center each condition's ghosts on its frame's cross-axis center
        nodes.forEach((node) => {
            const nodeData = node.data as NodeDataType;

            if (nodeData.taskDispatcher !== true || nodeData.componentName !== 'condition') {
                return;
            }

            const frameBox = absoluteBoxes.get(getFrameId(node.id));

            if (!frameBox) {
                return;
            }

            const frameCrossCenter =
                direction === 'TB' ? frameBox.x + frameBox.width / 2 : frameBox.y + frameBox.height / 2;

            [`${node.id}-condition-top-ghost`, `${node.id}-condition-bottom-ghost`].forEach((ghostId) => {
                const ghostBox = absoluteBoxes.get(ghostId);

                if (!ghostBox) {
                    return;
                }

                if (direction === 'TB') {
                    ghostBox.x = frameCrossCenter - ghostBox.width / 2;
                } else {
                    ghostBox.y = frameCrossCenter - ghostBox.height / 2;
                }
            });
        });

        // Canvas centering: put the trigger row midpoint on the canvas cross-axis center
        const crossAxis = getCrossAxis(direction);
        const canvasCrossDimension = direction === 'LR' && canvasHeight ? canvasHeight : canvasWidth;

        const entryCenters = nodes
            .filter(
                (node) => (node.data as NodeDataType).trigger === true && node.id !== TRIGGER_PLACEHOLDER_NODE_ID
            )
            .map((node) => {
                const box = absoluteBoxes.get(node.id);

                if (!box) {
                    return canvasCrossDimension / 2;
                }

                return crossAxis === 'x' ? box.x + box.width / 2 : box.y + box.height / 2;
            });

        const entryAnchor =
            entryCenters.length > 0
                ? (Math.min(...entryCenters) + Math.max(...entryCenters)) / 2
                : canvasCrossDimension / 2;

        const centeringOffset = canvasCrossDimension / 2 - entryAnchor;

        // Convert footprint boxes to rendered top-left positions
        const allNodes: Node[] = nodes.map((node) => {
            const box = absoluteBoxes.get(node.id);

            if (!box) {
                return node;
            }

            const renderedSize = getRenderedNodeSize(node, direction);

            const position = {
                x: box.x + (box.width - renderedSize.width) / 2,
                y: box.y + (box.height - renderedSize.height) / 2,
            };

            position[crossAxis] += centeringOffset;

            return {...node, position};
        });

        positionTriggerPlaceholder(allNodes, direction);

        applySavedPositions(allNodes, crossAxis, savedPositionCrossAxisShift);

        return {edges: filterAndDedupeLayoutEdges(allNodes, edges), nodes: allNodes};
    } catch (error) {
        console.error('ELK layout failed, falling back to dagre', error);

        return getLayoutElements({canvasHeight, canvasWidth, direction, edges, nodes, savedPositionCrossAxisShift});
    }
};
```

Note: `elkjs/lib/elk.bundled.js` runs fine under Node, so these tests execute the real ELK layout — the spacing assertions are genuine end-to-end checks.

- [ ] **Step 4: Run tests to verify they pass**

Run: `npx vitest run src/pages/platform/workflow-editor/utils/elkLayoutUtils.test.ts`
Expected: PASS (12 tests). If the ghost-centering test fails by >1px, apply the tuning noted in Step 1.

- [ ] **Step 5: Typecheck and commit**

Run: `npm run typecheck`
Expected: clean.

```bash
git add client/src/pages/platform/workflow-editor/utils/elkLayoutUtils.ts client/src/pages/platform/workflow-editor/utils/elkLayoutUtils.test.ts
git commit -m "client - Add ELK layout execution with uniform spacing at every nesting depth"
```

---

### Task 6: Engine selection in `useLayout`

**Files:**
- Modify: `client/src/pages/platform/workflow-editor/hooks/useLayout.tsx`

**Interfaces:**
- Consumes: `useLayoutEngineStore` (Task 2), `isElkLayoutSupported` (Task 1), `getElkLayoutElements` (Task 5).
- Produces: no new exports — behavior change only. Both engines already return the same `{nodes, edges}` shape, so the `.then(...)` handling (animation, pruning) is untouched.

- [ ] **Step 1: Add imports and store subscription**

Add to the import block of `useLayout.tsx` (respecting existing import grouping):

```ts
import useLayoutEngineStore from '../stores/useLayoutEngineStore';
import {getElkLayoutElements} from '../utils/elkLayoutUtils';
import isElkLayoutSupported from '../utils/isElkLayoutSupported';
```

Next to the existing `const storeDirection = useLayoutDirectionStore(...)` line (~line 123), add:

```ts
    const layoutEngine = useLayoutEngineStore((state) => state.layoutEngine);
```

- [ ] **Step 2: Branch at the layout call**

At the layout invocation (currently line ~952), change:

```ts
        getLayoutElements({
            canvasHeight: canvasHeightRef.current,
            canvasWidth: canvasWidthRef.current,
            direction: layoutDirection,
            edges,
            nodes: layoutNodes,
            savedPositionCrossAxisShift,
        }).then((elements) => {
```

to:

```ts
        const layoutFunction =
            layoutEngine === 'elk' && isElkLayoutSupported(layoutNodes) ? getElkLayoutElements : getLayoutElements;

        layoutFunction({
            canvasHeight: canvasHeightRef.current,
            canvasWidth: canvasWidthRef.current,
            direction: layoutDirection,
            edges,
            nodes: layoutNodes,
            savedPositionCrossAxisShift,
        }).then((elements) => {
```

- [ ] **Step 3: Add `layoutEngine` to the effect dependencies**

Change the dependency array (currently line ~1006):

```ts
    }, [layoutDirection, layoutResetCounter, tasks, triggers, isWorkflowLoaded]);
```

to:

```ts
    }, [layoutDirection, layoutEngine, layoutResetCounter, tasks, triggers, isWorkflowLoaded]);
```

- [ ] **Step 4: Verify**

Run: `npm run typecheck && npx vitest run src/pages/platform/workflow-editor`
Expected: clean typecheck; all workflow-editor tests PASS (toggling re-layouts via the effect; no test changes needed here).

- [ ] **Step 5: Commit**

```bash
git add client/src/pages/platform/workflow-editor/hooks/useLayout.tsx
git commit -m "client - Route layout through selected engine in useLayout"
```

---

### Task 7: Toolbar switch button

**Files:**
- Modify: `client/src/pages/platform/workflow-editor/components/WorkflowEditorToolbar.tsx`
- Test: `client/src/pages/platform/workflow-editor/components/WorkflowEditorToolbar.test.tsx` (append a `describe` block)

**Interfaces:**
- Consumes: `useLayoutEngineStore` (Task 2), `isElkLayoutSupported` (Task 1), existing `nodes` subscription from `useWorkflowDataStore`.
- Produces: a toolbar button (aria-labels: `Switch to experimental layout engine` / `Switch to standard layout engine`) placed directly after the TB/LR direction toggle button; disabled when `isElkLayoutSupported(nodes)` is false.

- [ ] **Step 1: Write the failing tests (append to `WorkflowEditorToolbar.test.tsx`)**

Add `import useLayoutEngineStore from '../stores/useLayoutEngineStore';` to the test file imports, then append:

```tsx
describe('WorkflowEditorToolbar - layout engine button', () => {
    beforeEach(() => {
        useWorkflowDataStore.setState({edges: [], nodes: []});
        useLayoutEngineStore.setState({layoutEngine: 'dagre'});
    });

    it('renders enabled for a condition-only workflow and toggles the engine', async () => {
        useWorkflowDataStore.setState({
            nodes: [
                {
                    data: {componentName: 'condition', taskDispatcher: true, taskDispatcherId: 'condition_1'},
                    id: 'condition_1',
                    position: {x: 0, y: 0},
                    type: 'workflow',
                },
            ],
        });

        const user = userEvent.setup();

        renderToolbar(false);

        const layoutEngineButton = screen.getByLabelText('Switch to experimental layout engine');

        expect(layoutEngineButton).toBeEnabled();

        await user.click(layoutEngineButton);

        expect(useLayoutEngineStore.getState().layoutEngine).toBe('elk');
        expect(screen.getByLabelText('Switch to standard layout engine')).toBeInTheDocument();
    });

    it('is disabled when the workflow contains an unsupported dispatcher', () => {
        useWorkflowDataStore.setState({
            nodes: [
                {
                    data: {componentName: 'loop', taskDispatcher: true, taskDispatcherId: 'loop_1'},
                    id: 'loop_1',
                    position: {x: 0, y: 0},
                    type: 'workflow',
                },
            ],
        });

        renderToolbar(false);

        expect(screen.getByLabelText('Switch to experimental layout engine')).toBeDisabled();
    });
});
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `npx vitest run src/pages/platform/workflow-editor/components/WorkflowEditorToolbar.test.tsx`
Expected: existing lock-button tests PASS; new tests FAIL (button not found).

- [ ] **Step 3: Implement the button**

In `WorkflowEditorToolbar.tsx`:

1. Add `NetworkIcon` to the lucide import (alphabetical position within the braces).
2. Add imports:

```ts
import {twMerge} from 'tailwind-merge';

import useLayoutEngineStore from '../stores/useLayoutEngineStore';
import isElkLayoutSupported from '../utils/isElkLayoutSupported';
```

3. Inside the component, after the `useLayoutDirectionStore` subscription:

```ts
    const {layoutEngine, setLayoutEngine} = useLayoutEngineStore(
        useShallow((state) => ({
            layoutEngine: state.layoutEngine,
            setLayoutEngine: state.setLayoutEngine,
        }))
    );
```

4. After the `taskCount` derivation:

```ts
    const elkLayoutSupported = isElkLayoutSupported(nodes);

    const layoutEngineLabel =
        layoutEngine === 'elk' ? 'Switch to standard layout engine' : 'Switch to experimental layout engine';

    let layoutEngineTooltip = layoutEngineLabel;

    if (!elkLayoutSupported) {
        layoutEngineTooltip = 'Experimental layout supports only Condition for now';
    }
```

5. Add the handler next to `handleToggleLayout`:

```ts
    const handleToggleLayoutEngine = useCallback(() => {
        setLayoutEngine(layoutEngine === 'elk' ? 'dagre' : 'elk');
    }, [layoutEngine, setLayoutEngine]);
```

6. Add the button JSX directly after the direction-toggle `<Tooltip>` block (the one wrapping the `ArrowRightIcon` button, currently ending at line ~170):

```tsx
                    <Tooltip>
                        <TooltipTrigger asChild>
                            <Button
                                aria-label={layoutEngineLabel}
                                className={twMerge(layoutEngine === 'elk' && 'text-content-brand-primary')}
                                disabled={!elkLayoutSupported}
                                icon={<NetworkIcon />}
                                onClick={handleToggleLayoutEngine}
                                size="icon"
                                variant="outline"
                            />
                        </TooltipTrigger>

                        <TooltipContent
                            className="rounded-lg bg-surface-tooltip text-content-onsurface-primary"
                            side="top"
                        >
                            {layoutEngineTooltip}
                        </TooltipContent>
                    </Tooltip>
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `npx vitest run src/pages/platform/workflow-editor/components/WorkflowEditorToolbar.test.tsx`
Expected: PASS (all describe blocks).

- [ ] **Step 5: Commit**

```bash
git add client/src/pages/platform/workflow-editor/components/WorkflowEditorToolbar.tsx client/src/pages/platform/workflow-editor/components/WorkflowEditorToolbar.test.tsx
git commit -m "client - Add layout engine switch button to workflow editor toolbar"
```

---

### Task 8: Full verification

**Files:** none new.

- [ ] **Step 1: Run the full client check**

Run from `client/`: `npm run check`
Expected: lint, typecheck, and all tests PASS. Fix any `sort-keys` or import-order violations flagged (they cannot be auto-fixed).

- [ ] **Step 2: Manual A/B verification in the dev server**

Start infra + server + client (`npm run dev` in `client/`; see CLAUDE.md for backend). Log in as admin@localhost.com/admin. In a project workflow:

1. Build: trigger → task → condition (both branches populated, one branch containing a nested condition with a further nested condition) → task.
2. Toggle the new toolbar button (network icon): layout should re-run and animate; every consecutive-node gap should be visually equal (50px between footprints) at every depth; nested condition frames must not overlap their siblings.
3. Toggle TB ↔ LR in ELK mode: same consistency horizontally.
4. Drag a node, save, re-open the workflow in ELK mode: dragged position must be honored.
5. Add a loop task: the engine button must disable with the "supports only Condition" tooltip, and layout must fall back to dagre without errors.
6. Compare visually against dagre mode (toggle back and forth) and note any spacing constants worth tuning — tuning happens in `getElkLayoutOptions` / `getRenderedNodeSize` only.

Expected: no console errors; layout switch round-trips cleanly.

- [ ] **Step 3: Final commit (only if fixes were needed)**

```bash
git add -A client/src
git commit -m "client - Polish ELK layout engine after manual verification"
```
