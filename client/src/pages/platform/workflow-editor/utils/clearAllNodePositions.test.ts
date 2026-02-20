import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {describe, expect, it} from 'vitest';

import {clearTaskPositions} from './clearAllNodePositions';

function makeTask(name: string, nodePosition?: {x: number; y: number}): WorkflowTask {
    return {
        metadata: nodePosition ? {ui: {nodePosition}} : undefined,
        name,
        type: `test/${name}`,
    } as WorkflowTask;
}

describe('clearTaskPositions', () => {
    it('should clear position from a single task', () => {
        const tasks = [makeTask('task_1', {x: 100, y: 200})];

        const result = clearTaskPositions(tasks);

        expect(result[0].metadata?.ui?.nodePosition).toBeUndefined();
    });

    it('should not modify tasks without positions', () => {
        const tasks = [makeTask('task_1')];

        const result = clearTaskPositions(tasks);

        expect(result[0].metadata?.ui?.nodePosition).toBeUndefined();
        expect(result[0].name).toBe('task_1');
    });

    it('should clear positions from multiple tasks', () => {
        const tasks = [makeTask('task_1', {x: 10, y: 20}), makeTask('task_2', {x: 30, y: 40})];

        const result = clearTaskPositions(tasks);

        expect(result[0].metadata?.ui?.nodePosition).toBeUndefined();
        expect(result[1].metadata?.ui?.nodePosition).toBeUndefined();
    });

    it('should clear positions inside condition caseTrue and caseFalse', () => {
        const conditionTask: WorkflowTask = {
            ...makeTask('condition_1', {x: 50, y: 50}),
            parameters: {
                caseFalse: [makeTask('false_child', {x: 300, y: 400})],
                caseTrue: [makeTask('true_child', {x: 100, y: 200})],
            },
        };

        const result = clearTaskPositions([conditionTask]);

        expect(result[0].metadata?.ui?.nodePosition).toBeUndefined();

        const trueTasks = result[0].parameters?.caseTrue as WorkflowTask[];
        const falseTasks = result[0].parameters?.caseFalse as WorkflowTask[];

        expect(trueTasks[0].metadata?.ui?.nodePosition).toBeUndefined();
        expect(falseTasks[0].metadata?.ui?.nodePosition).toBeUndefined();
    });

    it('should clear positions inside loop iteratee', () => {
        const loopTask: WorkflowTask = {
            ...makeTask('loop_1', {x: 100, y: 100}),
            parameters: {
                iteratee: [makeTask('child_1', {x: 200, y: 200}), makeTask('child_2', {x: 300, y: 300})],
            },
        };

        const result = clearTaskPositions([loopTask]);
        const iteratee = result[0].parameters?.iteratee as WorkflowTask[];

        expect(result[0].metadata?.ui?.nodePosition).toBeUndefined();
        expect(iteratee[0].metadata?.ui?.nodePosition).toBeUndefined();
        expect(iteratee[1].metadata?.ui?.nodePosition).toBeUndefined();
    });

    it('should skip non-array iteratee', () => {
        const eachTask: WorkflowTask = {
            ...makeTask('each_1'),
            parameters: {
                iteratee: {name: 'single_task', type: 'test/single'},
            },
        };

        const result = clearTaskPositions([eachTask]);

        expect(result[0].parameters?.iteratee).toEqual({name: 'single_task', type: 'test/single'});
    });

    it('should clear positions inside branch cases and default', () => {
        const branchTask: WorkflowTask = {
            ...makeTask('branch_1', {x: 10, y: 10}),
            parameters: {
                cases: [
                    {key: 'caseA', tasks: [makeTask('case_a_child', {x: 100, y: 100})], value: 'A'},
                    {key: 'caseB', tasks: [makeTask('case_b_child', {x: 200, y: 200})], value: 'B'},
                ],
                default: [makeTask('default_child', {x: 300, y: 300})],
            },
        };

        const result = clearTaskPositions([branchTask]);
        const cases = result[0].parameters?.cases as Array<{tasks: WorkflowTask[]}>;
        const defaultTasks = result[0].parameters?.default as WorkflowTask[];

        expect(result[0].metadata?.ui?.nodePosition).toBeUndefined();
        expect(cases[0].tasks[0].metadata?.ui?.nodePosition).toBeUndefined();
        expect(cases[1].tasks[0].metadata?.ui?.nodePosition).toBeUndefined();
        expect(defaultTasks[0].metadata?.ui?.nodePosition).toBeUndefined();
    });

    it('should handle branch cases with no tasks', () => {
        const branchTask: WorkflowTask = {
            ...makeTask('branch_1'),
            parameters: {
                cases: [{key: 'empty', tasks: undefined, value: 'E'}],
            },
        };

        const result = clearTaskPositions([branchTask]);
        const cases = result[0].parameters?.cases as Array<{tasks: WorkflowTask[] | undefined}>;

        expect(cases[0].tasks).toBeUndefined();
    });

    it('should clear positions inside parallel tasks', () => {
        const parallelTask: WorkflowTask = {
            ...makeTask('parallel_1', {x: 10, y: 10}),
            parameters: {
                tasks: [makeTask('par_child_1', {x: 100, y: 100}), makeTask('par_child_2', {x: 200, y: 200})],
            },
        };

        const result = clearTaskPositions([parallelTask]);
        const tasks = result[0].parameters?.tasks as WorkflowTask[];

        expect(result[0].metadata?.ui?.nodePosition).toBeUndefined();
        expect(tasks[0].metadata?.ui?.nodePosition).toBeUndefined();
        expect(tasks[1].metadata?.ui?.nodePosition).toBeUndefined();
    });

    it('should clear positions inside fork-join branches', () => {
        const forkJoinTask: WorkflowTask = {
            ...makeTask('forkJoin_1', {x: 10, y: 10}),
            parameters: {
                branches: [
                    [makeTask('branch_a_1', {x: 100, y: 100}), makeTask('branch_a_2', {x: 150, y: 150})],
                    [makeTask('branch_b_1', {x: 200, y: 200})],
                ],
            },
        };

        const result = clearTaskPositions([forkJoinTask]);
        const branches = result[0].parameters?.branches as WorkflowTask[][];

        expect(result[0].metadata?.ui?.nodePosition).toBeUndefined();
        expect(branches[0][0].metadata?.ui?.nodePosition).toBeUndefined();
        expect(branches[0][1].metadata?.ui?.nodePosition).toBeUndefined();
        expect(branches[1][0].metadata?.ui?.nodePosition).toBeUndefined();
    });

    it('should clear positions in deeply nested dispatchers', () => {
        // loop_1 > condition_1 > child_1
        const conditionTask: WorkflowTask = {
            ...makeTask('condition_1', {x: 200, y: 200}),
            parameters: {
                caseTrue: [makeTask('child_1', {x: 300, y: 300})],
            },
        };
        const loopTask: WorkflowTask = {
            ...makeTask('loop_1', {x: 100, y: 100}),
            parameters: {
                iteratee: [conditionTask],
            },
        };

        const result = clearTaskPositions([loopTask]);
        const iteratee = result[0].parameters?.iteratee as WorkflowTask[];
        const trueTasks = iteratee[0].parameters?.caseTrue as WorkflowTask[];

        expect(result[0].metadata?.ui?.nodePosition).toBeUndefined();
        expect(iteratee[0].metadata?.ui?.nodePosition).toBeUndefined();
        expect(trueTasks[0].metadata?.ui?.nodePosition).toBeUndefined();
    });

    it('should handle non-array input gracefully', () => {
        const result = clearTaskPositions(null as unknown as WorkflowTask[]);

        expect(result).toBeNull();
    });

    it('should handle empty array', () => {
        const result = clearTaskPositions([]);

        expect(result).toEqual([]);
    });

    it('should not mutate the original tasks', () => {
        const original = makeTask('task_1', {x: 100, y: 200});
        const tasks = [original];

        clearTaskPositions(tasks);

        expect(original.metadata?.ui?.nodePosition).toEqual({x: 100, y: 200});
    });
});
