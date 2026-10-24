import toClusterElementCallTaskExecutions from '@/shared/components/workflow-executions/util/toClusterElementCallTaskExecutions';
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

    it('should keep a clicked tool call row selected, with its latest state, across progress snapshots', () => {
        const createAgentTaskExecution = (toolCallStatus: string, output?: string, subAgentToolCallStatus?: string) =>
            createTaskExecution('10', 'STARTED', {
                clusterElementCallRecords: [
                    {id: 'call-1', output, status: toolCallStatus, workflowNodeName: 'httpClient_1'},
                    {id: 'call-2', status: 'STARTED', workflowNodeName: 'aiAgentUtils_1'},
                    {
                        id: 'call-3',
                        parentId: 'call-2',
                        status: subAgentToolCallStatus ?? 'STARTED',
                        workflowNodeName: 'httpClient_2',
                    },
                ],
            } as Partial<TaskExecution>);

        const {rerender, result} = renderHook(
            ({workflowTestExecution}) => useWorkflowExecutions({workflowTestExecution}),
            {initialProps: {workflowTestExecution: createSnapshot('STARTED', [createAgentTaskExecution('STARTED')])}}
        );

        const [toolCallRow, subAgentCallRow] = toClusterElementCallTaskExecutions(result.current.taskExecutions[0]);

        act(() => {
            result.current.handleExecutionClick(toolCallRow as TaskExecution);
        });

        rerender({workflowTestExecution: createSnapshot('STARTED', [createAgentTaskExecution('COMPLETED', '"ok"')])});

        expect(result.current.selectedExecution?.id).toBe('call-1');
        expect((result.current.selectedExecution as TaskExecution).status).toBe('COMPLETED');
        expect((result.current.selectedExecution as TaskExecution).output).toBe('ok');

        act(() => {
            result.current.handleExecutionClick(subAgentCallRow.children![0]);
        });

        rerender({
            workflowTestExecution: createSnapshot('STARTED', [createAgentTaskExecution('COMPLETED', '"ok"', 'FAILED')]),
        });

        expect(result.current.selectedExecution?.id).toBe('call-3');
        expect((result.current.selectedExecution as TaskExecution).status).toBe('FAILED');
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
