import {TaskExecution, WorkflowTestExecution} from '@/shared/middleware/platform/workflow/test';
import {act, renderHook} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';

import useWorkflowExecutions from '../useWorkflowExecutions';

vi.mock('@/shared/components/copilot/stores/useCopilotStore', () => ({
    useCopilotStore: {getState: () => ({setWorkflowExecutionError: vi.fn()})},
}));

const createTaskExecution = (id: string, status: string, overrides: Partial<TaskExecution> = {}): TaskExecution =>
    ({
        id,
        jobId: '1',
        priority: 0,
        startDate: new Date(),
        status,
        workflowTask: {name: `task_${id}`},
        ...overrides,
    }) as TaskExecution;

const createSnapshot = (jobStatus: string, taskExecutions: TaskExecution[]): WorkflowTestExecution =>
    ({
        job: {id: '1', priority: 0, status: jobStatus, taskExecutions},
    }) as WorkflowTestExecution;

describe('useWorkflowExecutions', () => {
    it('should keep the clicked execution selected, with its latest state, across progress snapshots', () => {
        const {rerender, result} = renderHook(
            ({workflowTestExecution}) => useWorkflowExecutions({workflowTestExecution}),
            {
                initialProps: {
                    workflowTestExecution: createSnapshot('STARTED', [
                        createTaskExecution('10', 'COMPLETED'),
                        createTaskExecution('11', 'STARTED'),
                    ]),
                },
            }
        );

        expect(result.current.selectedExecution?.id).toBe('10');

        act(() => {
            result.current.handleExecutionClick(result.current.taskExecutions[1]);
        });

        rerender({
            workflowTestExecution: createSnapshot('STARTED', [
                createTaskExecution('10', 'COMPLETED'),
                createTaskExecution('11', 'COMPLETED', {output: {value: 'done'}}),
                createTaskExecution('12', 'STARTED'),
            ]),
        });

        expect(result.current.selectedExecution?.id).toBe('11');
        expect((result.current.selectedExecution as TaskExecution).status).toBe('COMPLETED');
        expect((result.current.selectedExecution as TaskExecution).output).toEqual({value: 'done'});
    });

    it('should select the first execution once it appears in a run that started with none', () => {
        const {rerender, result} = renderHook(
            ({workflowTestExecution}) => useWorkflowExecutions({workflowTestExecution}),
            {initialProps: {workflowTestExecution: createSnapshot('STARTED', [])}}
        );

        expect(result.current.selectedExecution).toBeUndefined();

        rerender({workflowTestExecution: createSnapshot('STARTED', [createTaskExecution('10', 'STARTED')])});

        expect(result.current.selectedExecution?.id).toBe('10');
    });

    it('should move to the failed execution when the run finishes with a failure', () => {
        const {rerender, result} = renderHook(
            ({workflowTestExecution}) => useWorkflowExecutions({workflowTestExecution}),
            {
                initialProps: {
                    workflowTestExecution: createSnapshot('STARTED', [
                        createTaskExecution('10', 'COMPLETED'),
                        createTaskExecution('11', 'STARTED'),
                    ]),
                },
            }
        );

        expect(result.current.selectedExecution?.id).toBe('10');

        rerender({
            workflowTestExecution: createSnapshot('FAILED', [
                createTaskExecution('10', 'COMPLETED'),
                createTaskExecution('11', 'FAILED', {error: {message: 'Boom', stackTrace: []}}),
            ]),
        });

        expect(result.current.selectedExecution?.id).toBe('11');
        expect(result.current.activeTab).toBe('error');
    });
});
