import {describe, expect, it} from 'vitest';

import {collectTaskDispatcherData} from './layoutUtils';

describe('collectTaskDispatcherData', () => {
    it('should handle non-array caseFalse parameter without throwing error', () => {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        const task: any = {
            name: 'test-condition',
            parameters: {
                caseFalse: 'not-an-array', // This should not cause an error
                caseTrue: [{name: 'task1'}, {name: 'task2'}],
            },
            type: 'condition/v1',
        };

        const branchChildTasks = {};
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        const conditionChildTasks = {} as any;
        const eachChildTasks = {};
        const loopChildTasks = {};
        const parallelChildTasks = {};

        // This should not throw an error
        expect(() => {
            collectTaskDispatcherData(
                task,
                branchChildTasks,
                conditionChildTasks,
                eachChildTasks,
                loopChildTasks,
                parallelChildTasks
            );
        }).not.toThrow();

        // Should create empty array for caseFalse since it's not an array
        expect(conditionChildTasks['test-condition'].caseFalse).toEqual([]);
        expect(conditionChildTasks['test-condition'].caseTrue).toEqual(['task1', 'task2']);
    });

    it('should handle non-array caseTrue parameter without throwing error', () => {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        const task: any = {
            name: 'test-condition',
            parameters: {
                caseFalse: [{name: 'task1'}, {name: 'task2'}],
                caseTrue: {not: 'an-array'}, // This should not cause an error
            },
            type: 'condition/v1',
        };

        const branchChildTasks = {};
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        const conditionChildTasks = {} as any;
        const eachChildTasks = {};
        const loopChildTasks = {};
        const parallelChildTasks = {};

        // This should not throw an error
        expect(() => {
            collectTaskDispatcherData(
                task,
                branchChildTasks,
                conditionChildTasks,
                eachChildTasks,
                loopChildTasks,
                parallelChildTasks
            );
        }).not.toThrow();

        // Should create empty array for caseTrue since it's not an array
        expect(conditionChildTasks['test-condition'].caseFalse).toEqual(['task1', 'task2']);
        expect(conditionChildTasks['test-condition'].caseTrue).toEqual([]);
    });

    it('should handle valid array parameters correctly', () => {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        const task: any = {
            name: 'test-condition',
            parameters: {
                caseFalse: [{name: 'task1'}, {name: 'task2'}],
                caseTrue: [{name: 'task3'}, {name: 'task4'}],
            },
            type: 'condition/v1',
        };

        const branchChildTasks = {};
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        const conditionChildTasks = {} as any;
        const eachChildTasks = {};
        const loopChildTasks = {};
        const parallelChildTasks = {};

        collectTaskDispatcherData(
            task,
            branchChildTasks,
            conditionChildTasks,
            eachChildTasks,
            loopChildTasks,
            parallelChildTasks
        );

        expect(conditionChildTasks['test-condition'].caseFalse).toEqual(['task1', 'task2']);
        expect(conditionChildTasks['test-condition'].caseTrue).toEqual(['task3', 'task4']);
    });

    it('should handle non-array parameters for other task types', () => {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        const loopTask: any = {
            name: 'test-loop',
            parameters: {
                iteratee: 'not-an-array', // This should not cause an error
            },
            type: 'loop/v1',
        };

        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        const parallelTask: any = {
            name: 'test-parallel',
            parameters: {
                tasks: {not: 'an-array'}, // This should not cause an error
            },
            type: 'parallel/v1',
        };

        const branchChildTasks = {};
        const conditionChildTasks = {};
        const eachChildTasks = {};
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        const loopChildTasks = {} as any;
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        const parallelChildTasks = {} as any;

        // These should not throw errors
        expect(() => {
            collectTaskDispatcherData(
                loopTask,
                branchChildTasks,
                conditionChildTasks,
                eachChildTasks,
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
                loopChildTasks,
                parallelChildTasks
            );
        }).not.toThrow();

        // Should create empty arrays for non-array parameters
        expect(loopChildTasks['test-loop'].iteratee).toEqual([]);
        expect(parallelChildTasks['test-parallel'].tasks).toEqual([]);
    });
});
