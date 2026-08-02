import {EDGE_STYLES} from '@/shared/constants';
import {GraphNodeType, NodeDataType} from '@/shared/types';
import {Node} from '@xyflow/react';
import {describe, expect, it} from 'vitest';

import createGraphEdges, {getGraphNodeSide} from '../utils/createGraphEdges';

function makeGraphNode(id: string, nodes: Array<GraphNodeType> = []): Node {
    return {
        data: {
            componentName: 'graph',
            parameters: {
                maxTransitions: 100,
                nodes,
            },
        } as unknown as NodeDataType,
        id,
        position: {x: 0, y: 0},
        type: 'workflow',
    };
}

describe('createGraphEdges', () => {
    describe('base structure', () => {
        it('should create an edge from the graph node to its top ghost', () => {
            const graphNode = makeGraphNode('graph_1');

            const edges = createGraphEdges(graphNode);

            const graphToTopGhost = edges.find(
                (edge) => edge.source === 'graph_1' && edge.target === 'graph_1-graph-top-ghost'
            );

            expect(graphToTopGhost).toBeDefined();
            expect(graphToTopGhost!.type).toBe('smoothstep');
            expect(graphToTopGhost!.style).toEqual(EDGE_STYLES);
        });

        it('should create edges for the trailing add-node placeholder even with zero nodes', () => {
            const graphNode = makeGraphNode('graph_1', []);

            const edges = createGraphEdges(graphNode);

            const topGhostToPlaceholder = edges.find(
                (edge) =>
                    edge.source === 'graph_1-graph-top-ghost' && edge.target === 'graph_1-graph-node-0-placeholder-0'
            );
            const placeholderToBottomGhost = edges.find(
                (edge) =>
                    edge.source === 'graph_1-graph-node-0-placeholder-0' && edge.target === 'graph_1-graph-bottom-ghost'
            );

            // With no lanes the placeholder is the only child and layout centers it under the ghost
            // bars, so its edges anchor to the CENTER handles instead of the right-side ones.
            expect(topGhostToPlaceholder).toBeDefined();
            expect(topGhostToPlaceholder!.sourceHandle).toBe('graph_1-graph-top-ghost-bottom');
            expect(placeholderToBottomGhost).toBeDefined();
            expect(placeholderToBottomGhost!.targetHandle).toBe('graph_1-graph-bottom-ghost-top');
        });
    });

    describe('empty node lanes', () => {
        it('should create placeholder edges for an empty node', () => {
            const graphNode = makeGraphNode('graph_1', [{name: 'node_0', tasks: []}]);

            const edges = createGraphEdges(graphNode);

            const placeholderEdges = edges.filter(
                (edge) => edge.source.includes('placeholder') || edge.target.includes('placeholder')
            );

            expect(placeholderEdges.length).toBeGreaterThan(0);
        });
    });

    describe('non-empty node lanes', () => {
        it('should chain tasks within a node lane', () => {
            const graphNode = makeGraphNode('graph_1', [
                {
                    name: 'node_0',
                    tasks: [
                        {name: 'task_1', type: 'task/v1'},
                        {name: 'task_2', type: 'task/v1'},
                    ],
                },
            ]);

            const edges = createGraphEdges(graphNode);

            const taskToTask = edges.find((edge) => edge.source === 'task_1' && edge.target === 'task_2');

            expect(taskToTask).toBeDefined();
            expect(taskToTask!.type).toBe('workflow');

            const topGhostToFirstTask = edges.find(
                (edge) => edge.source === 'graph_1-graph-top-ghost' && edge.target === 'task_1'
            );

            expect(topGhostToFirstTask).toBeDefined();

            const lastTaskToBottomGhost = edges.find(
                (edge) => edge.source === 'task_2' && edge.target === 'graph_1-graph-bottom-ghost'
            );

            expect(lastTaskToBottomGhost).toBeDefined();
        });

        it('should stitch a nested task dispatcher lane member to the graph bottom ghost via its own bottom ghost', () => {
            const graphNode = makeGraphNode('graph_1', [{name: 'node_0', tasks: [{name: 'loop_1', type: 'loop/v1'}]}]);

            const edges = createGraphEdges(graphNode);

            const nestedGhostEdge = edges.find(
                (edge) => edge.source === 'loop_1-loop-bottom-ghost' && edge.target === 'graph_1-graph-bottom-ghost'
            );

            expect(nestedGhostEdge).toBeDefined();
        });
    });

    describe('node lane distribution', () => {
        it('should distribute left/middle/right the same way fork-join distributes branches (odd count including the trailing lane)', () => {
            // 2 real nodes => (2+1) is odd => left=1, middle=1, right=0 before accounting for
            // the fixed-right trailing placeholder edges added separately.
            const graphNode = makeGraphNode('graph_1', [
                {name: 'node_0', tasks: []},
                {name: 'node_1', tasks: []},
            ]);

            const edges = createGraphEdges(graphNode);

            const leftEdges = edges.filter((edge) => edge.sourceHandle?.endsWith('-left'));
            const middleEdges = edges.filter((edge) => edge.sourceHandle?.endsWith('-bottom'));

            expect(leftEdges.length).toBeGreaterThan(0);
            expect(middleEdges.length).toBeGreaterThan(0);
        });

        it('should give every node its own lane regardless of emptiness (unlike fork-join, which filters empty branches out)', () => {
            const graphNode = makeGraphNode('graph_1', [
                {name: 'node_0', tasks: []},
                {name: 'node_1', tasks: [{name: 'task_1', type: 'task/v1'}]},
                {name: 'node_2', tasks: []},
            ]);

            const edges = createGraphEdges(graphNode);

            const lanePlaceholderEdges = edges.filter(
                (edge) =>
                    edge.target === 'graph_1-graph-node-0-placeholder-0' ||
                    edge.target === 'graph_1-graph-node-2-placeholder-0'
            );

            expect(lanePlaceholderEdges.length).toBeGreaterThan(0);

            const nonEmptyLaneEdge = edges.find(
                (edge) => edge.source === 'graph_1-graph-top-ghost' && edge.target === 'task_1'
            );

            expect(nonEmptyLaneEdge).toBeDefined();
        });
    });
});

