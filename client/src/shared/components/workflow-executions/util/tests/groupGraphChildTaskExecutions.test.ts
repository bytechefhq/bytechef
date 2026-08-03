import {TaskExecution} from '@/shared/middleware/automation/workflow/execution';
import {describe, expect, it} from 'vitest';

import {groupGraphChildTaskExecutions, isGraphTaskExecution} from '../groupGraphChildTaskExecutions';

let nextId = 1;

function createNodeTaskExecution(
    nodeName: string,
    taskNumber: number,
    overrides: Partial<TaskExecution> = {}
): TaskExecution {
    return {
        id: String(nextId++),
        priority: 1,
        startDate: new Date('2024-01-01T10:00:00'),
        status: 'COMPLETED',
        taskNumber,
        title: `${nodeName} task`,
        type: 'test/v1/testAction',
        workflowTask: {
            name: `${nodeName}_task_${taskNumber}`,
            parameters: {__node: nodeName},
            type: 'test/v1/testAction',
        },
        ...overrides,
    } as TaskExecution;
}

describe('isGraphTaskExecution', () => {
    it('should return true for a graph task execution type', () => {
        expect(isGraphTaskExecution({type: 'graph/v1'} as TaskExecution)).toBe(true);
    });

    it('should return false for a non-graph task execution type', () => {
        expect(isGraphTaskExecution({type: 'loop/v1'} as TaskExecution)).toBe(false);
    });

    it('should return false when type is missing', () => {
        expect(isGraphTaskExecution({} as TaskExecution)).toBe(false);
    });
});

describe('groupGraphChildTaskExecutions', () => {
    it('should return an empty array for no children', () => {
        expect(groupGraphChildTaskExecutions([])).toEqual([]);
    });

    it('should group consecutive same-node tasks into a single visit', () => {
        const children = [createNodeTaskExecution('node_a', 1), createNodeTaskExecution('node_a', 2)];

        const visits = groupGraphChildTaskExecutions(children);

        expect(visits).toHaveLength(1);
        expect(visits[0]).toMatchObject({nodeName: 'node_a', visitNumber: 1});
        expect(visits[0].taskExecutions).toHaveLength(2);
    });

    it('should start a new visit when the node changes', () => {
        const children = [createNodeTaskExecution('node_a', 1), createNodeTaskExecution('node_b', 1)];

        const visits = groupGraphChildTaskExecutions(children);

        expect(visits.map((visit) => visit.nodeName)).toEqual(['node_a', 'node_b']);
        expect(visits.map((visit) => visit.visitNumber)).toEqual([1, 1]);
    });

    it('should start a new visit for a self-loop revisit of the same node', () => {
        const children = [
            createNodeTaskExecution('node_a', 1),
            createNodeTaskExecution('node_a', 1),
            createNodeTaskExecution('node_a', 1),
        ];

        const visits = groupGraphChildTaskExecutions(children);

        expect(visits).toHaveLength(3);
        expect(visits.map((visit) => visit.visitNumber)).toEqual([1, 2, 3]);
    });

    it('should preserve transition order across interleaved revisits', () => {
        const children = [
            createNodeTaskExecution('node_a', 1),
            createNodeTaskExecution('node_a', 2),
            createNodeTaskExecution('node_b', 1),
            createNodeTaskExecution('node_a', 1),
        ];

        const visits = groupGraphChildTaskExecutions(children);

        expect(visits.map((visit) => `${visit.nodeName}-${visit.visitNumber}`)).toEqual([
            'node_a-1',
            'node_b-1',
            'node_a-2',
        ]);
        expect(visits[0].taskExecutions).toHaveLength(2);
    });

    it('should fall back to the task name when the __node stamp is missing', () => {
        const orphanTask = {
            id: '99',
            priority: 1,
            startDate: new Date('2024-01-01T10:00:00'),
            status: 'COMPLETED',
            taskNumber: 1,
            type: 'test/v1/testAction',
            workflowTask: {
                name: 'orphanTask',
                type: 'test/v1/testAction',
            },
        } as TaskExecution;

        const visits = groupGraphChildTaskExecutions([orphanTask]);

        expect(visits).toHaveLength(1);
        expect(visits[0].nodeName).toBe('orphanTask');
    });
});
