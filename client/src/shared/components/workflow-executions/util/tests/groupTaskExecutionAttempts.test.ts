import {groupTaskExecutionAttempts} from '@/shared/components/workflow-executions/util/groupTaskExecutionAttempts';
import {TaskExecution} from '@/shared/middleware/platform/workflow/execution';
import {describe, expect, it} from 'vitest';

function createTaskExecution(id: string, name: string | undefined, status = 'COMPLETED'): TaskExecution {
    return {
        id,
        jobId: '1',
        priority: 0,
        startDate: new Date('2026-09-07T18:47:00Z'),
        status,
        workflowTask: name ? {name, type: 'condition/v1'} : undefined,
    } as TaskExecution;
}

describe('groupTaskExecutionAttempts', () => {
    it('keeps one entry per task when every task ran once', () => {
        const dataTable = createTaskExecution('1', 'dataTable_1');
        const condition = createTaskExecution('2', 'condition_1');

        expect(groupTaskExecutionAttempts([dataTable, condition])).toEqual([
            {latestTaskExecution: dataTable, previousTaskExecutions: []},
            {latestTaskExecution: condition, previousTaskExecutions: []},
        ]);
    });

    it('collapses restarted attempts of a task into its latest attempt, oldest first', () => {
        const dataTable = createTaskExecution('1', 'dataTable_1');
        const firstCondition = createTaskExecution('2', 'condition_1', 'FAILED');
        const secondCondition = createTaskExecution('3', 'condition_1', 'FAILED');
        const thirdCondition = createTaskExecution('4', 'condition_1');

        expect(groupTaskExecutionAttempts([dataTable, firstCondition, secondCondition, thirdCondition])).toEqual([
            {latestTaskExecution: dataTable, previousTaskExecutions: []},
            {latestTaskExecution: thirdCondition, previousTaskExecutions: [firstCondition, secondCondition]},
        ]);
    });

    it('keeps the position of the first attempt when later tasks ran after a restart', () => {
        const failedCondition = createTaskExecution('1', 'condition_1', 'FAILED');
        const restartedCondition = createTaskExecution('2', 'condition_1');
        const nextTask = createTaskExecution('3', 'dataTable_2');

        expect(
            groupTaskExecutionAttempts([failedCondition, restartedCondition, nextTask]).map(
                (taskExecutionAttempts) => taskExecutionAttempts.latestTaskExecution.id
            )
        ).toEqual(['2', '3']);
    });

    it('never groups task executions without a task name', () => {
        const first = createTaskExecution('1', undefined);
        const second = createTaskExecution('2', undefined);

        expect(groupTaskExecutionAttempts([first, second])).toHaveLength(2);
    });
});