describe('createGraphEdges transition overlay edges', () => {
    const cyclicNodes: Array<GraphNodeType> = [
        {name: 'nodeA', next: "'nodeB'", tasks: [{name: 'task_a', type: 'task/v1'}]},
        {name: 'nodeB', next: "'nodeA'", tasks: [{name: 'task_b', type: 'task/v1'}]},
    ];

    it('should NOT include transition edges by default (no options passed)', () => {
        const graphNode = makeGraphNode('graph_1', cyclicNodes);

        const edges = createGraphEdges(graphNode);

        expect(edges.some((edge) => edge.type === 'graphTransition')).toBe(false);
    });

    it('should NOT include transition edges when orderLanesByVisualPosition is false (the dagre path)', () => {
        const graphNode = makeGraphNode('graph_1', cyclicNodes);

        const edges = createGraphEdges(graphNode, {orderLanesByVisualPosition: false});

        expect(edges.some((edge) => edge.type === 'graphTransition')).toBe(false);
    });

    it('should derive a forward and a back transition edge for a cyclic pair under ELK', () => {
        const graphNode = makeGraphNode('graph_1', cyclicNodes);

        const edges = createGraphEdges(graphNode, {orderLanesByVisualPosition: true});

        const transitionEdges = edges.filter((edge) => edge.type === 'graphTransition');

        expect(transitionEdges).toHaveLength(2);

        const forwardEdge = transitionEdges.find((edge) => edge.data?.kind === 'forward');
        const backEdge = transitionEdges.find((edge) => edge.data?.kind === 'back');

        expect(forwardEdge).toMatchObject({
            data: {kind: 'forward', sourceIndex: 0, targetIndex: 1, targetName: 'nodeB'},
            id: 'graph_1-transition-0-1',
            source: 'task_a',
            target: 'task_b',
        });
        expect(backEdge).toMatchObject({
            data: {kind: 'back', sourceIndex: 1, targetIndex: 0, targetName: 'nodeA'},
            id: 'graph_1-transition-1-0',
            source: 'task_b',
            target: 'task_a',
        });
    });

    it('should anchor to the lane placeholder for an empty lane', () => {
        const graphNode = makeGraphNode('graph_1', [
            {name: 'nodeA', next: "'nodeB'", tasks: []},
            {name: 'nodeB', tasks: []},
        ]);

        const edges = createGraphEdges(graphNode, {orderLanesByVisualPosition: true});

        const transitionEdge = edges.find((edge) => edge.type === 'graphTransition');

        expect(transitionEdge).toMatchObject({
            source: 'graph_1-graph-node-0-placeholder-0',
            target: 'graph_1-graph-node-1-placeholder-0',
        });
    });

    it('should derive a self transition edge with a <graphId>-transition-<index>-<index> id shape', () => {
        const graphNode = makeGraphNode('graph_1', [
            {name: 'nodeA', next: "'nodeA'", tasks: [{name: 'task_a', type: 'task/v1'}]},
        ]);

        const edges = createGraphEdges(graphNode, {orderLanesByVisualPosition: true});

        const selfEdge = edges.find((edge) => edge.type === 'graphTransition');

        expect(selfEdge).toMatchObject({
            data: {kind: 'self', sourceIndex: 0, targetIndex: 0},
            id: 'graph_1-transition-0-0',
            source: 'task_a',
            target: 'task_a',
        });
    });

    it('should produce no transition edges for a graph with no resolvable next targets', () => {
        const graphNode = makeGraphNode('graph_1', [{name: 'nodeA', tasks: [{name: 'task_a', type: 'task/v1'}]}]);

        const edges = createGraphEdges(graphNode, {orderLanesByVisualPosition: true});

        expect(edges.some((edge) => edge.type === 'graphTransition')).toBe(false);
    });
});

