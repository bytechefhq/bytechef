import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {describe, expect, it} from 'vitest';

import {getConditionBranchSide, hasTaskInConditionBranches} from './createConditionEdges';

describe('createConditionEdges', () => {
    describe('hasTaskInConditionBranches', () => {
        it('should handle non-array caseTrue parameter without throwing error', () => {
            const tasks: WorkflowTask[] = [
                {
                    name: 'test-condition',
                    parameters: {
                        caseFalse: [{name: 'task1'}, {name: 'task2'}],
                        caseTrue: 'not-an-array', // This should not cause an error
                    },
                    type: 'condition/v1',
                },
            ];

            // This should not throw an error
            expect(() => {
                hasTaskInConditionBranches('test-condition', 'task1', tasks);
            }).not.toThrow();

            // Should return true for task1 (in caseFalse array)
            expect(hasTaskInConditionBranches('test-condition', 'task1', tasks)).toBe(true);

            // Should return false for non-existent task
            expect(hasTaskInConditionBranches('test-condition', 'non-existent', tasks)).toBe(false);
        });

        it('should handle non-array caseFalse parameter without throwing error', () => {
            const tasks: WorkflowTask[] = [
                {
                    name: 'test-condition',
                    parameters: {
                        caseFalse: {not: 'an-array'}, // This should not cause an error
                        caseTrue: [{name: 'task1'}, {name: 'task2'}],
                    },
                    type: 'condition/v1',
                },
            ];

            // This should not throw an error
            expect(() => {
                hasTaskInConditionBranches('test-condition', 'task1', tasks);
            }).not.toThrow();

            // Should return true for task1 (in caseTrue array)
            expect(hasTaskInConditionBranches('test-condition', 'task1', tasks)).toBe(true);
        });

        it('should handle both non-array parameters without throwing error', () => {
            const tasks: WorkflowTask[] = [
                {
                    name: 'test-condition',
                    parameters: {
                        caseFalse: {not: 'an-array'},
                        caseTrue: 'not-an-array',
                    },
                    type: 'condition/v1',
                },
            ];

            // This should not throw an error
            expect(() => {
                hasTaskInConditionBranches('test-condition', 'any-task', tasks);
            }).not.toThrow();

            // Should return false since no arrays contain tasks
            expect(hasTaskInConditionBranches('test-condition', 'any-task', tasks)).toBe(false);
        });

        it('should work correctly with valid array parameters', () => {
            const tasks: WorkflowTask[] = [
                {
                    name: 'test-condition',
                    parameters: {
                        caseFalse: [{name: 'task3'}, {name: 'task4'}],
                        caseTrue: [{name: 'task1'}, {name: 'task2'}],
                    },
                    type: 'condition/v1',
                },
            ];

            expect(hasTaskInConditionBranches('test-condition', 'task1', tasks)).toBe(true);
            expect(hasTaskInConditionBranches('test-condition', 'task3', tasks)).toBe(true);
            expect(hasTaskInConditionBranches('test-condition', 'non-existent', tasks)).toBe(false);
        });
    });

    describe('getConditionBranchSide', () => {
        it('should handle non-array caseTrue parameter without throwing error', () => {
            const tasks: WorkflowTask[] = [
                {
                    name: 'parent-condition',
                    parameters: {
                        caseFalse: [{name: 'task1'}],
                        caseTrue: 'not-an-array', // This should not cause an error
                    },
                    type: 'condition/v1',
                },
            ];

            // This should not throw an error
            expect(() => {
                getConditionBranchSide('child-condition', tasks, 'parent-condition');
            }).not.toThrow();

            // Should return 'right' since caseTrue is not an array
            expect(getConditionBranchSide('child-condition', tasks, 'parent-condition')).toBe('right');
        });

        it('should work correctly with valid array parameters', () => {
            const tasks: WorkflowTask[] = [
                {
                    name: 'parent-condition',
                    parameters: {
                        caseFalse: [{name: 'other-task'}],
                        caseTrue: [{name: 'child-condition'}],
                    },
                    type: 'condition/v1',
                },
            ];

            // Should return 'left' since child-condition is in caseTrue
            expect(getConditionBranchSide('child-condition', tasks, 'parent-condition')).toBe('left');

            // Should return 'right' since other-task is not in caseTrue
            expect(getConditionBranchSide('other-task', tasks, 'parent-condition')).toBe('right');
        });

        it('should return right when parent condition is not found', () => {
            const tasks: WorkflowTask[] = [];

            expect(getConditionBranchSide('child-condition', tasks, 'non-existent-parent')).toBe('right');
        });
    });
});
