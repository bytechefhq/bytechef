import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {describe, expect, it} from 'vitest';

import {flattenDefinitionTasks} from './flattenDefinitionTasks';

function task(name: string, type = 'test/v1/action', parameters: Record<string, unknown> = {}): WorkflowTask {
    return {name, parameters, type};
}

describe('flattenDefinitionTasks', () => {
    it('should return empty array for empty input', () => {
        expect(flattenDefinitionTasks([])).toEqual([]);
    });

    it('should return flat tasks unchanged', () => {
        const tasks = [task('a'), task('b'), task('c')];

        expect(flattenDefinitionTasks(tasks)).toEqual(tasks);
    });

    it('should flatten condition caseTrue and caseFalse subtasks', () => {
        const conditionTask = task('condition_1', 'condition/v1', {
            caseFalse: [task('false_1')],
            caseTrue: [task('true_1'), task('true_2')],
        });

        const result = flattenDefinitionTasks([task('before'), conditionTask, task('after')]);

        expect(result.map((task) => task.name)).toEqual([
            'before',
            'condition_1',
            'false_1',
            'true_1',
            'true_2',
            'after',
        ]);
    });

    it('should flatten loop iteratee subtasks (array)', () => {
        const loopTask = task('loop_1', 'loop/v1', {
            iteratee: [task('inner_1'), task('inner_2')],
        });

        const result = flattenDefinitionTasks([loopTask]);

        expect(result.map((task) => task.name)).toEqual(['loop_1', 'inner_1', 'inner_2']);
    });

    it('should flatten each iteratee subtask (single object)', () => {
        const eachTask = task('each_1', 'each/v1', {
            iteratee: task('inner_1'),
        });

        const result = flattenDefinitionTasks([eachTask]);

        expect(result.map((task) => task.name)).toEqual(['each_1', 'inner_1']);
    });

    it('should flatten branch cases and default', () => {
        const branchTask = task('branch_1', 'branch/v1', {
            cases: [
                {key: 'case_a', tasks: [task('case_a_1')]},
                {key: 'case_b', tasks: [task('case_b_1'), task('case_b_2')]},
            ],
            default: [task('default_1')],
        });

        const result = flattenDefinitionTasks([branchTask]);

        expect(result.map((task) => task.name)).toEqual(['branch_1', 'case_a_1', 'case_b_1', 'case_b_2', 'default_1']);
    });

    it('should flatten parallel tasks', () => {
        const parallelTask = task('parallel_1', 'parallel/v1', {
            tasks: [task('p_1'), task('p_2')],
        });

        const result = flattenDefinitionTasks([parallelTask]);

        expect(result.map((task) => task.name)).toEqual(['parallel_1', 'p_1', 'p_2']);
    });

    it('should flatten fork-join branches (list of lists)', () => {
        const forkJoinTask = task('forkJoin_1', 'fork-join/v1', {
            branches: [[task('branch_a_1'), task('branch_a_2')], [task('branch_b_1')]],
        });

        const result = flattenDefinitionTasks([forkJoinTask]);

        expect(result.map((task) => task.name)).toEqual(['forkJoin_1', 'branch_a_1', 'branch_a_2', 'branch_b_1']);
    });

    it('should recursively flatten nested task dispatchers', () => {
        const innerCondition = task('inner_condition', 'condition/v1', {
            caseFalse: [task('inner_false')],
            caseTrue: [task('inner_true')],
        });

        const outerLoop = task('loop_1', 'loop/v1', {
            iteratee: [innerCondition, task('after_inner')],
        });

        const result = flattenDefinitionTasks([outerLoop]);

        expect(result.map((task) => task.name)).toEqual([
            'loop_1',
            'inner_condition',
            'inner_false',
            'inner_true',
            'after_inner',
        ]);
    });

    it('should skip non-task parameter values', () => {
        const taskWithMixedParams = task('task_1', 'test/v1/action', {
            boolParam: true,
            nullParam: null,
            numberParam: 42,
            stringParam: 'hello',
        });

        const result = flattenDefinitionTasks([taskWithMixedParams]);

        expect(result).toHaveLength(1);
        expect(result[0].name).toBe('task_1');
    });
});
