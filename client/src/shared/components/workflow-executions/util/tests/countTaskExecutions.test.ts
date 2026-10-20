import {countTaskExecutions} from '@/shared/components/workflow-executions/util/countTaskExecutions';
import {TaskExecution} from '@/shared/middleware/platform/workflow/execution';
import {describe, expect, it} from 'vitest';

function createTaskExecution(id: string, name: string, overrides: Partial<TaskExecution> = {}): TaskExecution {
    return {
        id,
        jobId: '1',
        priority: 0,
        startDate: new Date('2026-10-20T21:32:41Z'),
        status: 'COMPLETED',
        workflowTask: {name, type: 'logger/v1/info'},
        ...overrides,
    } as TaskExecution;
}

describe('countTaskExecutions', () => {
    it('counts each top-level task once', () => {
        expect(countTaskExecutions([createTaskExecution('1', 'logger_1'), createTaskExecution('2', 'logger_2')])).toBe(
            2
        );
    });

    it('counts a restarted task once', () => {
        expect(
            countTaskExecutions([
                createTaskExecution('1', 'condition_1', {status: 'FAILED'}),
                createTaskExecution('2', 'condition_1'),
            ])
        ).toBe(1);
    });

    it('counts the tasks of every loop iteration', () => {
        const loop = createTaskExecution('1', 'loop_1', {
            iterations: [
                [createTaskExecution('2', 'logger_1')],
                [createTaskExecution('3', 'logger_1')],
                [createTaskExecution('4', 'logger_1')],
            ],
        });

        expect(countTaskExecutions([loop])).toBe(4);
    });

    it('counts the tasks of nested loop iterations', () => {
        const innerLoop = (id: string) =>
            createTaskExecution(id, 'loop_2', {
                iterations: [
                    [createTaskExecution(`${id}-a`, 'logger_1')],
                    [createTaskExecution(`${id}-b`, 'logger_1')],
                ],
            });

        const outerLoop = createTaskExecution('1', 'loop_1', {iterations: [[innerLoop('2')], [innerLoop('3')]]});

        expect(countTaskExecutions([outerLoop])).toBe(7);
    });

    it('counts the children of a dispatcher without iterations', () => {
        const condition = createTaskExecution('1', 'condition_1', {
            children: [createTaskExecution('2', 'logger_1'), createTaskExecution('3', 'logger_2')],
        });

        expect(countTaskExecutions([condition])).toBe(3);
    });

    it('counts a restarted task inside an iteration once', () => {
        const loop = createTaskExecution('1', 'loop_1', {
            iterations: [
                [createTaskExecution('2', 'logger_1', {status: 'FAILED'}), createTaskExecution('3', 'logger_1')],
            ],
        });

        expect(countTaskExecutions([loop])).toBe(2);
    });
});
