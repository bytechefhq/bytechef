import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {
    BranchChildTasksType,
    ConditionChildTasksType,
    EachChildTasksType,
    ForkJoinChildTasksType,
    LoopChildTasksType,
    ParallelChildTasksType,
} from '@/shared/types';
import {describe, expect, it} from 'vitest';

import {collectTaskDispatcherData} from './layoutUtils';

// Type for test tasks with potentially malformed parameters
type TestTaskType = Omit<WorkflowTask, 'parameters'> & {
    parameters?: {
        caseFalse?: unknown;
        caseTrue?: unknown;
        branches?: unknown;
        iteratee?: unknown;
        tasks?: unknown;
    };
};

describe('collectTaskDispatcherData', () => {
    it('should handle non-array caseFalse parameter without throwing error', () => {
        const task: TestTaskType = {
            name: 'test-condition',
            parameters: {
                caseFalse: 'not-an-array', // This should not cause an error
                caseTrue: [{name: 'task1'}, {name: 'task2'}],
            },
            type: 'condition/v1',
        };

        const branchChildTasks: BranchChildTasksType = {};
        const conditionChildTasks: ConditionChildTasksType = {};
        const eachChildTasks: EachChildTasksType = {};
        const forkJoinChildTasks: ForkJoinChildTasksType = {};
        const loopChildTasks: LoopChildTasksType = {};
        const parallelChildTasks: ParallelChildTasksType = {};

        // This should not throw an error
        expect(() => {
            collectTaskDispatcherData(
                task,
                branchChildTasks,
                conditionChildTasks,
                eachChildTasks,
                forkJoinChildTasks,
                loopChildTasks,
                parallelChildTasks
            );
        }).not.toThrow();

        // Should create empty array for caseFalse since it's not an array
        expect(conditionChildTasks['test-condition'].caseFalse).toEqual([]);
        expect(conditionChildTasks['test-condition'].caseTrue).toEqual(['task1', 'task2']);
    });

    it('should handle non-array caseTrue parameter without throwing error', () => {
        const task: TestTaskType = {
            name: 'test-condition',
            parameters: {
                caseFalse: [{name: 'task1'}, {name: 'task2'}],
                caseTrue: {not: 'an-array'}, // This should not cause an error
            },
            type: 'condition/v1',
        };

        const branchChildTasks: BranchChildTasksType = {};
        const conditionChildTasks: ConditionChildTasksType = {};
        const eachChildTasks: EachChildTasksType = {};
        const forkJoinChildTasks: ForkJoinChildTasksType = {};
        const loopChildTasks: LoopChildTasksType = {};
        const parallelChildTasks: ParallelChildTasksType = {};

        // This should not throw an error
        expect(() => {
            collectTaskDispatcherData(
                task,
                branchChildTasks,
                conditionChildTasks,
                eachChildTasks,
                forkJoinChildTasks,
                loopChildTasks,
                parallelChildTasks
            );
        }).not.toThrow();

        // Should create empty array for caseTrue since it's not an array
        expect(conditionChildTasks['test-condition'].caseFalse).toEqual(['task1', 'task2']);
        expect(conditionChildTasks['test-condition'].caseTrue).toEqual([]);
    });

    it('should handle valid array parameters correctly', () => {
        const task: WorkflowTask = {
            name: 'test-condition',
            parameters: {
                caseFalse: [{name: 'task1'}, {name: 'task2'}],
                caseTrue: [{name: 'task3'}, {name: 'task4'}],
            },
            type: 'condition/v1',
        };

        const branchChildTasks: BranchChildTasksType = {};
        const conditionChildTasks: ConditionChildTasksType = {};
        const eachChildTasks: EachChildTasksType = {};
        const forkJoinChildTasks: ForkJoinChildTasksType = {};
        const loopChildTasks: LoopChildTasksType = {};
        const parallelChildTasks: ParallelChildTasksType = {};

        collectTaskDispatcherData(
            task,
            branchChildTasks,
            conditionChildTasks,
            eachChildTasks,
            forkJoinChildTasks,
            loopChildTasks,
            parallelChildTasks
        );

        expect(conditionChildTasks['test-condition'].caseFalse).toEqual(['task1', 'task2']);
        expect(conditionChildTasks['test-condition'].caseTrue).toEqual(['task3', 'task4']);

        // Additional assertions to ensure other collections are not affected
        expect(branchChildTasks).toEqual({});
        expect(eachChildTasks).toEqual({});
        expect(forkJoinChildTasks).toEqual({});
        expect(loopChildTasks).toEqual({});
        expect(parallelChildTasks).toEqual({});
    });

    it('should handle valid array parameters for fork-join correctly', () => {
        const task: WorkflowTask = {
            name: 'test-fork-join',
            parameters: {
                branches: [
                    [{name: 'task1'}, {name: 'task2'}], // First branch
                    [{name: 'task3'}, {name: 'task4'}], // Second branch
                ],
            },
            type: 'fork-join/v1',
        };

        const branchChildTasks: BranchChildTasksType = {};
        const conditionChildTasks: ConditionChildTasksType = {};
        const eachChildTasks: EachChildTasksType = {};
        const forkJoinChildTasks: ForkJoinChildTasksType = {};
        const loopChildTasks: LoopChildTasksType = {};
        const parallelChildTasks: ParallelChildTasksType = {};

        collectTaskDispatcherData(
            task,
            branchChildTasks,
            conditionChildTasks,
            eachChildTasks,
            forkJoinChildTasks,
            loopChildTasks,
            parallelChildTasks
        );

        // Should preserve the array-of-arrays structure (each branch is separate)
        expect(forkJoinChildTasks['test-fork-join'].branches).toEqual([
            ['task1', 'task2'],
            ['task3', 'task4'],
        ]);
    });

    it('should handle fork-join with empty branches', () => {
        const task: WorkflowTask = {
            name: 'test-fork-join-empty',
            parameters: {
                branches: [],
            },
            type: 'fork-join/v1',
        };

        const branchChildTasks: BranchChildTasksType = {};
        const conditionChildTasks: ConditionChildTasksType = {};
        const eachChildTasks: EachChildTasksType = {};
        const forkJoinChildTasks: ForkJoinChildTasksType = {};
        const loopChildTasks: LoopChildTasksType = {};
        const parallelChildTasks: ParallelChildTasksType = {};

        collectTaskDispatcherData(
            task,
            branchChildTasks,
            conditionChildTasks,
            eachChildTasks,
            forkJoinChildTasks,
            loopChildTasks,
            parallelChildTasks
        );

        expect(forkJoinChildTasks['test-fork-join-empty'].branches).toEqual([]);
    });

    it('should handle fork-join with single empty branch', () => {
        const task: WorkflowTask = {
            name: 'test-fork-join-single-empty',
            parameters: {
                branches: [[]],
            },
            type: 'fork-join/v1',
        };

        const branchChildTasks: BranchChildTasksType = {};
        const conditionChildTasks: ConditionChildTasksType = {};
        const eachChildTasks: EachChildTasksType = {};
        const forkJoinChildTasks: ForkJoinChildTasksType = {};
        const loopChildTasks: LoopChildTasksType = {};
        const parallelChildTasks: ParallelChildTasksType = {};

        collectTaskDispatcherData(
            task,
            branchChildTasks,
            conditionChildTasks,
            eachChildTasks,
            forkJoinChildTasks,
            loopChildTasks,
            parallelChildTasks
        );

        expect(forkJoinChildTasks['test-fork-join-single-empty'].branches).toEqual([[]]);
    });

    it('should handle fork-join with malformed branch data', () => {
        const task: TestTaskType = {
            name: 'test-fork-join-malformed',
            parameters: {
                branches: [
                    [{name: 'task1'}, {name: 'task2'}], // Valid branch
                    'not-an-array', // Malformed branch
                    [{name: 'task3'}], // Valid branch
                ],
            },
            type: 'fork-join/v1',
        };

        const branchChildTasks: BranchChildTasksType = {};
        const conditionChildTasks: ConditionChildTasksType = {};
        const eachChildTasks: EachChildTasksType = {};
        const forkJoinChildTasks: ForkJoinChildTasksType = {};
        const loopChildTasks: LoopChildTasksType = {};
        const parallelChildTasks: ParallelChildTasksType = {};

        collectTaskDispatcherData(
            task,
            branchChildTasks,
            conditionChildTasks,
            eachChildTasks,
            forkJoinChildTasks,
            loopChildTasks,
            parallelChildTasks
        );

        // Should handle malformed branches gracefully
        expect(forkJoinChildTasks['test-fork-join-malformed'].branches).toEqual([['task1', 'task2'], [], ['task3']]);
    });

    it('should handle non-array parameters for other task types', () => {
        const loopTask: TestTaskType = {
            name: 'test-loop',
            parameters: {
                iteratee: 'not-an-array', // This should not cause an error
            },
            type: 'loop/v1',
        };

        const parallelTask: TestTaskType = {
            name: 'test-parallel',
            parameters: {
                tasks: {not: 'an-array'}, // This should not cause an error
            },
            type: 'parallel/v1',
        };

        const forkJoinTask: TestTaskType = {
            name: 'test-fork-join',
            parameters: {
                branches: 'not-an-array', // This should not cause an error
            },
            type: 'fork-join/v1',
        };

        const branchChildTasks: BranchChildTasksType = {};
        const conditionChildTasks: ConditionChildTasksType = {};
        const eachChildTasks: EachChildTasksType = {};
        const forkJoinChildTasks: ForkJoinChildTasksType = {};
        const loopChildTasks: LoopChildTasksType = {};
        const parallelChildTasks: ParallelChildTasksType = {};

        // These should not throw errors
        expect(() => {
            collectTaskDispatcherData(
                loopTask,
                branchChildTasks,
                conditionChildTasks,
                eachChildTasks,
                forkJoinChildTasks,
                loopChildTasks,
                parallelChildTasks
            );
        }).not.toThrow();

        expect(() => {
            collectTaskDispatcherData(
                parallelTask,
                branchChildTasks,
                conditionChildTasks,
                eachChildTasks,
                forkJoinChildTasks,
                loopChildTasks,
                parallelChildTasks
            );
        }).not.toThrow();

        expect(() => {
            collectTaskDispatcherData(
                forkJoinTask,
                branchChildTasks,
                conditionChildTasks,
                eachChildTasks,
                forkJoinChildTasks,
                loopChildTasks,
                parallelChildTasks
            );
        }).not.toThrow();

        // Should create empty arrays for non-array parameters
        expect(loopChildTasks['test-loop'].iteratee).toEqual([]);
        expect(parallelChildTasks['test-parallel'].tasks).toEqual([]);
        expect(forkJoinChildTasks['test-fork-join'].branches).toEqual([]);
    });
});
