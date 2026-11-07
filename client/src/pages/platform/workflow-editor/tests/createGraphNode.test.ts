import {GRAPH_FRAME_NODE_TYPE, GRAPH_START_NODE_TYPE} from '@/shared/constants';
import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {NodeDataType} from '@/shared/types';
import {Node} from '@xyflow/react';
import {describe, expect, it} from 'vitest';

import createGraphNode from '../utils/createGraphNode';
import {
    GRAPH_FRAME_HEADER_HEIGHT,
    GRAPH_FRAME_MIN_HEIGHT,
    GRAPH_FRAME_MIN_WIDTH,
    GRAPH_FRAME_PADDING,
} from '../utils/graph/graphFrameGeometry';
import {TASK_DISPATCHER_CONFIG} from '../utils/taskDispatcherConfig';

function makeGraphTaskNode(id: string, nodes: Array<WorkflowTask> = []): Node {
    return {
        data: {
            componentName: 'graph',
            parameters: {
                maxTransitions: 100,
                nodes,
                startNode: nodes[1]?.name,
                transitions: [],
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

describe('createGraphNode', () => {
    it('should create no ghost bars, using the frame as the chain anchor instead', () => {
        const graphTaskNode = makeGraphTaskNode('graph_1');

        const allNodes = createGraphNode({allNodes: [graphTaskNode], graphId: 'graph_1'});

        expect(allNodes.some((node) => node.id.endsWith('-ghost'))).toBe(false);
        expect(allNodes.some((node) => node.id === 'graph_1-graph-frame')).toBe(true);
    });

    it('should create a non-interactive, minimum-sized frame node', () => {
        const graphTaskNode = makeGraphTaskNode('graph_1', twoNodes);

        const allNodes = createGraphNode({allNodes: [graphTaskNode], graphId: 'graph_1'});

        const frameNode = allNodes.find((node) => node.id === 'graph_1-graph-frame');

        expect(frameNode).toBeDefined();
        expect(frameNode!.type).toBe(GRAPH_FRAME_NODE_TYPE);
        expect(frameNode!.draggable).toBe(false);
        expect(frameNode!.selectable).toBe(false);
        expect(frameNode!.style).toMatchObject({pointerEvents: 'all'});
        expect(frameNode!.width).toBe(GRAPH_FRAME_MIN_WIDTH);
        expect(frameNode!.height).toBe(GRAPH_FRAME_MIN_HEIGHT);
        expect(frameNode!.data).toMatchObject({
            graphFrame: {graphId: 'graph_1', height: GRAPH_FRAME_MIN_HEIGHT, width: GRAPH_FRAME_MIN_WIDTH},
            graphId: 'graph_1',
            taskDispatcherId: 'graph_1',
        });
    });

    it('should create a Start pill parented to the frame at the frame content origin', () => {
        const graphTaskNode = makeGraphTaskNode('graph_1', twoNodes);

        const allNodes = createGraphNode({allNodes: [graphTaskNode], graphId: 'graph_1'});

        const startNode = allNodes.find((node) => node.id === 'graph_1-graph-start');

        expect(startNode).toBeDefined();
        expect(startNode!.type).toBe(GRAPH_START_NODE_TYPE);
        expect(startNode!.parentId).toBe('graph_1-graph-frame');
        expect(startNode!.draggable).toBe(false);
        expect(startNode!.position).toEqual({x: GRAPH_FRAME_PADDING, y: GRAPH_FRAME_HEADER_HEIGHT});
        expect(startNode!.data).toMatchObject({graphStart: {graphId: 'graph_1'}});
    });

    it('should create exactly one add-node placeholder, hidden and parented to the frame', () => {
        const graphTaskNode = makeGraphTaskNode('graph_1', twoNodes);

        const allNodes = createGraphNode({allNodes: [graphTaskNode], graphId: 'graph_1'});

        const placeholders = allNodes.filter((node) => node.type === 'placeholder');

        expect(placeholders).toHaveLength(1);
        expect(placeholders[0].id).toBe('graph_1-graph-placeholder');
        expect(placeholders[0].parentId).toBe('graph_1-graph-frame');
        expect(placeholders[0].hidden).toBe(true);
        expect(placeholders[0].position).toEqual({x: 0, y: 0});
        expect(placeholders[0].data).toMatchObject({graphId: 'graph_1', label: '+', taskDispatcherId: 'graph_1'});
    });

    it('should mint the same auxiliary nodes regardless of how many members are declared', () => {
        const emptyGraphNodes = createGraphNode({allNodes: [makeGraphTaskNode('graph_1', [])], graphId: 'graph_1'});
        const populatedGraphNodes = createGraphNode({
            allNodes: [makeGraphTaskNode('graph_1', twoNodes)],
            graphId: 'graph_1',
        });

        expect(emptyGraphNodes.map((node) => node.id)).toEqual(populatedGraphNodes.map((node) => node.id));
    });

    it('should not touch the member task nodes - the layout pre-pass parents and positions them', () => {
        const graphTaskNode = makeGraphTaskNode('graph_1', twoNodes);
        const memberNode: Node = {data: {}, id: 'task_a', position: {x: 0, y: 0}, type: 'workflow'};

        const allNodes = createGraphNode({allNodes: [graphTaskNode, memberNode], graphId: 'graph_1'});

        expect(allNodes.find((node) => node.id === 'task_a')).toBe(memberNode);
        expect(allNodes.find((node) => node.id === 'task_b')).toBeUndefined();
    });

    it('should propagate isNested onto the frame, which is what the enclosing chain reads', () => {
        const graphTaskNode = makeGraphTaskNode('graph_1');

        const allNodes = createGraphNode({allNodes: [graphTaskNode], graphId: 'graph_1', isNested: true});

        const frameNode = allNodes.find((node) => node.id === 'graph_1-graph-frame');

        expect((frameNode!.data as Record<string, unknown>).isNestedBottomGhost).toBe(true);
    });

    it('should insert the auxiliary nodes right after the graph task node, in chain order', () => {
        const graphTaskNode = makeGraphTaskNode('graph_1', twoNodes);
        const followingNode: Node = {data: {}, id: 'task_after', position: {x: 0, y: 0}, type: 'workflow'};

        const allNodes = createGraphNode({allNodes: [graphTaskNode, followingNode], graphId: 'graph_1'});

        expect(allNodes.map((node) => node.id)).toEqual([
            'graph_1',
            'graph_1-graph-frame',
            'graph_1-graph-start',
            'graph_1-graph-placeholder',
            'task_after',
        ]);
    });

    describe('mint -> parse round trip (placeholder-id convention)', () => {
        it('should round-trip the add-node placeholder through extractContextFromPlaceholder', () => {
            const graphTaskNode = makeGraphTaskNode('graph_1', twoNodes);

            const allNodes = createGraphNode({allNodes: [graphTaskNode], graphId: 'graph_1'});

            const placeholderNode = allNodes.find((node) => node.id === 'graph_1-graph-placeholder')!;

            const context = TASK_DISPATCHER_CONFIG.graph.extractContextFromPlaceholder(placeholderNode.id);

            expect(context).toEqual({taskDispatcherId: 'graph_1'});
            expect(context.index).toBeUndefined();
        });

        it('should round-trip regardless of how many nodes are already declared', () => {
            const nodes = Array.from({length: 13}, (_emptySlot, index) => ({name: `task_${index}`, type: 'task/v1'}));
            const graphTaskNode = makeGraphTaskNode('graph_1', nodes);

            const allNodes = createGraphNode({allNodes: [graphTaskNode], graphId: 'graph_1'});

            const placeholderNode = allNodes.find((node) => node.id === 'graph_1-graph-placeholder')!;

            expect(placeholderNode).toBeDefined();

            const context = TASK_DISPATCHER_CONFIG.graph.extractContextFromPlaceholder(placeholderNode.id);

            expect(context).toEqual({taskDispatcherId: 'graph_1'});
            expect(context.index).toBeUndefined();
        });
    });
});
