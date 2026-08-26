import {type Node} from '@xyflow/react';
import {describe, expect, it} from 'vitest';

import getExecutedEdgeStatus, {resolveTestStateNodeName} from './getExecutedEdgeStatus';

const taskNode = (workflowNodeName: string): Node => ({
    data: {workflowNodeName},
    id: workflowNodeName,
    position: {x: 0, y: 0},
});

const ghostNode = (taskDispatcherId: string, suffix: string): Node => ({
    data: {taskDispatcherId},
    id: `${taskDispatcherId}-${suffix}`,
    position: {x: 0, y: 0},
    type: 'taskDispatcherTopGhostNode',
});

describe('resolveTestStateNodeName', () => {
    it('prefers workflowNodeName, falls back to taskDispatcherId, then id', () => {
        expect(resolveTestStateNodeName(taskNode('logger_1'))).toBe('logger_1');
        expect(resolveTestStateNodeName(ghostNode('condition_1', 'condition-top-ghost'))).toBe('condition_1');
        expect(resolveTestStateNodeName({data: {}, id: 'raw-id', position: {x: 0, y: 0}})).toBe('raw-id');
        expect(resolveTestStateNodeName(undefined)).toBeUndefined();
    });
});

describe('getExecutedEdgeStatus', () => {
    it('marks an edge COMPLETED when both endpoints completed', () => {
        const states = {logger_1: {status: 'COMPLETED' as const}, var_1: {status: 'COMPLETED' as const}};

        expect(getExecutedEdgeStatus(taskNode('var_1'), taskNode('logger_1'), states)).toBe('COMPLETED');
    });

    it('marks the incoming edge of a failed node FAILED', () => {
        const states = {http_1: {status: 'FAILED' as const}, var_1: {status: 'COMPLETED' as const}};

        expect(getExecutedEdgeStatus(taskNode('var_1'), taskNode('http_1'), states)).toBe('FAILED');
    });

    it('colors dispatcher plumbing through ghost nodes', () => {
        const states = {condition_1: {status: 'COMPLETED' as const}, logger_3: {status: 'COMPLETED' as const}};

        expect(
            getExecutedEdgeStatus(ghostNode('condition_1', 'condition-top-ghost'), taskNode('logger_3'), states)
        ).toBe('COMPLETED');
    });

    it('keeps the untaken branch gray: child without state into an executed ghost', () => {
        const states = {condition_1: {status: 'COMPLETED' as const}};

        expect(
            getExecutedEdgeStatus(taskNode('logger_2'), ghostNode('condition_1', 'condition-bottom-ghost'), states)
        ).toBeUndefined();
    });

    it('is neutral while the target is still running or has no state', () => {
        const states = {logger_1: {status: 'RUNNING' as const}, var_1: {status: 'COMPLETED' as const}};

        expect(getExecutedEdgeStatus(taskNode('var_1'), taskNode('logger_1'), states)).toBeUndefined();
        expect(getExecutedEdgeStatus(taskNode('var_1'), taskNode('missing'), states)).toBeUndefined();
    });

    it('treats the trigger as completed once anything ran, so its outgoing edge colors', () => {
        const triggerNode: Node = {
            data: {trigger: true, workflowNodeName: 'trigger_1'},
            id: 'trigger_1',
            position: {x: 0, y: 0},
        };

        const states = {var_1: {status: 'COMPLETED' as const}};

        expect(getExecutedEdgeStatus(triggerNode, taskNode('var_1'), states)).toBe('COMPLETED');

        expect(getExecutedEdgeStatus(triggerNode, taskNode('var_1'), {})).toBeUndefined();
    });
});

