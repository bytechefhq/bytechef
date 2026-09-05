import {type Edge, type Node} from '@xyflow/react';
import {describe, expect, it} from 'vitest';

import {WorkflowTestNodeStateI} from '../stores/useWorkflowEditorStore';
import resolveGhostBarSideStatuses from './resolveGhostBarSideStatuses';

const TOP_GHOST_ID = 'condition_1-condition-top-ghost';
const BOTTOM_GHOST_ID = 'condition_1-condition-bottom-ghost';

function createGhostNode(id: string): Node {
    return {data: {taskDispatcherId: 'condition_1'}, id, position: {x: 0, y: 0}};
}

function createTaskNode(id: string): Node {
    return {data: {workflowNodeName: id}, id, position: {x: 0, y: 0}};
}

function createNodeStates(entries: Record<string, WorkflowTestNodeStateI['status']>) {
    return Object.fromEntries(Object.entries(entries).map(([nodeName, status]) => [nodeName, {status}])) as Record<
        string,
        WorkflowTestNodeStateI
    >;
}

const NODES = [
    createGhostNode(TOP_GHOST_ID),
    createGhostNode(BOTTOM_GHOST_ID),
    createTaskNode('resultAfter'),
    createTaskNode('resultNotAfter'),
];

const TOP_GHOST_EDGES: Edge[] = [
    {
        id: `${TOP_GHOST_ID}=>resultAfter`,
        source: TOP_GHOST_ID,
        sourceHandle: `${TOP_GHOST_ID}-left`,
        target: 'resultAfter',
    },
    {
        id: `${TOP_GHOST_ID}=>resultNotAfter`,
        source: TOP_GHOST_ID,
        sourceHandle: `${TOP_GHOST_ID}-right`,
        target: 'resultNotAfter',
    },
];

const BOTTOM_GHOST_EDGES: Edge[] = [
    {
        id: `resultAfter=>${BOTTOM_GHOST_ID}`,
        source: 'resultAfter',
        target: BOTTOM_GHOST_ID,
        targetHandle: `${BOTTOM_GHOST_ID}-left`,
    },
    {
        id: `resultNotAfter=>${BOTTOM_GHOST_ID}`,
        source: 'resultNotAfter',
        target: BOTTOM_GHOST_ID,
        targetHandle: `${BOTTOM_GHOST_ID}-right`,
    },
];

