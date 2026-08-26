import {WorkflowTestExecution} from '@/shared/middleware/platform/workflow/test';
import {describe, expect, it} from 'vitest';

import collectGraphNodeExecutions from './collectGraphNodeExecutions';

function nodeTaskExecution(nodeName: string, startedAtSecond: number, status = 'COMPLETED') {
    return {
        id: `${nodeName}-${startedAtSecond}`,
        startDate: new Date(`2024-01-01T10:00:${String(startedAtSecond).padStart(2, '0')}`),
        status,
        workflowTask: {name: nodeName, parameters: {__node: nodeName}},
    };
}

function testExecution(taskExecutions: unknown[]): WorkflowTestExecution {
    return {job: {taskExecutions}} as WorkflowTestExecution;
}

describe('collectGraphNodeExecutions', () => {
    it('should read every node dispatch of the named graph off its task execution children', () => {
        const workflowTestExecution = testExecution([
            {id: 'var-1', workflowTask: {name: 'var_1'}},
            {
                children: [nodeTaskExecution('node_a', 1), nodeTaskExecution('node_b', 2, 'FAILED')],
                id: 'graph-1',
                workflowTask: {name: 'graph_1'},
            },
        ]);

        expect(collectGraphNodeExecutions(workflowTestExecution, 'graph_1')).toEqual([
            {nodeName: 'node_a', startDate: new Date('2024-01-01T10:00:01'), status: 'COMPLETED'},
            {nodeName: 'node_b', startDate: new Date('2024-01-01T10:00:02'), status: 'FAILED'},
        ]);
    });

    it('should find a graph nested inside another dispatcher', () => {
        const workflowTestExecution = testExecution([
            {
                children: [
                    {
                        children: [nodeTaskExecution('node_a', 1)],
                        id: 'graph-1',
                        workflowTask: {name: 'graph_1'},
                    },
                ],
                id: 'condition-1',
                workflowTask: {name: 'condition_1'},
            },
        ]);

        expect(collectGraphNodeExecutions(workflowTestExecution, 'graph_1').map((one) => one.nodeName)).toEqual([
            'node_a',
        ]);
    });

    it('should find a graph nested inside a loop iteration', () => {
        const workflowTestExecution = testExecution([
            {
                id: 'loop-1',
                iterations: [
                    [{children: [nodeTaskExecution('node_a', 1)], id: 'graph-1', workflowTask: {name: 'graph_1'}}],
                ],
                workflowTask: {name: 'loop_1'},
            },
        ]);

        expect(collectGraphNodeExecutions(workflowTestExecution, 'graph_1').map((one) => one.nodeName)).toEqual([
            'node_a',
        ]);
    });

    it('should prefer the __node stamp over the task name, which is the dispatcher own record', () => {
        const workflowTestExecution = testExecution([
            {
                children: [
                    {
                        id: 'renamed',
                        startDate: new Date('2024-01-01T10:00:01'),
                        status: 'COMPLETED',
                        workflowTask: {name: 'some_task_name', parameters: {__node: 'node_a'}},
                    },
                ],
                id: 'graph-1',
                workflowTask: {name: 'graph_1'},
            },
        ]);

        expect(collectGraphNodeExecutions(workflowTestExecution, 'graph_1')[0].nodeName).toBe('node_a');
    });

    it('should fall back to the task name when the __node stamp is missing', () => {
        const workflowTestExecution = testExecution([
            {
                children: [{id: 'orphan', startDate: new Date(), status: 'COMPLETED', workflowTask: {name: 'node_a'}}],
                id: 'graph-1',
                workflowTask: {name: 'graph_1'},
            },
        ]);

        expect(collectGraphNodeExecutions(workflowTestExecution, 'graph_1')[0].nodeName).toBe('node_a');
    });

    it('should return nothing when no run happened, or when the graph did not run', () => {
        expect(collectGraphNodeExecutions(undefined, 'graph_1')).toEqual([]);
        expect(collectGraphNodeExecutions(testExecution([]), 'graph_1')).toEqual([]);
        expect(
            collectGraphNodeExecutions(testExecution([{id: 'var-1', workflowTask: {name: 'var_1'}}]), 'graph_1')
        ).toEqual([]);
    });
});
