import {NodeDataType} from '@/shared/types';
import {Node} from '@xyflow/react';
import {describe, expect, it} from 'vitest';

import {
    createBranchExitEdges,
    createSubtaskChainEdges,
    getSubtaskExitNodeId,
} from '../utils/taskDispatcherSubtaskEdges';

function subtask(name: string) {
    return {name, parameters: {}, type: 'test/v1/action'};
}

function node(id: string, taskDispatcher: boolean): Node {
    return {data: {taskDispatcher} as unknown as NodeDataType, id, position: {x: 0, y: 0}, type: 'workflow'};
}

describe('getSubtaskExitNodeId', () => {
    it('leaves a dispatcher with subtasks from its bottom ghost', () => {
        expect(getSubtaskExitNodeId('forkJoin_1')).toBe('forkJoin_1-forkJoin-bottom-ghost');
        expect(getSubtaskExitNodeId('fork-join_1')).toBe('fork-join_1-forkJoin-bottom-ghost');
        expect(getSubtaskExitNodeId('condition_2')).toBe('condition_2-condition-bottom-ghost');
    });

    it('leaves a task or a childless dispatcher from the node itself', () => {
        expect(getSubtaskExitNodeId('action_1')).toBe('action_1');
        expect(getSubtaskExitNodeId('subflow_1')).toBe('subflow_1');
    });
});

describe('createSubtaskChainEdges', () => {
    it('connects each subtask to the next one', () => {
        expect(createSubtaskChainEdges([subtask('action_1'), subtask('action_2')])).toEqual([
            expect.objectContaining({id: 'action_1=>action_2', source: 'action_1', target: 'action_2'}),
        ]);
    });

    it('connects a nested dispatcher to the next subtask from its bottom ghost', () => {
        const [edge] = createSubtaskChainEdges([subtask('forkJoin_1'), subtask('action_1')]);

        expect(edge).toMatchObject({
            id: 'forkJoin_1-forkJoin-bottom-ghost=>action_1',
            source: 'forkJoin_1-forkJoin-bottom-ghost',
        });
        expect(edge.sourceHandle).toBeUndefined();
    });

    it('adds the ghost handle when the caller asks for one', () => {
        const [nestedEdge, taskEdge] = createSubtaskChainEdges(
            [subtask('forkJoin_1'), subtask('action_1'), subtask('action_2')],
            'bottom'
        );

        expect(nestedEdge.sourceHandle).toBe('forkJoin_1-forkJoin-bottom-ghost-bottom');
        expect(taskEdge.sourceHandle).toBeUndefined();
    });
});

describe('createBranchExitEdges', () => {
    it('leaves the branch from a nested dispatcher bottom ghost', () => {
        expect(
            createBranchExitEdges('parent-bottom-ghost', [subtask('forkJoin_1')], 'left', [node('forkJoin_1', true)])
        ).toEqual([
            expect.objectContaining({
                source: 'forkJoin_1-forkJoin-bottom-ghost',
                target: 'parent-bottom-ghost',
                targetHandle: 'parent-bottom-ghost-left',
            }),
        ]);
    });

    it('leaves the branch from the last task itself', () => {
        expect(
            createBranchExitEdges('parent-bottom-ghost', [subtask('action_1')], 'right', [node('action_1', false)])
        ).toEqual([
            expect.objectContaining({
                source: 'action_1',
                targetHandle: 'parent-bottom-ghost-right',
            }),
        ]);
    });

    it('creates no edge for an empty branch', () => {
        expect(createBranchExitEdges('parent-bottom-ghost', [], 'left', [])).toEqual([]);
    });
});