describe('resolveGhostBarSideStatuses', () => {
    it('leaves the untaken half of a top bar neutral when only one branch ran', () => {
        const statuses = resolveGhostBarSideStatuses({
            edges: TOP_GHOST_EDGES,
            fallbackStatus: 'COMPLETED',
            ghostNodeId: TOP_GHOST_ID,
            isBottomGhost: false,
            nodes: NODES,
            workflowTestNodeStates: createNodeStates({condition_1: 'COMPLETED', resultNotAfter: 'COMPLETED'}),
        });

        expect(statuses).toEqual({leftStatus: undefined, rightStatus: 'COMPLETED'});
    });

    it('leaves the untaken half of a bottom bar neutral when only one branch ran', () => {
        const statuses = resolveGhostBarSideStatuses({
            edges: BOTTOM_GHOST_EDGES,
            fallbackStatus: 'COMPLETED',
            ghostNodeId: BOTTOM_GHOST_ID,
            isBottomGhost: true,
            nodes: NODES,
            workflowTestNodeStates: createNodeStates({condition_1: 'COMPLETED', resultNotAfter: 'COMPLETED'}),
        });

        expect(statuses).toEqual({leftStatus: undefined, rightStatus: 'COMPLETED'});
    });

    it('paints both halves when every side ran', () => {
        const statuses = resolveGhostBarSideStatuses({
            edges: TOP_GHOST_EDGES,
            fallbackStatus: 'COMPLETED',
            ghostNodeId: TOP_GHOST_ID,
            isBottomGhost: false,
            nodes: NODES,
            workflowTestNodeStates: createNodeStates({
                condition_1: 'COMPLETED',
                resultAfter: 'COMPLETED',
                resultNotAfter: 'COMPLETED',
            }),
        });

        expect(statuses).toEqual({leftStatus: 'COMPLETED', rightStatus: 'COMPLETED'});
    });

    it('keeps both halves neutral when the dispatcher never ran', () => {
        const statuses = resolveGhostBarSideStatuses({
            edges: TOP_GHOST_EDGES,
            fallbackStatus: undefined,
            ghostNodeId: TOP_GHOST_ID,
            isBottomGhost: false,
            nodes: NODES,
            workflowTestNodeStates: {},
        });

        expect(statuses).toEqual({leftStatus: undefined, rightStatus: undefined});
    });

    it('falls back to the dispatcher status on a side with no edges attached', () => {
        const statuses = resolveGhostBarSideStatuses({
            edges: [TOP_GHOST_EDGES[1]],
            fallbackStatus: 'COMPLETED',
            ghostNodeId: TOP_GHOST_ID,
            isBottomGhost: false,
            nodes: NODES,
            workflowTestNodeStates: createNodeStates({condition_1: 'COMPLETED', resultNotAfter: 'COMPLETED'}),
        });

        expect(statuses).toEqual({leftStatus: 'COMPLETED', rightStatus: 'COMPLETED'});
    });

    it('paints a half COMPLETED when the case under it failed, since the data reached it', () => {
        const sharedHandleEdges: Edge[] = [
            TOP_GHOST_EDGES[0],
            {
                id: `${TOP_GHOST_ID}=>resultNotAfter`,
                source: TOP_GHOST_ID,
                sourceHandle: `${TOP_GHOST_ID}-left`,
                target: 'resultNotAfter',
            },
        ];

        const statuses = resolveGhostBarSideStatuses({
            edges: sharedHandleEdges,
            fallbackStatus: 'FAILED',
            ghostNodeId: TOP_GHOST_ID,
            isBottomGhost: false,
            nodes: NODES,
            workflowTestNodeStates: createNodeStates({
                condition_1: 'COMPLETED',
                resultAfter: 'COMPLETED',
                resultNotAfter: 'FAILED',
            }),
        });

        expect(statuses.leftStatus).toBe('COMPLETED');
    });

    it('ignores an edge that only shares the ghost id prefix with the bar handles', () => {
        const nestedGhostEdges: Edge[] = [
            {
                id: `${TOP_GHOST_ID}=>resultAfter`,
                source: TOP_GHOST_ID,
                sourceHandle: `${TOP_GHOST_ID}-bottom`,
                target: 'resultAfter',
            },
        ];

        const statuses = resolveGhostBarSideStatuses({
            edges: nestedGhostEdges,
            fallbackStatus: 'COMPLETED',
            ghostNodeId: TOP_GHOST_ID,
            isBottomGhost: false,
            nodes: NODES,
            workflowTestNodeStates: createNodeStates({condition_1: 'COMPLETED', resultAfter: 'COMPLETED'}),
        });

        expect(statuses).toEqual({leftStatus: 'COMPLETED', rightStatus: 'COMPLETED'});
    });

    it('paints the completed half of a failed parallel join green and leaves the failed half neutral', () => {
        const parallelBottomGhostId = 'parallel_1-parallel-bottom-ghost';

        const parallelNodes: Node[] = [
            {
                data: {taskDispatcherId: 'parallel_1'},
                id: parallelBottomGhostId,
                position: {x: 0, y: 0},
                type: 'taskDispatcherBottomGhostNode',
            },
            createTaskNode('anthropic_1'),
            {data: {disabled: true, workflowNodeName: 'dataStorage_1'}, id: 'dataStorage_1', position: {x: 0, y: 0}},
            createTaskNode('dataStorage_2'),
        ];

        const parallelEdges: Edge[] = [
            {id: 'anthropic_1=>dataStorage_1', source: 'anthropic_1', target: 'dataStorage_1'},
            {
                id: `dataStorage_1=>${parallelBottomGhostId}`,
                source: 'dataStorage_1',
                target: parallelBottomGhostId,
                targetHandle: `${parallelBottomGhostId}-left`,
            },
            {
                id: `dataStorage_2=>${parallelBottomGhostId}`,
                source: 'dataStorage_2',
                target: parallelBottomGhostId,
                targetHandle: `${parallelBottomGhostId}-right`,
            },
        ];

        const statuses = resolveGhostBarSideStatuses({
            edges: parallelEdges,
            fallbackStatus: 'FAILED',
            ghostNodeId: parallelBottomGhostId,
            isBottomGhost: true,
            nodes: parallelNodes,
            workflowTestNodeStates: createNodeStates({
                anthropic_1: 'FAILED',
                dataStorage_2: 'COMPLETED',
                parallel_1: 'FAILED',
            }),
        });

        expect(statuses).toEqual({leftStatus: undefined, rightStatus: 'COMPLETED'});
    });
});
