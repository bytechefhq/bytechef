import {EDGE_STYLES, GRAPH_START_EDGE_TYPE, GRAPH_TRANSITION_EDGE_TYPE} from '@/shared/constants';
import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {GraphTransitionType, NodeDataType} from '@/shared/types';
import {MarkerType, Node} from '@xyflow/react';
import {describe, expect, it} from 'vitest';

import createGraphEdges from '../utils/createGraphEdges';

// `graph/v1`'s `nodes` is a plain task list (a node IS one task) and every route between members
// is one entry of the explicit `parameters.transitions` edge list.
function makeGraphNode(
    id: string,
    nodes: Array<WorkflowTask> = [],
    transitions: Array<GraphTransitionType> = [],
    startNode?: string
): Node {
    return {
        data: {
            componentName: 'graph',
            parameters: {
                maxTransitions: 100,
                nodes,
                startNode,
                transitions,
            },
        } as unknown as NodeDataType,
        id,
        position: {x: 0, y: 0},
        type: 'workflow',
    };
}

const twoNodes: Array<WorkflowTask> = [
    {name: 'task_a', type: 'task/v1'},
    {name: 'task_b', type: 'task/v1'},
];

const twoTransitions: Array<GraphTransitionType> = [
    {condition: '${task_a.output} == true', from: 'task_a', to: 'task_b'},
    {from: 'task_b', to: '=expression'},
];

