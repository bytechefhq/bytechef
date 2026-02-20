import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {describe, expect, it} from 'vitest';

import {clearSingleTaskPosition} from './removeWorkflowNodePosition';

function makeTask(name: string, nodePosition?: {x: number; y: number}): WorkflowTask {
    return {
        metadata: nodePosition ? {ui: {nodePosition}} : undefined,
        name,
        type: `test/${name}`,
    } as WorkflowTask;
}

describe('clearSingleTaskPosition', () => {
    it('should clear position for the matching task', () => {
        const tasks = [makeTask('task_1', {x: 100, y: 200})];

        const result = clearSingleTaskPosition(tasks, 'task_1');

        expect(result[0].metadata?.ui?.nodePosition).toBeUndefined();
    });

    it('should not clear positions for non-matching tasks', () => {
        const tasks = [makeTask('task_1', {x: 100, y: 200}), makeTask('task_2', {x: 300, y: 400})];

        const result = clearSingleTaskPosition(tasks, 'task_1');

        expect(result[0].metadata?.ui?.nodePosition).toBeUndefined();
        expect(result[1].metadata?.ui?.nodePosition).toEqual({x: 300, y: 400});
    });

    it('should leave task unchanged when it has no position and matches by name', () => {
        const tasks = [makeTask('task_1')];

        const result = clearSingleTaskPosition(tasks, 'task_1');

        expect(result[0].metadata?.ui?.nodePosition).toBeUndefined();
        expect(result[0].name).toBe('task_1');
    });

    it('should clear ALL child positions when matching a task dispatcher (loop)', () => {
        // When the matched task is a dispatcher, clearTaskPositions is used for children
        const loopTask: WorkflowTask = {
            ...makeTask('loop_1', {x: 100, y: 100}),
            parameters: {
                iteratee: [makeTask('child_1', {x: 200, y: 200}), makeTask('child_2', {x: 300, y: 300})],
            },
        };

        const result = clearSingleTaskPosition([loopTask], 'loop_1');
        const iteratee = result[0].parameters?.iteratee as WorkflowTask[];

        expect(result[0].metadata?.ui?.nodePosition).toBeUndefined();
        expect(iteratee[0].metadata?.ui?.nodePosition).toBeUndefined();
        expect(iteratee[1].metadata?.ui?.nodePosition).toBeUndefined();
    });

    it('should continue searching by name in non-matching dispatchers', () => {
        // When the dispatcher doesn't match, search its children by name
        const loopTask: WorkflowTask = {
            ...makeTask('loop_1', {x: 50, y: 50}),
            parameters: {
                iteratee: [makeTask('child_1', {x: 200, y: 200}), makeTask('child_2', {x: 300, y: 300})],
            },
        };

        const result = clearSingleTaskPosition([loopTask], 'child_1');
        const iteratee = result[0].parameters?.iteratee as WorkflowTask[];

        // loop_1 not matched, so its position stays
        expect(result[0].metadata?.ui?.nodePosition).toEqual({x: 50, y: 50});
        // child_1 matched, position cleared
        expect(iteratee[0].metadata?.ui?.nodePosition).toBeUndefined();
        // child_2 not matched, position stays
        expect(iteratee[1].metadata?.ui?.nodePosition).toEqual({x: 300, y: 300});
    });

    it('should clear ALL descendant positions when matching a nested dispatcher', () => {
        // loop_1 > condition_1 > grandchild_1
        // Search for condition_1: should clear condition_1 + ALL its children
        const conditionTask: WorkflowTask = {
            ...makeTask('condition_1', {x: 200, y: 200}),
            parameters: {
                caseFalse: [makeTask('false_child', {x: 400, y: 400})],
                caseTrue: [makeTask('true_child', {x: 300, y: 300})],
            },
        };
        const loopTask: WorkflowTask = {
            ...makeTask('loop_1', {x: 100, y: 100}),
            parameters: {
                iteratee: [conditionTask],
            },
        };

        const result = clearSingleTaskPosition([loopTask], 'condition_1');
        const iteratee = result[0].parameters?.iteratee as WorkflowTask[];
        const trueTasks = iteratee[0].parameters?.caseTrue as WorkflowTask[];
        const falseTasks = iteratee[0].parameters?.caseFalse as WorkflowTask[];

        // loop_1: not matched, position stays
        expect(result[0].metadata?.ui?.nodePosition).toEqual({x: 100, y: 100});
        // condition_1: matched, position cleared
        expect(iteratee[0].metadata?.ui?.nodePosition).toBeUndefined();
        // All children cleared via clearTaskPositions
        expect(trueTasks[0].metadata?.ui?.nodePosition).toBeUndefined();
        expect(falseTasks[0].metadata?.ui?.nodePosition).toBeUndefined();
    });

    it('should search through condition caseTrue and caseFalse', () => {
        const conditionTask: WorkflowTask = {
            ...makeTask('condition_1', {x: 50, y: 50}),
            parameters: {
                caseFalse: [makeTask('false_child', {x: 300, y: 300})],
                caseTrue: [makeTask('true_child', {x: 200, y: 200})],
            },
        };

        const result = clearSingleTaskPosition([conditionTask], 'false_child');
        const falseTasks = result[0].parameters?.caseFalse as WorkflowTask[];
        const trueTasks = result[0].parameters?.caseTrue as WorkflowTask[];

        expect(result[0].metadata?.ui?.nodePosition).toEqual({x: 50, y: 50});
        expect(trueTasks[0].metadata?.ui?.nodePosition).toEqual({x: 200, y: 200});
        expect(falseTasks[0].metadata?.ui?.nodePosition).toBeUndefined();
    });

    it('should search through branch cases and default', () => {
        const branchTask: WorkflowTask = {
            ...makeTask('branch_1', {x: 10, y: 10}),
            parameters: {
                cases: [{key: 'caseA', tasks: [makeTask('case_child', {x: 100, y: 100})], value: 'A'}],
                default: [makeTask('default_child', {x: 200, y: 200})],
            },
        };

        const result = clearSingleTaskPosition([branchTask], 'default_child');
        const cases = result[0].parameters?.cases as Array<{tasks: WorkflowTask[]}>;
        const defaultTasks = result[0].parameters?.default as WorkflowTask[];

        expect(cases[0].tasks[0].metadata?.ui?.nodePosition).toEqual({x: 100, y: 100});
        expect(defaultTasks[0].metadata?.ui?.nodePosition).toBeUndefined();
    });

    it('should search through parallel tasks parameter', () => {
        const parallelTask: WorkflowTask = {
            ...makeTask('parallel_1', {x: 10, y: 10}),
            parameters: {
                tasks: [makeTask('par_child_1', {x: 100, y: 100}), makeTask('par_child_2', {x: 200, y: 200})],
            },
        };

        const result = clearSingleTaskPosition([parallelTask], 'par_child_2');
        const tasks = result[0].parameters?.tasks as WorkflowTask[];

        expect(tasks[0].metadata?.ui?.nodePosition).toEqual({x: 100, y: 100});
        expect(tasks[1].metadata?.ui?.nodePosition).toBeUndefined();
    });

    it('should search through fork-join branches', () => {
        const forkJoinTask: WorkflowTask = {
            ...makeTask('forkJoin_1', {x: 10, y: 10}),
            parameters: {
                branches: [[makeTask('branch_a_1', {x: 100, y: 100})], [makeTask('branch_b_1', {x: 200, y: 200})]],
            },
        };

        const result = clearSingleTaskPosition([forkJoinTask], 'branch_b_1');
        const branches = result[0].parameters?.branches as WorkflowTask[][];

        expect(branches[0][0].metadata?.ui?.nodePosition).toEqual({x: 100, y: 100});
        expect(branches[1][0].metadata?.ui?.nodePosition).toBeUndefined();
    });

    it('should handle non-array input gracefully', () => {
        const result = clearSingleTaskPosition(null as unknown as WorkflowTask[], 'task_1');

        expect(result).toBeNull();
    });

    it('should handle empty array', () => {
        const result = clearSingleTaskPosition([], 'task_1');

        expect(result).toEqual([]);
    });

    it('should handle no match found anywhere', () => {
        const tasks = [makeTask('task_1', {x: 100, y: 200}), makeTask('task_2', {x: 300, y: 400})];

        const result = clearSingleTaskPosition(tasks, 'nonexistent');

        // All positions should remain unchanged
        expect(result[0].metadata?.ui?.nodePosition).toEqual({x: 100, y: 200});
        expect(result[1].metadata?.ui?.nodePosition).toEqual({x: 300, y: 400});
    });

    it('should not mutate the original tasks', () => {
        const original = makeTask('task_1', {x: 100, y: 200});
        const tasks = [original];

        clearSingleTaskPosition(tasks, 'task_1');

        expect(original.metadata?.ui?.nodePosition).toEqual({x: 100, y: 200});
    });
});
