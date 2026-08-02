import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {describe, expect, it} from 'vitest';

import {forEachNestedTaskGroup} from './taskTraversalUtils';

function makeTask(name: string, type = 'test/v1/action'): WorkflowTask {
    return {name, type} as WorkflowTask;
}

describe('forEachNestedTaskGroup', () => {
    it('should visit caseTrue and caseFalse arrays', () => {
        const visited: Array<{key: string; names: string[]}> = [];
        const parameters = {
            caseFalse: [makeTask('falseTask')],
            caseTrue: [makeTask('trueTask1'), makeTask('trueTask2')],
        };

        forEachNestedTaskGroup(parameters, (tasks, key) => {
            visited.push({key, names: tasks.map((task) => task.name)});
        });

        expect(visited).toEqual([
            {key: 'caseTrue', names: ['trueTask1', 'trueTask2']},
            {key: 'caseFalse', names: ['falseTask']},
        ]);
    });

    it('should visit iteratee as array (loop/map)', () => {
        const visited: Array<{key: string; names: string[]}> = [];

        forEachNestedTaskGroup({iteratee: [makeTask('loopBody')]}, (tasks, key) => {
            visited.push({key, names: tasks.map((task) => task.name)});
        });

        expect(visited).toEqual([{key: 'iteratee', names: ['loopBody']}]);
    });

    it('should visit iteratee as single object (each)', () => {
        const visited: Array<{key: string; names: string[]}> = [];

        forEachNestedTaskGroup({iteratee: makeTask('eachBody')}, (tasks, key) => {
            visited.push({key, names: tasks.map((task) => task.name)});
        });

        expect(visited).toEqual([{key: 'iteratee', names: ['eachBody']}]);
    });

    it('should visit branch cases individually', () => {
        const visited: Array<{key: string; names: string[]}> = [];
        const parameters = {
            cases: [
                {key: 'case1', tasks: [makeTask('caseTask1')]},
                {key: 'case2', tasks: [makeTask('caseTask2a'), makeTask('caseTask2b')]},
            ],
        };

        forEachNestedTaskGroup(parameters, (tasks, key) => {
            visited.push({key, names: tasks.map((task) => task.name)});
        });

        expect(visited).toEqual([
            {key: 'cases', names: ['caseTask1']},
            {key: 'cases', names: ['caseTask2a', 'caseTask2b']},
        ]);
    });

    it('should visit default tasks (branch default)', () => {
        const visited: Array<{key: string; names: string[]}> = [];

        forEachNestedTaskGroup({default: [makeTask('defaultTask')]}, (tasks, key) => {
            visited.push({key, names: tasks.map((task) => task.name)});
        });

        expect(visited).toEqual([{key: 'default', names: ['defaultTask']}]);
    });

    it('should visit parallel tasks', () => {
        const visited: Array<{key: string; names: string[]}> = [];

        forEachNestedTaskGroup({tasks: [makeTask('parallelTask')]}, (tasks, key) => {
            visited.push({key, names: tasks.map((task) => task.name)});
        });

        expect(visited).toEqual([{key: 'tasks', names: ['parallelTask']}]);
    });

    it('should visit fork-join branches individually', () => {
        const visited: Array<{key: string; names: string[]}> = [];
        const parameters = {
            branches: [[makeTask('branch1a'), makeTask('branch1b')], [makeTask('branch2a')]],
        };

        forEachNestedTaskGroup(parameters, (tasks, key) => {
            visited.push({key, names: tasks.map((task) => task.name)});
        });

        expect(visited).toEqual([
            {key: 'branches', names: ['branch1a', 'branch1b']},
            {key: 'branches', names: ['branch2a']},
        ]);
    });

    it('should visit graph nodes individually', () => {
        const visited: Array<{key: string; names: string[]}> = [];
        const parameters = {
            maxTransitions: 100,
            nodes: [
                {name: 'node_0', next: 'node_1', tasks: [makeTask('node0Task1'), makeTask('node0Task2')]},
                {name: 'node_1', tasks: [makeTask('node1Task1')]},
            ],
        };

        forEachNestedTaskGroup(parameters, (tasks, key) => {
            visited.push({key, names: tasks.map((task) => task.name)});
        });

        expect(visited).toEqual([
            {key: 'nodes', names: ['node0Task1', 'node0Task2']},
            {key: 'nodes', names: ['node1Task1']},
        ]);
    });

    it('should still visit an empty graph node (survives task deletion, contributes no subtasks)', () => {
        const visited: Array<{key: string; names: string[]}> = [];
        const parameters = {
            nodes: [
                {name: 'node_0', tasks: []},
                {name: 'node_1', tasks: [makeTask('node1Task1')]},
            ],
        };

        forEachNestedTaskGroup(parameters, (tasks, key) => {
            visited.push({key, names: tasks.map((task) => task.name)});
        });

        // Mirrors fork-join's 'branches' handling: every array-typed node group is visited
        // regardless of length, so an empty node (survives last-task deletion) is included
        // with zero names rather than being filtered out.
        expect(visited).toEqual([
            {key: 'nodes', names: []},
            {key: 'nodes', names: ['node1Task1']},
        ]);
    });

    it('should skip empty arrays and non-task arrays', () => {
        const visited: string[] = [];
        const parameters = {
            caseFalse: [],
            caseTrue: [{value: 'not a task'}],
            iteratee: 'string-value',
            tasks: [],
        };

        forEachNestedTaskGroup(parameters as Record<string, unknown>, (_, key) => {
            visited.push(key);
        });

        expect(visited).toEqual([]);
    });

    it('should handle parameters with no subtask collections', () => {
        const visited: string[] = [];

        forEachNestedTaskGroup({someProperty: 'value', timeout: 30}, (_, key) => {
            visited.push(key);
        });

        expect(visited).toEqual([]);
    });
});