describe('createGraphEdges lane handle-side grouping', () => {
    // Declaration order is node_0..node_3, but the transitions form the chain
    // node_0 -> node_3 -> node_2 -> node_1 (with node_1 -> node_0 closing the cycle), so under
    // orderLanesByVisualPosition the physical left-to-right column order is node_0, node_3,
    // node_2, node_1 — matching the ELK lane order from elkLayoutUtils' getMemberCaseRank.
    const cyclicFourNodeChain: Array<GraphNodeType> = [
        {name: 'node_0', next: "'node_3'", tasks: [{name: 'task_0', type: 'task/v1'}]},
        {name: 'node_1', next: "'node_0'", tasks: [{name: 'task_1', type: 'task/v1'}]},
        {name: 'node_2', next: "'node_1'", tasks: [{name: 'task_2', type: 'task/v1'}]},
        {name: 'node_3', next: "'node_2'", tasks: [{name: 'task_3', type: 'task/v1'}]},
    ];

    const sourceHandleForTask = (
        edges: ReturnType<typeof createGraphEdges>,
        taskName: string
    ): string | null | undefined =>
        edges.find((edge) => edge.source === 'graph_1-graph-top-ghost' && edge.target === taskName)?.sourceHandle;

    it('keeps declaration-order handle grouping when orderLanesByVisualPosition is not set (dagre path), even though the graph declares a different transition order', () => {
        const graphNode = makeGraphNode('graph_1', cyclicFourNodeChain);

        const edges = createGraphEdges(graphNode);

        // Declaration order [0,1,2,3]: left=[0,1], middle=2, right=[3] — unaffected by `next`
        expect(sourceHandleForTask(edges, 'task_0')).toBe('graph_1-graph-top-ghost-left');
        expect(sourceHandleForTask(edges, 'task_1')).toBe('graph_1-graph-top-ghost-left');
        expect(sourceHandleForTask(edges, 'task_2')).toBe('graph_1-graph-top-ghost-bottom');
        expect(sourceHandleForTask(edges, 'task_3')).toBe('graph_1-graph-top-ghost-right');
    });

    it('bows transition arcs by how many lanes they span, not by emission order', () => {
        const graphNode = makeGraphNode('graph_1', cyclicFourNodeChain);

        const edges = createGraphEdges(graphNode, {orderLanesByVisualPosition: true});

        const offsetOf = (sourceIndex: number, targetIndex: number): number =>
            edges.find((edge) => edge.id === `graph_1-transition-${sourceIndex}-${targetIndex}`)!.data!
                .offset as number;

        // Visual order is [0, 3, 2, 1], so 0->3, 3->2 and 2->1 are all adjacent hops. They occupy
        // disjoint horizontal ranges, so they must bow identically — a running counter would fan
        // them to three different heights and read as arcs crossing at random.
        expect(offsetOf(0, 3)).toBe(0);
        expect(offsetOf(3, 2)).toBe(0);
        expect(offsetOf(2, 1)).toBe(0);

        // 1->0 is the cycle's return arc. Back arcs sweep the full width whatever they span, so
        // span must NOT lift them — bowing this one by its span pushed it clear out of the
        // container and up past the dispatcher node. It stacks by count instead, and as the only
        // back arc here it sits at the base height.
        expect(offsetOf(1, 0)).toBe(0);
    });

    it('also keeps declaration-order handle grouping when orderLanesByVisualPosition is explicitly false', () => {
        const graphNode = makeGraphNode('graph_1', cyclicFourNodeChain);

        const edges = createGraphEdges(graphNode, {orderLanesByVisualPosition: false});

        expect(sourceHandleForTask(edges, 'task_0')).toBe('graph_1-graph-top-ghost-left');
        expect(sourceHandleForTask(edges, 'task_1')).toBe('graph_1-graph-top-ghost-left');
        expect(sourceHandleForTask(edges, 'task_2')).toBe('graph_1-graph-top-ghost-bottom');
        expect(sourceHandleForTask(edges, 'task_3')).toBe('graph_1-graph-top-ghost-right');
    });

    it('follows the visual (topological) lane order when orderLanesByVisualPosition is true (ELK path) — the regression this fixes', () => {
        const graphNode = makeGraphNode('graph_1', cyclicFourNodeChain);

        const edges = createGraphEdges(graphNode, {orderLanesByVisualPosition: true});

        // Visual order [0,3,2,1]: left=[0,3], middle=2, right=[1] — node_1's declared index (1)
        // would put it in the LEFT group under raw declaration order, but node_1 renders as the
        // RIGHTMOST populated lane, so its edge must use the right handle or ReactFlow's
        // smoothstep router jogs it across the whole frame from the wrong shared handle,
        // producing the empty boxy detour the code review caught.
        expect(sourceHandleForTask(edges, 'task_0')).toBe('graph_1-graph-top-ghost-left');
        expect(sourceHandleForTask(edges, 'task_3')).toBe('graph_1-graph-top-ghost-left');
        expect(sourceHandleForTask(edges, 'task_2')).toBe('graph_1-graph-top-ghost-bottom');
        expect(sourceHandleForTask(edges, 'task_1')).toBe('graph_1-graph-top-ghost-right');
    });

    it('leaves the bottom-ghost handle side following the same visual grouping (top and bottom must agree, or the jog reappears on the exit edge)', () => {
        const graphNode = makeGraphNode('graph_1', cyclicFourNodeChain);

        const edges = createGraphEdges(graphNode, {orderLanesByVisualPosition: true});

        const targetHandleForTask = (taskName: string): string | null | undefined =>
            edges.find((edge) => edge.source === taskName && edge.target === 'graph_1-graph-bottom-ghost')
                ?.targetHandle;

        expect(targetHandleForTask('task_1')).toBe('graph_1-graph-bottom-ghost-right');
    });

    it('should classify transition arcs against the visual order', () => {
        const graphNode = makeGraphNode('graph_1', cyclicFourNodeChain);

        const edges = createGraphEdges(graphNode, {orderLanesByVisualPosition: true});

        const kindOf = (sourceIndex: number, targetIndex: number): string =>
            edges.find((edge) => edge.id === `graph_1-transition-${sourceIndex}-${targetIndex}`)!.data!.kind as string;

        expect(kindOf(0, 3)).toBe('forward');
        expect(kindOf(3, 2)).toBe('forward');
        expect(kindOf(2, 1)).toBe('forward');
        expect(kindOf(1, 0)).toBe('back');
    });

    // `includeTransitionEdges` and `orderLanesByVisualPosition` used to be two independently
    // settable options; {includeTransitionEdges: true, orderLanesByVisualPosition: false} was a
    // reachable state where transition arcs were classified against the visual order while lanes
    // still rendered their shared top/bottom-ghost handle in declaration order — the arcs then
    // described an ordering the lanes did not have. Now there is only ONE flag, so lane handle
    // grouping and transition arc classification can never disagree: this test exercises both
    // from the SAME `createGraphEdges` call to pin that they move together.
    it('keeps lane handle grouping and transition arc classification in lockstep for the same call', () => {
        const graphNode = makeGraphNode('graph_1', cyclicFourNodeChain);

        const edges = createGraphEdges(graphNode, {orderLanesByVisualPosition: true});

        // Visual order [0,3,2,1]: node_1 (declared index 1) renders as the RIGHTMOST lane...
        expect(sourceHandleForTask(edges, 'task_1')).toBe('graph_1-graph-top-ghost-right');

        // ...and its incoming transition arc is classified against that SAME visual order, not
        // declaration order (declaration order would make node_1 -> node_0 a forward hop).
        const kindOf = (sourceIndex: number, targetIndex: number): string =>
            edges.find((edge) => edge.id === `graph_1-transition-${sourceIndex}-${targetIndex}`)!.data!.kind as string;

        expect(kindOf(1, 0)).toBe('back');
    });
});

