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

function placedAt(newTask: WorkflowTask, nodePosition: {x: number; y: number}): WorkflowTask {
    return {...newTask, metadata: {...newTask.metadata, ui: {...newTask.metadata?.ui, nodePosition}}};
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

describe('insertTaskDispatcherSubtask - chain dispatchers', () => {
    it('should pin a chain subtask inserted between two pinned subtasks at their midpoint', () => {
        const firstTask = placedAt(task({name: 'first_action'}), {x: 100, y: 600});
        const thirdTask = placedAt(task({name: 'third_action'}), {x: 500, y: 800});
        const newTask = task({name: 'second_action'});
        const conditionTask = task({
            name: 'condition_1',
            parameters: {caseFalse: [], caseTrue: [firstTask, thirdTask]},
            type: 'condition/v1',
        });

        const updatedTasks = insertTaskDispatcherSubtask({
            newTask,
            taskDispatcherContext: {conditionCase: 'caseTrue', index: 1, taskDispatcherId: 'condition_1'},
            tasks: [conditionTask],
        });

        const caseTrue = updatedTasks[0].parameters?.caseTrue as WorkflowTask[];

        expect(caseTrue[1].name).toBe('second_action');
        expect(caseTrue[1].metadata?.ui?.nodePosition).toEqual({x: 300, y: 700});
    });

    it('should keep the saved positions of chain subtasks after the insertion point intact', () => {
        const firstTask = task({name: 'first_action'});
        const thirdTask = placedAt(task({name: 'third_action'}), {x: 300, y: 120});
        const newTask = task({name: 'second_action'});
        const conditionTask = task({
            name: 'condition_1',
            parameters: {caseFalse: [], caseTrue: [firstTask, thirdTask]},
            type: 'condition/v1',
        });

        const updatedTasks = insertTaskDispatcherSubtask({
            newTask,
            taskDispatcherContext: {conditionCase: 'caseTrue', index: 1, taskDispatcherId: 'condition_1'},
            tasks: [conditionTask],
        });

        const caseTrue = updatedTasks[0].parameters?.caseTrue as WorkflowTask[];

        expect(caseTrue.map((subtask) => subtask.name)).toEqual(['first_action', 'second_action', 'third_action']);
        expect(caseTrue[2].metadata?.ui?.nodePosition).toEqual({x: 300, y: 120});
    });
});
