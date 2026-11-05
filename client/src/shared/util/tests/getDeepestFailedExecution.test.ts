import {TaskExecution, TriggerExecution} from '@/shared/middleware/platform/workflow/test';
import {describe, expect, it} from 'vitest';

import getDeepestFailedExecution from '../getDeepestFailedExecution';

const executionError = {message: 'Boom'};

function makeTaskExecution(id: string | undefined, overrides: Partial<TaskExecution> = {}): TaskExecution {
    return {id, workflowNodeName: id, ...overrides} as TaskExecution;
}

describe('getDeepestFailedExecution', () => {
    describe('trigger executions', () => {
        it('returns a failed trigger with the current path, without appending its id', () => {
            const triggerExecution = {error: executionError, id: 'trigger-1'} as TriggerExecution;

            expect(
                getDeepestFailedExecution({currentPath: [], execution: triggerExecution, isTriggerExecution: true})
            ).toEqual({execution: triggerExecution, path: []});
            expect(
                getDeepestFailedExecution({
                    currentPath: ['job-1'],
                    execution: triggerExecution,
                    isTriggerExecution: true,
                })
            ).toEqual({execution: triggerExecution, path: ['job-1']});
        });

        it('returns null for a trigger without an error', () => {
            const triggerExecution = {id: 'trigger-1'} as TriggerExecution;

            expect(
                getDeepestFailedExecution({currentPath: [], execution: triggerExecution, isTriggerExecution: true})
            ).toBeNull();
        });

        it('appends the id of a failed execution that is not flagged as a trigger', () => {
            const triggerExecution = {error: executionError, id: 'trigger-1'} as TriggerExecution;

            expect(getDeepestFailedExecution({currentPath: [], execution: triggerExecution})).toEqual({
                execution: triggerExecution,
                path: ['trigger-1'],
            });
        });
    });

    describe('task executions without children', () => {
        it('returns null when the task did not fail', () => {
            expect(getDeepestFailedExecution({currentPath: [], execution: makeTaskExecution('task-1')})).toBeNull();
        });

        it('returns a failed task with its id appended to the path', () => {
            const taskExecution = makeTaskExecution('task-1', {error: executionError});

            expect(getDeepestFailedExecution({currentPath: ['root'], execution: taskExecution})).toEqual({
                execution: taskExecution,
                path: ['root', 'task-1'],
            });
        });

        it('keeps the current path when the failed task has no id', () => {
            const taskExecution = makeTaskExecution(undefined, {error: executionError});

            expect(getDeepestFailedExecution({currentPath: ['root'], execution: taskExecution})).toEqual({
                execution: taskExecution,
                path: ['root'],
            });
        });

        it('does not mutate the current path', () => {
            const currentPath = ['root'];

            getDeepestFailedExecution({currentPath, execution: makeTaskExecution('task-1', {error: executionError})});

            expect(currentPath).toEqual(['root']);
        });

        it('treats empty children and iterations as no nesting', () => {
            const taskExecution = makeTaskExecution('task-1', {children: [], error: executionError, iterations: []});

            expect(getDeepestFailedExecution({currentPath: [], execution: taskExecution})).toEqual({
                execution: taskExecution,
                path: ['task-1'],
            });
        });
    });

    describe('children', () => {
        it('returns the first failed child with the parent and child ids in the path', () => {
            const firstFailedChild = makeTaskExecution('child-2', {error: executionError});
            const secondFailedChild = makeTaskExecution('child-3', {error: executionError});

            const parentExecution = makeTaskExecution('parent-1', {
                children: [makeTaskExecution('child-1'), firstFailedChild, secondFailedChild],
                error: executionError,
            });

            expect(getDeepestFailedExecution({currentPath: [], execution: parentExecution})).toEqual({
                execution: firstFailedChild,
                path: ['parent-1', 'child-2'],
            });
        });

        it('descends through several levels of children', () => {
            const deepestChild = makeTaskExecution('grandchild-1', {error: executionError});

            const parentExecution = makeTaskExecution('parent-1', {
                children: [makeTaskExecution('child-1', {children: [deepestChild], error: executionError})],
                error: executionError,
            });

            expect(getDeepestFailedExecution({currentPath: ['job-1'], execution: parentExecution})).toEqual({
                execution: deepestChild,
                path: ['job-1', 'parent-1', 'child-1', 'grandchild-1'],
            });
        });

        it('returns null when no child failed and the parent did not fail', () => {
            const parentExecution = makeTaskExecution('parent-1', {
                children: [makeTaskExecution('child-1'), makeTaskExecution('child-2')],
            });

            expect(getDeepestFailedExecution({currentPath: [], execution: parentExecution})).toBeNull();
        });
    });

    describe('iterations', () => {
        it('returns the failed iteration task with an iteration segment in the path', () => {
            const failedIterationTask = makeTaskExecution('task-b', {error: executionError});

            const loopExecution = makeTaskExecution('loop-1', {
                error: executionError,
                iterations: [[makeTaskExecution('task-a')], [makeTaskExecution('task-a'), failedIterationTask]],
            });

            expect(getDeepestFailedExecution({currentPath: [], execution: loopExecution})).toEqual({
                execution: failedIterationTask,
                path: ['loop-1', 'loop-1-iteration-1', 'task-b'],
            });
        });

        it('returns the failure from the earliest iteration', () => {
            const firstFailure = makeTaskExecution('task-a', {error: {message: 'first'}});

            const loopExecution = makeTaskExecution('loop-1', {
                iterations: [[firstFailure], [makeTaskExecution('task-a', {error: {message: 'second'}})]],
            });

            expect(getDeepestFailedExecution({currentPath: [], execution: loopExecution})?.execution).toBe(
                firstFailure
            );
        });

        it('descends into children of an iteration task', () => {
            const deepestChild = makeTaskExecution('child-1', {error: executionError});

            const loopExecution = makeTaskExecution('loop-1', {
                iterations: [[makeTaskExecution('task-a', {children: [deepestChild]})]],
            });

            expect(getDeepestFailedExecution({currentPath: [], execution: loopExecution})).toEqual({
                execution: deepestChild,
                path: ['loop-1', 'loop-1-iteration-0', 'task-a', 'child-1'],
            });
        });

        it('takes iterations over children when both are present', () => {
            const failedChild = makeTaskExecution('child-1', {error: executionError});

            const loopExecution = makeTaskExecution('loop-1', {
                children: [failedChild],
                iterations: [[makeTaskExecution('task-a')]],
            });

            expect(getDeepestFailedExecution({currentPath: [], execution: loopExecution})).toBeNull();
        });

        it('returns null when no iteration task failed and the loop did not fail', () => {
            const loopExecution = makeTaskExecution('loop-1', {
                iterations: [[makeTaskExecution('task-a')], []],
            });

            expect(getDeepestFailedExecution({currentPath: [], execution: loopExecution})).toBeNull();
        });
    });
});
