import {TaskExecution} from '@/shared/middleware/platform/workflow/test';
import {describe, expect, it} from 'vitest';

import collectGraphNodeExecutions from './collectGraphNodeExecutions';
import {GraphNodeExecutionI} from './getExecutedEdgeStatus';

interface NodeTaskExecutionProps {
    nodeName: string;
    startedAtSecond: number;
    status?: string;
}

function nodeTaskExecution({nodeName, startedAtSecond, status = 'COMPLETED'}: NodeTaskExecutionProps) {
    return {
        id: `${nodeName}-${startedAtSecond}`,
        startDate: new Date(`2024-01-01T10:00:${String(startedAtSecond).padStart(2, '0')}`),
        status,
        workflowTask: {name: nodeName},
    };
}

function asTaskExecutions(taskExecutions: unknown[]): TaskExecution[] {
    return taskExecutions as TaskExecution[];
}

function toNodeNameRuns(nodeExecutionRuns: GraphNodeExecutionI[][]): string[][] {
    return nodeExecutionRuns.map((nodeExecutions) => nodeExecutions.map((nodeExecution) => nodeExecution.nodeName));
}

describe('collectGraphNodeExecutions', () => {
    it('should read every node dispatch of the named graph off its task execution children', () => {
        const taskExecutions = asTaskExecutions([
            {id: 'var-1', workflowTask: {name: 'var_1'}},
            {
                children: [
                    nodeTaskExecution({nodeName: 'node_a', startedAtSecond: 1}),
                    nodeTaskExecution({nodeName: 'node_b', startedAtSecond: 2, status: 'FAILED'}),
                ],
                id: 'graph-1',
                workflowTask: {name: 'graph_1'},
            },
        ]);

        expect(collectGraphNodeExecutions(taskExecutions, 'graph_1')).toEqual([
            [
                {nodeName: 'node_a', startDate: new Date('2024-01-01T10:00:01'), status: 'COMPLETED'},
                {nodeName: 'node_b', startDate: new Date('2024-01-01T10:00:02'), status: 'FAILED'},
            ],
        ]);
    });

    it('should find a graph nested inside another dispatcher', () => {
        const taskExecutions = asTaskExecutions([
            {
                children: [
                    {
                        children: [nodeTaskExecution({nodeName: 'node_a', startedAtSecond: 1})],
                        id: 'graph-1',
                        workflowTask: {name: 'graph_1'},
                    },
                ],
                id: 'condition-1',
                workflowTask: {name: 'condition_1'},
            },
        ]);

        expect(toNodeNameRuns(collectGraphNodeExecutions(taskExecutions, 'graph_1'))).toEqual([['node_a']]);
    });

    it('should find a graph nested inside a loop iteration', () => {
        const taskExecutions = asTaskExecutions([
            {
                id: 'loop-1',
                iterations: [
                    [
                        {
                            children: [nodeTaskExecution({nodeName: 'node_a', startedAtSecond: 1})],
                            id: 'graph-1',
                            workflowTask: {name: 'graph_1'},
                        },
                    ],
                ],
                workflowTask: {name: 'loop_1'},
            },
        ]);

        expect(toNodeNameRuns(collectGraphNodeExecutions(taskExecutions, 'graph_1'))).toEqual([['node_a']]);
    });

    it('should read the start dates of raw JSON loop iterations as dates', () => {
        const taskExecutions = asTaskExecutions([
            {
                id: 'loop-1',
                iterations: [
                    [
                        {
                            children: [
                                {
                                    id: 'node-a',
                                    startDate: '2024-01-01T10:00:01Z',
                                    status: 'COMPLETED',
                                    workflowTask: {name: 'node_a'},
                                },
                            ],
                            id: 'graph-1',
                            workflowTask: {name: 'graph_1'},
                        },
                    ],
                ],
                workflowTask: {name: 'loop_1'},
            },
        ]);

        expect(collectGraphNodeExecutions(taskExecutions, 'graph_1')[0][0].startDate).toEqual(
            new Date('2024-01-01T10:00:01Z')
        );
    });

    it('should keep each loop iteration of the graph as its own run', () => {
        const taskExecutions = asTaskExecutions([
            {
                id: 'loop-1',
                iterations: [
                    [
                        {
                            children: [
                                nodeTaskExecution({nodeName: 'node_a', startedAtSecond: 1}),
                                nodeTaskExecution({nodeName: 'node_c', startedAtSecond: 2}),
                            ],
                            id: 'graph-1-first',
                            workflowTask: {name: 'graph_1'},
                        },
                    ],
                    [
                        {
                            children: [
                                nodeTaskExecution({nodeName: 'node_a', startedAtSecond: 3}),
                                nodeTaskExecution({nodeName: 'node_b', startedAtSecond: 4}),
                            ],
                            id: 'graph-1-second',
                            workflowTask: {name: 'graph_1'},
                        },
                    ],
                ],
                workflowTask: {name: 'loop_1'},
            },
        ]);

        expect(toNodeNameRuns(collectGraphNodeExecutions(taskExecutions, 'graph_1'))).toEqual([
            ['node_a', 'node_c'],
            ['node_a', 'node_b'],
        ]);
    });

    it('should name a node dispatch by its task name', () => {
        const taskExecutions = asTaskExecutions([
            {
                children: [{id: 'orphan', startDate: new Date(), status: 'COMPLETED', workflowTask: {name: 'node_a'}}],
                id: 'graph-1',
                workflowTask: {name: 'graph_1'},
            },
        ]);

        expect(collectGraphNodeExecutions(taskExecutions, 'graph_1')[0][0].nodeName).toBe('node_a');
    });

    it('should return nothing when no run happened, or when the graph did not run', () => {
        expect(collectGraphNodeExecutions(undefined, 'graph_1')).toEqual([]);
        expect(collectGraphNodeExecutions(asTaskExecutions([]), 'graph_1')).toEqual([]);
        expect(
            collectGraphNodeExecutions(asTaskExecutions([{id: 'var-1', workflowTask: {name: 'var_1'}}]), 'graph_1')
        ).toEqual([]);
    });
});
