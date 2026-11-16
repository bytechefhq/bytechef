import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {NodeDataType} from '@/shared/types';
import {Node} from '@xyflow/react';
import {describe, expect, it} from 'vitest';

import {
    createBranchExitEdges,
    createSubtaskChainEdges,
    getSubtaskExitNodeId,
} from '../utils/taskDispatcherSubtaskEdges';

function subtask(name: string, type = 'test/v1/action') {
    return {name, parameters: {}, type};
}

function forkJoinSubtask(name = 'forkJoin_1') {
    return subtask(name, 'fork-join/v1');
}

function node(id: string, taskDispatcher: boolean): Node {
    return {data: {taskDispatcher} as unknown as NodeDataType, id, position: {x: 0, y: 0}, type: 'workflow'};
}

describe('getSubtaskExitNodeId', () => {
    it('leaves a dispatcher with subtasks from its bottom ghost', () => {
        expect(getSubtaskExitNodeId(forkJoinSubtask())).toBe('forkJoin_1-forkJoin-bottom-ghost');
        expect(getSubtaskExitNodeId(forkJoinSubtask('fork-join_1'))).toBe('fork-join_1-forkJoin-bottom-ghost');
        expect(getSubtaskExitNodeId(subtask('condition_2', 'condition/v1'))).toBe('condition_2-condition-bottom-ghost');
    });

    it('leaves a task or a childless dispatcher from the node itself', () => {
        expect(getSubtaskExitNodeId(subtask('action_1'))).toBe('action_1');
        expect(getSubtaskExitNodeId(subtask('subflow_1', 'subflow/v1'))).toBe('subflow_1');
    });

    it('reads a custom-named dispatcher from its task type', () => {
        expect(getSubtaskExitNodeId(subtask('deleteSubjects', 'each/v1'))).toBe('deleteSubjects-each-bottom-ghost');
        expect(getSubtaskExitNodeId(subtask('retryAll', 'fork-join/v1'))).toBe('retryAll-forkJoin-bottom-ghost');
    });

    it('trusts the task type over a dispatcher-looking name', () => {
        expect(getSubtaskExitNodeId(subtask('each_1', 'httpClient/v1/get'))).toBe('each_1');
    });

    it('falls back to the name when the task carries no type', () => {
        expect(getSubtaskExitNodeId({name: 'each_1'} as WorkflowTask)).toBe('each_1-each-bottom-ghost');
    });
});

describe('createSubtaskChainEdges', () => {
    it('connects each subtask to the next one', () => {
        expect(createSubtaskChainEdges([subtask('action_1'), subtask('action_2')])).toEqual([
            expect.objectContaining({id: 'action_1=>action_2', source: 'action_1', target: 'action_2'}),
        ]);
    });

    it('connects a nested dispatcher to the next subtask from its bottom ghost', () => {
        const [edge] = createSubtaskChainEdges([forkJoinSubtask(), subtask('action_1')]);

        expect(edge).toMatchObject({
            id: 'forkJoin_1-forkJoin-bottom-ghost=>action_1',
            source: 'forkJoin_1-forkJoin-bottom-ghost',
        });
        expect(edge.sourceHandle).toBeUndefined();
    });

    it('adds the ghost handle when the caller asks for one', () => {
        const [nestedEdge, taskEdge] = createSubtaskChainEdges(
            [forkJoinSubtask(), subtask('action_1'), subtask('action_2')],
            'bottom'
        );

        expect(nestedEdge.sourceHandle).toBe('forkJoin_1-forkJoin-bottom-ghost-bottom');
        expect(taskEdge.sourceHandle).toBeUndefined();
    });

    it('connects a custom-named dispatcher to the next subtask from its bottom ghost', () => {
        const edges = createSubtaskChainEdges(
            [subtask('deleteSubjects', 'each/v1'), subtask('deleteTransfers', 'each/v1')],
            'bottom'
        );

        expect(edges.map((edge) => edge.id)).toEqual(['deleteSubjects-each-bottom-ghost=>deleteTransfers']);
    });
});

describe('createBranchExitEdges', () => {
    it('leaves the branch from a nested dispatcher bottom ghost', () => {
        expect(
            createBranchExitEdges('parent-bottom-ghost', [forkJoinSubtask()], 'left', [node('forkJoin_1', true)])
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

    it('leaves the branch from a custom-named dispatcher bottom ghost', () => {
        const [exitEdge] = createBranchExitEdges(
            'parent-bottom-ghost',
            [subtask('deleteSubjects', 'each/v1')],
            'right',
            [node('deleteSubjects', true)]
        );

        expect(exitEdge.source).toBe('deleteSubjects-each-bottom-ghost');
    });

    it('creates no edge for an empty branch', () => {
        expect(createBranchExitEdges('parent-bottom-ghost', [], 'left', [])).toEqual([]);
    });
});
