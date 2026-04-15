import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {describe, expect, it} from 'vitest';

import {getTask} from './getTask';

function task(overrides: Partial<WorkflowTask> & {name: string}): WorkflowTask {
    return {
        parameters: {},
        type: 'test/v1/action',
        ...overrides,
    };
}

describe('getTask', () => {
    it('returns undefined when the task is not found', () => {
        expect(getTask({tasks: [task({name: 'a'})], workflowNodeName: 'missing'})).toBeUndefined();
    });

    it('returns a direct top-level match', () => {
        const target = task({name: 'target'});

        expect(getTask({tasks: [task({name: 'a'}), target], workflowNodeName: 'target'})).toBe(target);
    });

    it('recurses into loop iteratee when no direct match exists', () => {
        const nested = task({name: 'nested'});
        const loopTask = task({
            name: 'loop_1',
            parameters: {iteratee: [nested]},
            type: 'loop/v1',
        });

        expect(getTask({tasks: [loopTask], workflowNodeName: 'nested'})).toBe(nested);
    });

    // Regression test for bug 732: server flattens all tasks (including those nested in task
    // dispatchers) and enriches each with .connections/.clusterElements/.clusterRoot. The flat
    // enriched DTO coexists with the raw nested map inside the dispatcher's iteratee. Without
    // a direct-match-first pass, getTask returned the raw nested map, stripping the server-
    // computed .connections and breaking connection selection for AI agents wrapped in loops.
    it('prefers the flat enriched DTO over a raw nested copy in dispatcher parameters', () => {
        const nestedRawAgent = task({
            name: 'aiAgent_1',
            parameters: {format: 'SIMPLE'},
            type: 'aiAgent/v1/streamChat',
        });
        const loopTask = task({
            name: 'loop_1',
            parameters: {iteratee: [nestedRawAgent]},
            type: 'loop/v1',
        });
        const flatEnrichedAgent = {
            ...task({name: 'aiAgent_1', type: 'aiAgent/v1/streamChat'}),
            clusterRoot: true,
            connections: [
                {
                    componentName: 'openAi',
                    componentVersion: 1,
                    key: 'openAi_1',
                    required: true,
                    workflowNodeName: 'aiAgent_1',
                },
            ],
        };

        const found = getTask({
            tasks: [loopTask, flatEnrichedAgent],
            workflowNodeName: 'aiAgent_1',
        });

        expect(found).toBe(flatEnrichedAgent);
        expect(found?.connections).toHaveLength(1);
    });

    it('finds a task nested inside a branch case', () => {
        const nested = task({name: 'branch_action'});
        const branchTask = task({
            name: 'branch_1',
            parameters: {cases: [{key: 'case_a', tasks: [nested]}]},
            type: 'branch/v1',
        });

        expect(getTask({tasks: [branchTask], workflowNodeName: 'branch_action'})).toBe(nested);
    });

    it('finds a task nested inside a fork-join branch', () => {
        const nested = task({name: 'forked_action'});
        const forkJoinTask = task({
            name: 'forkJoin_1',
            parameters: {branches: [[nested]]},
            type: 'fork-join/v1',
        });

        expect(getTask({tasks: [forkJoinTask], workflowNodeName: 'forked_action'})).toBe(nested);
    });
});