describe('createGraphEdges', () => {
    describe('structural chain', () => {
        it('should drop the graph node straight into its frame', () => {
            const graphNode = makeGraphNode('graph_1');

            const edges = createGraphEdges(graphNode);

            const graphToFrame = edges.find(
                (edge) => edge.source === 'graph_1' && edge.target === 'graph_1-graph-frame'
            );

            expect(graphToFrame).toBeDefined();
            expect(graphToFrame!.type).toBe('smoothstep');
            expect(graphToFrame!.style).toEqual(EDGE_STYLES);
            expect(graphToFrame!.targetHandle).toBe('graph_1-graph-frame-top');
        });

        // What LEAVES the frame is not built here: the chain owns whatever follows a dispatcher and
        // builds that edge off the frame, the same way it used to build it off a bottom ghost bar.
        it('should emit no ghost bar edges and leave the frame exit to the chain', () => {
            const graphNode = makeGraphNode('graph_1', twoNodes, twoTransitions, 'task_b');

            const edges = createGraphEdges(graphNode);

            expect(edges.some((edge) => edge.source.endsWith('-ghost') || edge.target.endsWith('-ghost'))).toBe(false);
            expect(edges.some((edge) => edge.source === 'graph_1-graph-frame')).toBe(false);
            expect(edges.some((edge) => edge.source === 'task_b' && edge.target.endsWith('-ghost'))).toBe(false);
        });
    });

    describe('start edge', () => {
        it('should join the Start pill to the declared start node', () => {
            const graphNode = makeGraphNode('graph_1', twoNodes, twoTransitions, 'task_b');

            const edges = createGraphEdges(graphNode);

            const startEdge = edges.find((edge) => edge.type === GRAPH_START_EDGE_TYPE);

            expect(startEdge).toBeDefined();
            expect(startEdge!.source).toBe('graph_1-graph-start');
            expect(startEdge!.sourceHandle).toBe('graph_1-graph-start-source');
            expect(startEdge!.target).toBe('task_b');
            expect(startEdge!.targetHandle).toBe('task_b-graph-transition-target');
        });

        it('should fall back to the first declared node when startNode is unset', () => {
            const graphNode = makeGraphNode('graph_1', twoNodes, twoTransitions);

            const edges = createGraphEdges(graphNode);

            const startEdge = edges.find((edge) => edge.type === GRAPH_START_EDGE_TYPE);

            expect(startEdge!.target).toBe('task_a');
        });

        it('should fall back to the first declared node when startNode is blank', () => {
            // GraphTaskDispatcher.resolveStartNode treats null OR blank as absent, so a blank
            // value must not draw a start edge to a node named ''.
            const graphNode = makeGraphNode('graph_1', twoNodes, twoTransitions, '   ');

            const edges = createGraphEdges(graphNode);

            const startEdge = edges.find((edge) => edge.type === GRAPH_START_EDGE_TYPE);

            expect(startEdge!.target).toBe('task_a');
        });

        // The canvas sets `edgesReconnectable={false}`, so re-pointing the Start edge at another
        // member works only while this per-edge opt-in is present, and only its target end moves.
        // Every other edge stays without it, which keeps the chain edges free of endpoint anchors.
        it('should be the only edge that opts into reconnecting, target end only', () => {
            const graphNode = makeGraphNode('graph_1', twoNodes, twoTransitions, 'task_b');

            const edges = createGraphEdges(graphNode);

            expect(edges.find((edge) => edge.type === GRAPH_START_EDGE_TYPE)!.reconnectable).toBe('target');
            expect(
                edges.filter((edge) => edge.type !== GRAPH_START_EDGE_TYPE).every((edge) => !edge.reconnectable)
            ).toBe(true);
        });

        it('should emit no start edge for a graph with no members', () => {
            const graphNode = makeGraphNode('graph_1');

            const edges = createGraphEdges(graphNode);

            expect(edges.some((edge) => edge.type === GRAPH_START_EDGE_TYPE)).toBe(false);
        });
    });

    describe('transition edges', () => {
        it('should emit one edge per declared transition, keyed by declaration index', () => {
            const graphNode = makeGraphNode('graph_1', twoNodes, twoTransitions, 'task_b');

            const edges = createGraphEdges(graphNode);

            const transitionEdges = edges.filter((edge) => edge.type === GRAPH_TRANSITION_EDGE_TYPE);

            expect(transitionEdges.map((edge) => edge.id)).toEqual(['graph_1-transition-0', 'graph_1-transition-1']);
        });

        it('should point a static transition at its target member', () => {
            const graphNode = makeGraphNode('graph_1', twoNodes, twoTransitions, 'task_b');

            const staticEdge = createGraphEdges(graphNode).find((edge) => edge.id === 'graph_1-transition-0')!;

            expect(staticEdge.source).toBe('task_a');
            expect(staticEdge.sourceHandle).toBe('task_a-graph-transition-source');
            expect(staticEdge.target).toBe('task_b');
            expect(staticEdge.targetHandle).toBe('task_b-graph-transition-target');
            expect(staticEdge.markerEnd).toEqual({type: MarkerType.ArrowClosed});
            expect(staticEdge.zIndex).toBe(5);
            expect(staticEdge.data).toEqual({
                condition: '${task_a.output} == true',
                dangling: false,
                dynamic: false,
                graphId: 'graph_1',
                index: 0,
                to: 'task_b',
            });
        });

        it('should stub a dynamic transition back onto its own source node', () => {
            const graphNode = makeGraphNode('graph_1', twoNodes, twoTransitions, 'task_b');

            const dynamicEdge = createGraphEdges(graphNode).find((edge) => edge.id === 'graph_1-transition-1')!;

            expect(dynamicEdge.source).toBe('task_b');
            expect(dynamicEdge.sourceHandle).toBe('task_b-graph-transition-source');
            expect(dynamicEdge.target).toBe('task_b');
            expect(dynamicEdge.targetHandle).toBe('task_b-graph-transition-dynamic');
            expect(dynamicEdge.data).toEqual({
                condition: undefined,
                dangling: false,
                dynamic: true,
                graphId: 'graph_1',
                index: 1,
                to: '=expression',
            });
        });

        it('should treat a datapill target as dynamic too', () => {
            const graphNode = makeGraphNode('graph_1', twoNodes, [{from: 'task_a', to: '${task_a.next}'}]);

            const dynamicEdge = createGraphEdges(graphNode).find((edge) => edge.id === 'graph_1-transition-0')!;

            expect(dynamicEdge.targetHandle).toBe('task_a-graph-transition-dynamic');
            expect(dynamicEdge.data).toMatchObject({dynamic: true});
        });

        it('should still emit a transition naming no static member, flagged dangling', () => {
            const graphNode = makeGraphNode('graph_1', twoNodes, [
                {from: 'task_a', to: 'task_gone'},
                {from: 'task_gone', to: 'task_b'},
            ]);

            const edges = createGraphEdges(graphNode);

            expect(edges.find((edge) => edge.id === 'graph_1-transition-0')!.data).toMatchObject({dangling: true});
            expect(edges.find((edge) => edge.id === 'graph_1-transition-1')!.data).toMatchObject({dangling: true});
        });

        it('should emit no transition edges when the graph declares none', () => {
            const graphNode = makeGraphNode('graph_1', twoNodes);

            const edges = createGraphEdges(graphNode);

            expect(edges.some((edge) => edge.type === GRAPH_TRANSITION_EDGE_TYPE)).toBe(false);
        });
    });
});
