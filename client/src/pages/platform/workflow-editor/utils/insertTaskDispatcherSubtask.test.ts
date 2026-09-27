import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {describe, expect, it} from 'vitest';

import insertTaskDispatcherSubtask from './insertTaskDispatcherSubtask';

function task(overrides: Partial<WorkflowTask> & {name: string}): WorkflowTask {
    return {
        parameters: {},
        type: 'test/v1/action',
        ...overrides,
    };
}

describe('insertTaskDispatcherSubtask', () => {
    it('should insert into a fork-join branch whose node name is camelCased', () => {
        const forkJoinTask = task({
            name: 'forkJoin_1',
            parameters: {branches: [[task({name: 'first_action'})]]},
            type: 'fork-join/v1',
        });

        const updatedTasks = insertTaskDispatcherSubtask({
            newTask: task({name: 'second_action'}),
            taskDispatcherContext: {branchIndex: 0, index: 1, taskDispatcherId: 'forkJoin_1'},
            tasks: [forkJoinTask],
        });

        const branches = updatedTasks[0].parameters?.branches as WorkflowTask[][];

        expect(branches[0].map((subtask) => subtask.name)).toEqual(['first_action', 'second_action']);
    });

    it('should still insert into a fork-join branch with a hyphenated node name', () => {
        const forkJoinTask = task({
            name: 'fork-join_1',
            parameters: {branches: [[task({name: 'first_action'})]]},
            type: 'fork-join/v1',
        });

        const updatedTasks = insertTaskDispatcherSubtask({
            newTask: task({name: 'second_action'}),
            taskDispatcherContext: {branchIndex: 0, index: 1, taskDispatcherId: 'fork-join_1'},
            tasks: [forkJoinTask],
        });

        const branches = updatedTasks[0].parameters?.branches as WorkflowTask[][];

        expect(branches[0].map((subtask) => subtask.name)).toEqual(['first_action', 'second_action']);
    });
});
