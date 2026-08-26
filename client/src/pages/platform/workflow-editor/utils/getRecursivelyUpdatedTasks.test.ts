import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {describe, expect, it} from 'vitest';

import getRecursivelyUpdatedTasks from './getRecursivelyUpdatedTasks';

function makeTask(name: string, parameters?: Record<string, unknown>): WorkflowTask {
    return {
        label: name,
        name,
        parameters,
        type: 'component/v1/action',
    } as WorkflowTask;
}

describe('getRecursivelyUpdatedTasks', () => {
    it('should replace a top-level task', () => {
        const tasks = [makeTask('task_1'), makeTask('task_2')];
        const replacement = makeTask('task_1', {updated: true});

        const result = getRecursivelyUpdatedTasks(tasks, replacement);

        expect(result[0]).toEqual(replacement);
        expect(result[1]).toEqual(tasks[1]);
    });

    it('should replace a task inside a graph node list', () => {
        const graphTask: WorkflowTask = {
            label: 'graph_1',
            name: 'graph_1',
            parameters: {
                maxTransitions: 100,
                nodes: [makeTask('task_1'), makeTask('task_2')],
                transitions: [],
            },
            type: 'graph/v1',
        } as WorkflowTask;

        const replacement = makeTask('task_2', {updated: true});

        const result = getRecursivelyUpdatedTasks([graphTask], replacement);

        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        const nodes = (result[0].parameters as any).nodes;

        expect(nodes[0]).toEqual(makeTask('task_1'));
        expect(nodes[1]).toEqual(replacement);
    });

    it('should replace a task nested inside a condition that is itself a graph node (pins the previously-missing nodes branch)', () => {
        const conditionTask: WorkflowTask = {
            label: 'condition_1',
            name: 'condition_1',
            parameters: {
                caseFalse: [],
                caseTrue: [makeTask('task_1')],
            },
            type: 'condition/v1',
        } as WorkflowTask;

        const graphTask: WorkflowTask = {
            label: 'graph_1',
            name: 'graph_1',
            parameters: {
                maxTransitions: 100,
                nodes: [conditionTask],
                transitions: [],
            },
            type: 'graph/v1',
        } as WorkflowTask;

        const replacement = makeTask('task_1', {updated: true});

        const result = getRecursivelyUpdatedTasks([graphTask], replacement);

        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        const nodes = (result[0].parameters as any).nodes;
        const updatedConditionTask = nodes[0];

        expect(updatedConditionTask.parameters.caseTrue).toEqual([replacement]);
    });

    it('should leave a task alone when it is not found anywhere in the graph nodes', () => {
        const graphTask: WorkflowTask = {
            label: 'graph_1',
            name: 'graph_1',
            parameters: {
                maxTransitions: 100,
                nodes: [makeTask('task_1')],
                transitions: [],
            },
            type: 'graph/v1',
        } as WorkflowTask;

        const replacement = makeTask('task_unrelated', {updated: true});

        const result = getRecursivelyUpdatedTasks([graphTask], replacement);

        expect(result[0]).toEqual(graphTask);
    });
});
