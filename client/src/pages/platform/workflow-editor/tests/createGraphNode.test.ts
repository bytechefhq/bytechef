import {NodeDataType} from '@/shared/types';
import {Node} from '@xyflow/react';
import {describe, expect, it} from 'vitest';

import createGraphNode from '../utils/createGraphNode';
import {TASK_DISPATCHER_CONFIG} from '../utils/taskDispatcherConfig';

function makeGraphTaskNode(
    id: string,
    nodes: Array<{name: string; tasks: Array<{name: string; type?: string}>}> = []
): Node {
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

describe('createGraphNode', () => {
    it('should create a top ghost and a bottom ghost node', () => {
        const graphTaskNode = makeGraphTaskNode('graph_1');

        const allNodes = createGraphNode({allNodes: [graphTaskNode], graphId: 'graph_1'});

        expect(allNodes.some((node) => node.id === 'graph_1-graph-top-ghost')).toBe(true);
        expect(allNodes.some((node) => node.id === 'graph_1-graph-bottom-ghost')).toBe(true);
    });

    it('should always create a trailing add-node placeholder at nodeIndex = nodes.length', () => {
        const graphTaskNode = makeGraphTaskNode('graph_1', [{name: 'node_0', tasks: [{name: 'task_1'}]}]);

        const allNodes = createGraphNode({allNodes: [graphTaskNode], graphId: 'graph_1'});

        const trailingPlaceholder = allNodes.find((node) => node.id === 'graph_1-graph-node-1-placeholder-0');

        expect(trailingPlaceholder).toBeDefined();
        expect(trailingPlaceholder!.type).toBe('placeholder');
        expect((trailingPlaceholder!.data as Record<string, unknown>).nodeIndex).toBe(1);
    });

    it('should create a placeholder for an empty node lane', () => {
        const graphTaskNode = makeGraphTaskNode('graph_1', [{name: 'node_0', tasks: []}]);

        const allNodes = createGraphNode({allNodes: [graphTaskNode], graphId: 'graph_1'});

        const emptyLanePlaceholder = allNodes.find((node) => node.id === 'graph_1-graph-node-0-placeholder-0');

        expect(emptyLanePlaceholder).toBeDefined();
        expect((emptyLanePlaceholder!.data as Record<string, unknown>).nodeIndex).toBe(0);
    });

    it('should NOT create a lane placeholder for a non-empty node — only the trailing add-node one', () => {
        const graphTaskNode = makeGraphTaskNode('graph_1', [{name: 'node_0', tasks: [{name: 'task_1'}]}]);

        const allNodes = createGraphNode({allNodes: [graphTaskNode], graphId: 'graph_1'});

        const placeholders = allNodes.filter((node) => node.type === 'placeholder');

        expect(placeholders).toHaveLength(1);
        expect(placeholders[0].id).toBe('graph_1-graph-node-1-placeholder-0');
    });

    it('should create one placeholder per empty node, keeping non-empty nodes lane-placeholder-free', () => {
        const graphTaskNode = makeGraphTaskNode('graph_1', [
            {name: 'node_0', tasks: []},
            {name: 'node_1', tasks: [{name: 'task_1'}]},
            {name: 'node_2', tasks: []},
        ]);

        const allNodes = createGraphNode({allNodes: [graphTaskNode], graphId: 'graph_1'});

        const placeholderIds = allNodes.filter((node) => node.type === 'placeholder').map((node) => node.id);

        expect(placeholderIds).toEqual(
            expect.arrayContaining([
                'graph_1-graph-node-0-placeholder-0',
                'graph_1-graph-node-2-placeholder-0',
                'graph_1-graph-node-3-placeholder-0',
            ])
        );
        expect(placeholderIds).not.toContain('graph_1-graph-node-1-placeholder-0');
        expect(placeholderIds).toHaveLength(3);
    });

    it('should propagate isNested onto the bottom ghost', () => {
        const graphTaskNode = makeGraphTaskNode('graph_1');

        const allNodes = createGraphNode({allNodes: [graphTaskNode], graphId: 'graph_1', isNested: true});

        const bottomGhost = allNodes.find((node) => node.id === 'graph_1-graph-bottom-ghost');

        expect((bottomGhost!.data as Record<string, unknown>).isNestedBottomGhost).toBe(true);
    });

    it('should insert the auxiliary nodes right after the graph task node', () => {
        const graphTaskNode = makeGraphTaskNode('graph_1', [{name: 'node_0', tasks: []}]);
        const followingNode: Node = {data: {}, id: 'task_after', position: {x: 0, y: 0}, type: 'workflow'};

        const allNodes = createGraphNode({allNodes: [graphTaskNode, followingNode], graphId: 'graph_1'});

        expect(allNodes[0].id).toBe('graph_1');
        expect(allNodes[1].id).toBe('graph_1-graph-top-ghost');
        expect(allNodes[allNodes.length - 1].id).toBe('task_after');
    });

    describe('mint -> parse round trip (Task 1 placeholder-id convention)', () => {
        it('should round-trip an empty lane placeholder through extractContextFromPlaceholder', () => {
            const graphTaskNode = makeGraphTaskNode('graph_1', [{name: 'node_0', tasks: []}]);

            const allNodes = createGraphNode({allNodes: [graphTaskNode], graphId: 'graph_1'});

            const placeholder = allNodes.find((node) => node.id === 'graph_1-graph-node-0-placeholder-0')!;

            const context = TASK_DISPATCHER_CONFIG.graph.extractContextFromPlaceholder(placeholder.id);

            expect(context).toEqual({nodeIndex: 0, taskDispatcherId: 'graph_1'});
        });

        it('should round-trip the trailing add-node placeholder through extractContextFromPlaceholder', () => {
            const graphTaskNode = makeGraphTaskNode('graph_1', [
                {name: 'node_0', tasks: [{name: 'task_1'}]},
                {name: 'node_1', tasks: [{name: 'task_2'}]},
            ]);

            const allNodes = createGraphNode({allNodes: [graphTaskNode], graphId: 'graph_1'});

            const trailingPlaceholder = allNodes.find((node) => node.id === 'graph_1-graph-node-2-placeholder-0')!;

            const context = TASK_DISPATCHER_CONFIG.graph.extractContextFromPlaceholder(trailingPlaceholder.id);

            expect(context).toEqual({nodeIndex: 2, taskDispatcherId: 'graph_1'});
        });

        it('should round-trip a multi-digit node index', () => {
            const nodes = Array.from({length: 13}, (_, index) => ({
                name: `node_${index}`,
                tasks: index === 12 ? [] : [{name: `task_${index}`}],
            }));
            const graphTaskNode = makeGraphTaskNode('graph_1', nodes);

            const allNodes = createGraphNode({allNodes: [graphTaskNode], graphId: 'graph_1'});

            // node_12 is empty, so it gets a lane placeholder at nodeIndex 12
            const placeholder = allNodes.find((node) => node.id === 'graph_1-graph-node-12-placeholder-0')!;

            expect(placeholder).toBeDefined();

            const context = TASK_DISPATCHER_CONFIG.graph.extractContextFromPlaceholder(placeholder.id);

            expect(context).toEqual({nodeIndex: 12, taskDispatcherId: 'graph_1'});

            // And the trailing add-node placeholder lands at nodeIndex 13 (multi-digit too)
            const trailingPlaceholder = allNodes.find((node) => node.id === 'graph_1-graph-node-13-placeholder-0')!;

            expect(trailingPlaceholder).toBeDefined();

            const trailingContext = TASK_DISPATCHER_CONFIG.graph.extractContextFromPlaceholder(trailingPlaceholder.id);

            expect(trailingContext).toEqual({nodeIndex: 13, taskDispatcherId: 'graph_1'});
        });
    });
});
