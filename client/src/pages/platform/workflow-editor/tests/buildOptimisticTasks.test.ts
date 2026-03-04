import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {WorkflowTaskType} from '@/shared/types';
import {describe, expect, it} from 'vitest';

import {buildOptimisticTasks} from '../utils/handleDeleteTask';

function makeRichTask(name: string, parameters?: Record<string, unknown>): WorkflowTask {
    return {
        name,
        parameters: parameters || {},
        type: `${name.split('_')[0]}/v1`,
    } as WorkflowTask;
}

function makeDefinitionTask(name: string, parameters?: Record<string, unknown>): WorkflowTaskType {
    return {
        name,
        parameters: parameters || {},
        type: `${name.split('_')[0]}/v1`,
    } as WorkflowTaskType;
}

describe('buildOptimisticTasks', () => {
    it('should remove a top-level task by name', () => {
        const workflowTasks = [makeRichTask('logger_1'), makeRichTask('httpClient_1')];
        const updatedTasks = [makeDefinitionTask('httpClient_1')];

        const result = buildOptimisticTasks(workflowTasks, updatedTasks, 'logger_1');

        expect(result).toHaveLength(1);
        expect(result[0].name).toBe('httpClient_1');
    });

    it('should update parent condition parameters when deleting a nested task', () => {
        const originalConditionParams = {
            caseFalse: [{name: 'httpClient_2', type: 'httpClient/v1'}],
            caseTrue: [{name: 'logger_1', type: 'logger/v1'}],
        };

        const updatedConditionParams = {
            caseFalse: [{name: 'httpClient_2', type: 'httpClient/v1'}],
            caseTrue: [],
        };

        const workflowTasks = [makeRichTask('condition_1', originalConditionParams)];

        const updatedTasks = [makeDefinitionTask('condition_1', updatedConditionParams)];

        const result = buildOptimisticTasks(workflowTasks, updatedTasks, 'logger_1');

        expect(result).toHaveLength(1);
        expect(result[0].name).toBe('condition_1');
        expect(result[0].parameters?.caseTrue).toEqual([]);
        expect(result[0].parameters?.caseFalse).toEqual([{name: 'httpClient_2', type: 'httpClient/v1'}]);
    });

    it('should update parent loop parameters when deleting a nested iteratee task', () => {
        const originalLoopParams = {
            iteratee: [
                {name: 'logger_1', type: 'logger/v1'},
                {name: 'httpClient_1', type: 'httpClient/v1'},
            ],
        };

        const updatedLoopParams = {
            iteratee: [{name: 'httpClient_1', type: 'httpClient/v1'}],
        };

        const workflowTasks = [makeRichTask('loop_1', originalLoopParams)];
        const updatedTasks = [makeDefinitionTask('loop_1', updatedLoopParams)];

        const result = buildOptimisticTasks(workflowTasks, updatedTasks, 'logger_1');

        expect(result).toHaveLength(1);
        expect(result[0].parameters?.iteratee).toEqual([{name: 'httpClient_1', type: 'httpClient/v1'}]);
    });

    it('should update parent branch parameters when deleting a nested task from a case', () => {
        const originalBranchParams = {
            cases: [{key: 'case1', tasks: [{name: 'logger_1', type: 'logger/v1'}]}],
            default: [{name: 'httpClient_1', type: 'httpClient/v1'}],
        };

        const updatedBranchParams = {
            cases: [{key: 'case1', tasks: []}],
            default: [{name: 'httpClient_1', type: 'httpClient/v1'}],
        };

        const workflowTasks = [makeRichTask('branch_1', originalBranchParams)];
        const updatedTasks = [makeDefinitionTask('branch_1', updatedBranchParams)];

        const result = buildOptimisticTasks(workflowTasks, updatedTasks, 'logger_1');

        expect(result).toHaveLength(1);
        expect(result[0].parameters?.cases).toEqual([{key: 'case1', tasks: []}]);
    });

    it('should preserve rich task properties when updating parameters', () => {
        const richTask = {
            ...makeRichTask('condition_1', {caseTrue: [{name: 'logger_1', type: 'logger/v1'}]}),
            componentName: 'condition',
            icon: 'condition-icon',
            taskDispatcher: true,
        } as WorkflowTask;

        const updatedTasks = [makeDefinitionTask('condition_1', {caseTrue: []})];

        const result = buildOptimisticTasks([richTask], updatedTasks, 'logger_1');

        expect(result).toHaveLength(1);
        const resultTask = result[0] as unknown as Record<string, unknown>;

        expect(resultTask.componentName).toBe('condition');
        expect(resultTask.icon).toBe('condition-icon');
        expect(resultTask.taskDispatcher).toBe(true);
        expect(result[0].parameters?.caseTrue).toEqual([]);
    });

    it('should not modify tasks whose parameters have not changed', () => {
        const sharedParams = {text: 'hello'};
        const workflowTasks = [makeRichTask('condition_1', {caseTrue: []}), makeRichTask('logger_1', sharedParams)];

        const updatedTasks = [
            makeDefinitionTask('condition_1', {caseTrue: []}),
            makeDefinitionTask('logger_1', sharedParams),
        ];

        const result = buildOptimisticTasks(workflowTasks, updatedTasks, 'nonExistent_1');

        expect(result).toHaveLength(2);

        // logger_1 shares the same params reference, so the original object should be returned
        expect(result[1]).toBe(workflowTasks[1]);
    });

    it('should handle empty workflow tasks', () => {
        const result = buildOptimisticTasks([], [], 'logger_1');

        expect(result).toEqual([]);
    });

    it('should handle parallel task dispatcher nested deletion', () => {
        const originalParams = {
            tasks: [
                {name: 'logger_1', type: 'logger/v1'},
                {name: 'httpClient_1', type: 'httpClient/v1'},
            ],
        };

        const updatedParams = {
            tasks: [{name: 'httpClient_1', type: 'httpClient/v1'}],
        };

        const workflowTasks = [makeRichTask('parallel_1', originalParams)];
        const updatedTasks = [makeDefinitionTask('parallel_1', updatedParams)];

        const result = buildOptimisticTasks(workflowTasks, updatedTasks, 'logger_1');

        expect(result).toHaveLength(1);
        expect(result[0].parameters?.tasks).toEqual([{name: 'httpClient_1', type: 'httpClient/v1'}]);
    });
});