describe('getGraphNodeSide', () => {
    const tasks = [
        {
            name: 'graph_1',
            parameters: {
                nodes: [
                    {name: 'node_0', tasks: [{name: 'task_0'}]},
                    {name: 'node_1', tasks: [{name: 'task_1'}]},
                    {name: 'node_2', tasks: [{name: 'task_2'}]},
                ],
            },
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
        } as any,
    ];

    it('should return left for the first node', () => {
        expect(getGraphNodeSide('task_0', tasks, 'graph_1')).toBe('left');
    });

    it('should return bottom for a middle node', () => {
        expect(getGraphNodeSide('task_1', tasks, 'graph_1')).toBe('bottom');
    });

    it('should return right for the last node', () => {
        expect(getGraphNodeSide('task_2', tasks, 'graph_1')).toBe('right');
    });

    it('should return right when the parent graph task cannot be found', () => {
        expect(getGraphNodeSide('task_0', [], 'graph_missing')).toBe('right');
    });

    it('should return right when the task is not found in any node', () => {
        expect(getGraphNodeSide('unknown_task', tasks, 'graph_1')).toBe('right');
    });

    it('should keep declaration-order sides without the orderLanesByVisualPosition flag, even when the topological order differs (dagre contract)', () => {
        // Declaration order 0,1,2,3, but the transitions form the chain node_0 -> node_3 ->
        // node_2 -> node_1, so the visual (topological) order is node_0, node_3, node_2, node_1 —
        // node_1 is visually last but declared second. Dagre must still see it as a middle lane.
        const permutedTasks = [
            {
                name: 'graph_1',
                parameters: {
                    nodes: [
                        {name: 'node_0', next: "'node_3'", tasks: [{name: 'task_0'}]},
                        {name: 'node_1', next: "'node_0'", tasks: [{name: 'task_1'}]},
                        {name: 'node_2', next: "'node_1'", tasks: [{name: 'task_2'}]},
                        {name: 'node_3', next: "'node_2'", tasks: [{name: 'task_3'}]},
                    ],
                },
                // eslint-disable-next-line @typescript-eslint/no-explicit-any
            } as any,
        ];

        expect(getGraphNodeSide('task_0', permutedTasks, 'graph_1')).toBe('left');
        expect(getGraphNodeSide('task_1', permutedTasks, 'graph_1')).toBe('bottom');
        expect(getGraphNodeSide('task_2', permutedTasks, 'graph_1')).toBe('bottom');
        expect(getGraphNodeSide('task_3', permutedTasks, 'graph_1')).toBe('right');
    });

    it('should follow the visual (topological) order when orderLanesByVisualPosition is true (ELK path)', () => {
        const permutedTasks = [
            {
                name: 'graph_1',
                parameters: {
                    nodes: [
                        {name: 'node_0', next: "'node_3'", tasks: [{name: 'task_0'}]},
                        {name: 'node_1', next: "'node_0'", tasks: [{name: 'task_1'}]},
                        {name: 'node_2', next: "'node_1'", tasks: [{name: 'task_2'}]},
                        {name: 'node_3', next: "'node_2'", tasks: [{name: 'task_3'}]},
                    ],
                },
                // eslint-disable-next-line @typescript-eslint/no-explicit-any
            } as any,
        ];

        // Visual order node_0, node_3, node_2, node_1: node_0 is leftmost, node_1 is rightmost.
        expect(getGraphNodeSide('task_0', permutedTasks, 'graph_1', true)).toBe('left');
        expect(getGraphNodeSide('task_3', permutedTasks, 'graph_1', true)).toBe('bottom');
        expect(getGraphNodeSide('task_2', permutedTasks, 'graph_1', true)).toBe('bottom');
        expect(getGraphNodeSide('task_1', permutedTasks, 'graph_1', true)).toBe('right');
    });
});