describe('getExecutedEdgeStatus graph transition arm', () => {
    const nodeExecution = (nodeName: string, startedAtSecond: number, status = 'COMPLETED') => ({
        nodeName,
        startDate: new Date(`2024-01-01T10:00:${String(startedAtSecond).padStart(2, '0')}`),
        status,
    });

    // A conditional fan-out: `node_a` routed to `node_b`, which then routed on to `node_c`. Every
    // one of the three completed, so "both endpoints ran" would light `node_a -> node_c` too.
    const fanOutExecutions = [nodeExecution('node_a', 1), nodeExecution('node_b', 2), nodeExecution('node_c', 3)];

    it('marks a transition COMPLETED when its target was dispatched right after its source', () => {
        expect(
            getExecutedEdgeStatus(
                undefined,
                undefined,
                {},
                {
                    from: 'node_a',
                    nodeExecutions: fanOutExecutions,
                    to: 'node_b',
                }
            )
        ).toBe('COMPLETED');
    });

    it('leaves an untaken sibling transition neutral even though its target completed later', () => {
        expect(
            getExecutedEdgeStatus(
                undefined,
                undefined,
                {},
                {
                    from: 'node_a',
                    nodeExecutions: fanOutExecutions,
                    to: 'node_c',
                }
            )
        ).toBeUndefined();
    });

    it('orders executions by startDate rather than trusting the order it was handed', () => {
        expect(
            getExecutedEdgeStatus(
                undefined,
                undefined,
                {},
                {
                    from: 'node_a',
                    nodeExecutions: [
                        nodeExecution('node_c', 3),
                        nodeExecution('node_b', 2),
                        nodeExecution('node_a', 1),
                    ],
                    to: 'node_b',
                }
            )
        ).toBe('COMPLETED');
    });

    it('marks a self transition COMPLETED across consecutive visits of one node', () => {
        expect(
            getExecutedEdgeStatus(
                undefined,
                undefined,
                {},
                {
                    from: 'node_a',
                    nodeExecutions: [nodeExecution('node_a', 1), nodeExecution('node_a', 2)],
                    to: 'node_a',
                }
            )
        ).toBe('COMPLETED');
    });

    it('paints a transition FAILED when the visit it led to failed', () => {
        expect(
            getExecutedEdgeStatus(
                undefined,
                undefined,
                {},
                {
                    from: 'node_a',
                    nodeExecutions: [nodeExecution('node_a', 1), nodeExecution('node_b', 2, 'FAILED')],
                    to: 'node_b',
                }
            )
        ).toBe('FAILED');
    });

    it('prefers FAILED over COMPLETED when a cycle took the same transition both ways', () => {
        expect(
            getExecutedEdgeStatus(
                undefined,
                undefined,
                {},
                {
                    from: 'node_a',
                    nodeExecutions: [
                        nodeExecution('node_a', 1),
                        nodeExecution('node_b', 2),
                        nodeExecution('node_a', 3),
                        nodeExecution('node_b', 4, 'FAILED'),
                    ],
                    to: 'node_b',
                }
            )
        ).toBe('FAILED');
    });

    it('stays neutral while the visit it led to is still running', () => {
        expect(
            getExecutedEdgeStatus(
                undefined,
                undefined,
                {},
                {
                    from: 'node_a',
                    nodeExecutions: [nodeExecution('node_a', 1), nodeExecution('node_b', 2, 'STARTED')],
                    to: 'node_b',
                }
            )
        ).toBeUndefined();
    });

    it('stays neutral for a dynamic transition, whose expression names no node', () => {
        expect(
            getExecutedEdgeStatus(
                undefined,
                undefined,
                {},
                {
                    from: 'node_a',
                    nodeExecutions: fanOutExecutions,
                    to: "=nextStep + '_1'",
                }
            )
        ).toBeUndefined();
    });

    it('stays neutral when an endpoint has no name, which matches no dispatch', () => {
        const namelessExecutions = [nodeExecution('', 1), nodeExecution('', 2)];

        expect(
            getExecutedEdgeStatus(undefined, undefined, {}, {from: '', nodeExecutions: namelessExecutions, to: ''})
        ).toBeUndefined();
    });

    it('never consults node states in the graph arm, so a completed pair alone proves nothing', () => {
        const states = {node_a: {status: 'COMPLETED' as const}, node_c: {status: 'COMPLETED' as const}};

        expect(
            getExecutedEdgeStatus(taskNode('node_a'), taskNode('node_c'), states, {
                from: 'node_a',
                nodeExecutions: fanOutExecutions,
                to: 'node_c',
            })
        ).toBeUndefined();
    });
});
