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
