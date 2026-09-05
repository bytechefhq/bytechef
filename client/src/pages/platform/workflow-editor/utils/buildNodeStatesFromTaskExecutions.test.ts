import {describe, expect, it} from 'vitest';

import buildNodeStatesFromTaskExecutions from './buildNodeStatesFromTaskExecutions';

function taskExecution(name: string, status: string, startSecond = 0, endSecond?: number) {
    return {
        endDate:
            endSecond === undefined ? undefined : new Date(`2024-01-01T10:00:${String(endSecond).padStart(2, '0')}Z`),
        startDate: new Date(`2024-01-01T10:00:${String(startSecond).padStart(2, '0')}Z`),
        status,
        workflowTask: {name},
    };
}

describe('buildNodeStatesFromTaskExecutions', () => {
    it('should key each state by workflow node name, with its duration', () => {
        expect(
            buildNodeStatesFromTaskExecutions(
                [taskExecution('var_1', 'COMPLETED', 0, 2), taskExecution('http_1', 'FAILED', 2, 3)],
                false
            )
        ).toEqual({
            http_1: {durationMillis: 1000, error: undefined, status: 'FAILED'},
            var_1: {durationMillis: 2000, error: undefined, status: 'COMPLETED'},
        });
    });

    it('should walk nested children, as a fork-join or condition carries its branches', () => {
        const nodeStates = buildNodeStatesFromTaskExecutions(
            [
                {
                    ...taskExecution('forkJoin_1', 'COMPLETED', 0, 5),
                    children: [taskExecution('firecrawl_1', 'COMPLETED', 0, 4)],
                },
            ],
            false
        );

        expect(Object.keys(nodeStates).sort()).toEqual(['firecrawl_1', 'forkJoin_1']);
    });

    it('should read raw JSON loop iterations and merge repeated runs of one node', () => {
        const nodeStates = buildNodeStatesFromTaskExecutions(
            [
                {
                    ...taskExecution('loop_1', 'FAILED', 0, 9),
                    iterations: [
                        [
                            {
                                endDate: '2024-01-01T10:00:01Z',
                                startDate: '2024-01-01T10:00:00Z',
                                status: 'COMPLETED',
                                workflowTask: {name: 'var_1'},
                            },
                        ],
                        [
                            {
                                endDate: '2024-01-01T10:00:03Z',
                                error: {message: 'Boom'},
                                startDate: '2024-01-01T10:00:01Z',
                                status: 'FAILED',
                                workflowTask: {name: 'var_1'},
                            },
                        ],
                    ],
                },
            ],
            false
        );

        expect(nodeStates.var_1).toEqual({durationMillis: 3000, error: 'Boom', status: 'FAILED'});
    });

    it('should show started tasks as running only while the job runs', () => {
        const taskExecutions = [taskExecution('var_1', 'COMPLETED', 0, 1), taskExecution('http_1', 'STARTED', 1)];

        expect(buildNodeStatesFromTaskExecutions(taskExecutions, true).http_1).toEqual({
            durationMillis: undefined,
            error: undefined,
            status: 'RUNNING',
        });
        expect(buildNodeStatesFromTaskExecutions(taskExecutions, false).http_1).toBeUndefined();
    });

    it('should not descend into a subflow child job, whose tasks belong to another workflow', () => {
        const subflowTaskExecution = {
            ...taskExecution('subflow_1', 'COMPLETED', 0, 1),
            childJob: {taskExecutions: [taskExecution('var_1', 'COMPLETED', 0, 1)]},
        };

        const nodeStates = buildNodeStatesFromTaskExecutions([subflowTaskExecution], false);

        expect(Object.keys(nodeStates)).toEqual(['subflow_1']);
    });

    it('should return no states when there is nothing to read', () => {
        expect(buildNodeStatesFromTaskExecutions(undefined, false)).toEqual({});
    });
});
