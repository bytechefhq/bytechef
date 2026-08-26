import {TaskExecution} from '@/shared/middleware/automation/workflow/execution';
import {describe, expect, it} from 'vitest';

import {isGraphTaskExecution, toGraphNodeVisits} from '../toGraphNodeVisits';

let nextId = 1;

function createNodeTaskExecution(nodeName: string, overrides: Partial<TaskExecution> = {}): TaskExecution {
    return {
        id: String(nextId++),
        priority: 1,
        startDate: new Date('2024-01-01T10:00:00'),
        status: 'COMPLETED',
        taskNumber: 1,
        title: `${nodeName} task`,
        type: 'test/v1/testAction',
        workflowTask: {
            name: nodeName,
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

describe('toGraphNodeVisits', () => {
    it('should return an empty array for no children', () => {
        expect(toGraphNodeVisits([])).toEqual([]);
    });

    it('should make one visit out of each child task execution', () => {
        const children = [createNodeTaskExecution('node_a'), createNodeTaskExecution('node_b')];

        const visits = toGraphNodeVisits(children);

        expect(visits.map((visit) => visit.nodeName)).toEqual(['node_a', 'node_b']);
        expect(visits.map((visit) => visit.taskExecution)).toEqual(children);
    });

    it('should number repeat visits of the same node, self-loop included', () => {
        const visits = toGraphNodeVisits([
            createNodeTaskExecution('node_a'),
            createNodeTaskExecution('node_a'),
            createNodeTaskExecution('node_a'),
        ]);

        expect(visits.map((visit) => visit.visitNumber)).toEqual([1, 2, 3]);
    });

    it('should count visits per node while preserving the order the run made them', () => {
        const visits = toGraphNodeVisits([
            createNodeTaskExecution('node_a'),
            createNodeTaskExecution('node_b'),
            createNodeTaskExecution('node_a'),
            createNodeTaskExecution('node_c'),
        ]);

        expect(visits.map((visit) => `${visit.nodeName}-${visit.visitNumber}`)).toEqual([
            'node_a-1',
            'node_b-1',
            'node_a-2',
            'node_c-1',
        ]);
    });

    it('should label a visit by its __node stamp rather than the task name', () => {
        const renamedTask = createNodeTaskExecution('node_a', {
            workflowTask: {name: 'some_task_name', parameters: {__node: 'node_a'}, type: 'test/v1/testAction'},
        } as Partial<TaskExecution>);

        expect(toGraphNodeVisits([renamedTask])[0].nodeName).toBe('node_a');
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

        const visits = toGraphNodeVisits([orphanTask]);

        expect(visits).toHaveLength(1);
        expect(visits[0].nodeName).toBe('orphanTask');
    });
});
