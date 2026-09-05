import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {renderHook, waitFor} from '@testing-library/react';
import {type ReactNode, createElement} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {WorkflowExecutionKeys, useGetProjectWorkflowExecutionQuery} from '../workflowExecutions.queries';

const {getTriggerExecutionWorkflowExecutionMock, getWorkflowExecutionMock} = vi.hoisted(() => ({
    getTriggerExecutionWorkflowExecutionMock: vi.fn(),
    getWorkflowExecutionMock: vi.fn(),
}));

vi.mock('@/shared/middleware/automation/workflow/execution', () => ({
    WorkflowExecutionApi: class {
        getTriggerExecutionWorkflowExecution = getTriggerExecutionWorkflowExecutionMock;
        getWorkflowExecution = getWorkflowExecutionMock;
    },
}));

const wrapper = ({children}: {children: ReactNode}) =>
    createElement(
        QueryClientProvider,
        {client: new QueryClient({defaultOptions: {queries: {retry: false}}})},
        children
    );

describe('workflowExecutions.queries', () => {
    describe('WorkflowExecutionKeys', () => {
        it('generates correct base key for workflow executions', () => {
            expect(WorkflowExecutionKeys.workflowExecutions).toEqual(['automation_workflowExecutions']);
        });

        it('generates correct key for workflow execution by id', () => {
            const result = WorkflowExecutionKeys.workflowExecution(123);

            expect(result).toEqual(['automation_workflowExecutions', 123]);
        });

        it('generates correct key for filtered workflow executions', () => {
            const request = {
                id: 1,
                pageNumber: 0,
                pageSize: 10,
                projectId: 1,
            };

            const result = WorkflowExecutionKeys.filteredWorkflowExecutions(request);

            expect(result).toEqual(['automation_workflowExecutions', request]);
        });

        it('keeps a trigger-only execution apart from the job with the same id', () => {
            const triggerKey = WorkflowExecutionKeys.workflowExecution(123, 'TRIGGER_EXECUTION');

            expect(triggerKey).toEqual(['automation_workflowExecutions', 'TRIGGER_EXECUTION', 123]);
            expect(triggerKey).not.toEqual(WorkflowExecutionKeys.workflowExecution(123));
        });

        it('generates unique keys for different workflow execution ids', () => {
            const key1 = WorkflowExecutionKeys.workflowExecution(1);
            const key2 = WorkflowExecutionKeys.workflowExecution(2);

            expect(key1).not.toEqual(key2);
            expect(key1).toEqual(['automation_workflowExecutions', 1]);
            expect(key2).toEqual(['automation_workflowExecutions', 2]);
        });

        it('generates unique keys for different filter requests', () => {
            const request1 = {id: 1, pageNumber: 0, projectId: 1};
            const request2 = {id: 1, pageNumber: 0, projectId: 2};

            const key1 = WorkflowExecutionKeys.filteredWorkflowExecutions(request1);
            const key2 = WorkflowExecutionKeys.filteredWorkflowExecutions(request2);

            expect(key1).not.toEqual(key2);
        });

        it('workflow execution key includes base workflow executions key', () => {
            const executionKey = WorkflowExecutionKeys.workflowExecution(123);

            expect(executionKey[0]).toBe(WorkflowExecutionKeys.workflowExecutions[0]);
        });

        it('filtered workflow executions key includes base workflow executions key', () => {
            const filteredKey = WorkflowExecutionKeys.filteredWorkflowExecutions({id: 1, pageNumber: 0});

            expect(filteredKey[0]).toBe(WorkflowExecutionKeys.workflowExecutions[0]);
        });
    });

    describe('useGetProjectWorkflowExecutionQuery', () => {
        beforeEach(() => {
            getTriggerExecutionWorkflowExecutionMock.mockReset();
            getWorkflowExecutionMock.mockReset();
            getTriggerExecutionWorkflowExecutionMock.mockResolvedValue({id: 77});
            getWorkflowExecutionMock.mockResolvedValue({id: 5});
        });

        it('asks the trigger execution endpoint for a trigger-only row', async () => {
            const {result} = renderHook(
                () => useGetProjectWorkflowExecutionQuery({id: 77}, true, undefined, 'TRIGGER_EXECUTION'),
                {wrapper}
            );

            await waitFor(() => expect(result.current.data).toEqual({id: 77}));

            expect(getTriggerExecutionWorkflowExecutionMock).toHaveBeenCalledWith({triggerExecutionId: 77});
            expect(getWorkflowExecutionMock).not.toHaveBeenCalled();
        });

        it('asks the job endpoint for a job-backed row', async () => {
            const {result} = renderHook(() => useGetProjectWorkflowExecutionQuery({id: 5}, true), {wrapper});

            await waitFor(() => expect(result.current.data).toEqual({id: 5}));

            expect(getWorkflowExecutionMock).toHaveBeenCalledWith({id: 5});
            expect(getTriggerExecutionWorkflowExecutionMock).not.toHaveBeenCalled();
        });
    });
});
