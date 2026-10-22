import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {renderHook, waitFor} from '@testing-library/react';
import {type ReactNode, createElement} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {
    TASK_EXECUTION_CHILDREN_PAGE_SIZE,
    WorkflowExecutionKeys,
    getWorkflowExecutionsRefetchInterval,
    useGetProjectWorkflowExecutionQuery,
    useGetWorkflowExecutionTaskExecutionChildrenInfiniteQuery,
    useGetWorkspaceProjectWorkflowExecutionsQuery,
} from '../workflowExecutions.queries';

const {
    getTriggerExecutionWorkflowExecutionMock,
    getWorkflowExecutionMock,
    getWorkflowExecutionTaskExecutionChildrenMock,
    getWorkflowExecutionsPageMock,
} = vi.hoisted(() => ({
    getTriggerExecutionWorkflowExecutionMock: vi.fn(),
    getWorkflowExecutionMock: vi.fn(),
    getWorkflowExecutionTaskExecutionChildrenMock: vi.fn(),
    getWorkflowExecutionsPageMock: vi.fn(),
}));

vi.mock('@/shared/middleware/automation/workflow/execution', () => ({
    WorkflowExecutionApi: class {
        getTriggerExecutionWorkflowExecution = getTriggerExecutionWorkflowExecutionMock;
        getWorkflowExecution = getWorkflowExecutionMock;
        getWorkflowExecutionTaskExecutionChildren = getWorkflowExecutionTaskExecutionChildrenMock;
        getWorkflowExecutionsPage = getWorkflowExecutionsPageMock;
    },
}));

const wrapper = ({children}: {children: ReactNode}) =>
    createElement(
        QueryClientProvider,
        {client: new QueryClient({defaultOptions: {queries: {retry: false}}})},
        children
    );

describe('getWorkflowExecutionsRefetchInterval', () => {
    it('polls while a job on the page is created or started', () => {
        expect(getWorkflowExecutionsRefetchInterval({content: [{job: {status: 'CREATED'}}]} as never)).toBe(2000);
        expect(
            getWorkflowExecutionsRefetchInterval({
                content: [{job: {status: 'COMPLETED'}}, {job: {status: 'STARTED'}}],
            } as never)
        ).toBe(2000);
    });

    it('stops polling once every job on the page has finished', () => {
        expect(
            getWorkflowExecutionsRefetchInterval({
                content: [{job: {status: 'COMPLETED'}}, {job: {status: 'FAILED'}}, {job: {status: 'STOPPED'}}],
            } as never)
        ).toBe(false);
    });

    it('does not poll for failed trigger rows without a job or before any page is loaded', () => {
        expect(getWorkflowExecutionsRefetchInterval({content: [{triggerExecution: {status: 'FAILED'}}]} as never)).toBe(
            false
        );
        expect(getWorkflowExecutionsRefetchInterval(undefined)).toBe(false);
    });
});

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

    describe('useGetWorkspaceProjectWorkflowExecutionsQuery', () => {
        const request = {id: 1, pageNumber: 0, projectDeploymentId: 3, workflowId: 'workflow1'};

        beforeEach(() => {
            getWorkflowExecutionsPageMock.mockReset();
            getWorkflowExecutionsPageMock.mockResolvedValue({content: [], totalPages: 0});
        });

        it('fetches automation executions when enabled is not given', async () => {
            const {result} = renderHook(() => useGetWorkspaceProjectWorkflowExecutionsQuery(request), {wrapper});

            await waitFor(() => expect(result.current.data).toEqual({content: [], totalPages: 0}));

            expect(getWorkflowExecutionsPageMock).toHaveBeenCalledWith({...request, embedded: false});
        });

        it('does not fetch while disabled', async () => {
            const {result} = renderHook(() => useGetWorkspaceProjectWorkflowExecutionsQuery(request, false), {
                wrapper,
            });

            await new Promise((resolve) => setTimeout(resolve, 10));

            expect(result.current.fetchStatus).toBe('idle');
            expect(getWorkflowExecutionsPageMock).not.toHaveBeenCalled();
        });

        it('refetches while a listed execution is still running, so its finished status replaces STARTED', async () => {
            getWorkflowExecutionsPageMock
                .mockResolvedValueOnce({content: [{id: 1, job: {id: 1, status: 'STARTED'}}], totalPages: 1})
                .mockResolvedValue({content: [{id: 1, job: {id: 1, status: 'COMPLETED'}}], totalPages: 1});

            const {result} = renderHook(() => useGetWorkspaceProjectWorkflowExecutionsQuery(request), {wrapper});

            await waitFor(
                () => expect(result.current.data?.content).toEqual([{id: 1, job: {id: 1, status: 'COMPLETED'}}]),
                {interval: 50, timeout: 5000}
            );
        });

        it('fetches once enabled', async () => {
            const {rerender, result} = renderHook(
                ({enabled}: {enabled: boolean}) => useGetWorkspaceProjectWorkflowExecutionsQuery(request, enabled),
                {initialProps: {enabled: false}, wrapper}
            );

            expect(getWorkflowExecutionsPageMock).not.toHaveBeenCalled();

            rerender({enabled: true});

            await waitFor(() => expect(result.current.isSuccess).toBe(true));

            expect(getWorkflowExecutionsPageMock).toHaveBeenCalledTimes(1);
        });
    });
});

describe('lazy workflow execution keys and children query', () => {
    beforeEach(() => {
        getWorkflowExecutionTaskExecutionChildrenMock.mockReset();
    });

    it('keeps the non-lazy key and distinguishes the lazy key', () => {
        expect(WorkflowExecutionKeys.workflowExecution(5, 'JOB', false)).toEqual(
            WorkflowExecutionKeys.workflowExecution(5)
        );
        expect(WorkflowExecutionKeys.workflowExecution(5, 'JOB', true)).not.toEqual(
            WorkflowExecutionKeys.workflowExecution(5)
        );
    });

    it('builds the children key per execution and task execution', () => {
        expect(WorkflowExecutionKeys.workflowExecutionTaskExecutionChildren(5, 9)).not.toEqual(
            WorkflowExecutionKeys.workflowExecutionTaskExecutionChildren(5, 10)
        );
    });

    it('nests every children key of a job under the key that invalidates them', () => {
        const childrenOfJobKey = WorkflowExecutionKeys.workflowExecutionTaskExecutionChildrenOfJob(5);

        expect(
            WorkflowExecutionKeys.workflowExecutionTaskExecutionChildren(5, 9).slice(0, childrenOfJobKey.length)
        ).toEqual(childrenOfJobKey);
    });

    it('pages children with the page size and computes the next page', async () => {
        getWorkflowExecutionTaskExecutionChildrenMock.mockResolvedValue({number: 0, totalPages: 3});

        const {result} = renderHook(
            () => useGetWorkflowExecutionTaskExecutionChildrenInfiniteQuery({id: 5, taskExecutionId: 9}, true),
            {wrapper}
        );

        await waitFor(() => expect(result.current.isSuccess).toBe(true));

        expect(TASK_EXECUTION_CHILDREN_PAGE_SIZE).toBe(50);
        expect(getWorkflowExecutionTaskExecutionChildrenMock).toHaveBeenCalledWith({
            id: 5,
            pageNumber: 0,
            pageSize: 50,
            taskExecutionId: 9,
        });
        expect(result.current.hasNextPage).toBe(true);
    });

    it('has no next page on the last page', async () => {
        getWorkflowExecutionTaskExecutionChildrenMock.mockResolvedValue({number: 2, totalPages: 3});

        const {result} = renderHook(
            () => useGetWorkflowExecutionTaskExecutionChildrenInfiniteQuery({id: 5, taskExecutionId: 9}, true),
            {wrapper}
        );

        await waitFor(() => expect(result.current.isSuccess).toBe(true));

        expect(result.current.hasNextPage).toBe(false);
    });

    it('starts on the requested page and pages back to the first one', async () => {
        getWorkflowExecutionTaskExecutionChildrenMock.mockImplementation(({pageNumber}: {pageNumber: number}) =>
            Promise.resolve({number: pageNumber, totalPages: 5})
        );

        const queryClient = new QueryClient({defaultOptions: {queries: {retry: false}}});

        const {result} = renderHook(
            () =>
                useGetWorkflowExecutionTaskExecutionChildrenInfiniteQuery({id: 5, taskExecutionId: 9}, true, false, 3),
            {
                wrapper: ({children}: {children: ReactNode}) =>
                    createElement(QueryClientProvider, {client: queryClient}, children),
            }
        );

        await waitFor(() => expect(result.current.isSuccess).toBe(true));

        expect(getWorkflowExecutionTaskExecutionChildrenMock).toHaveBeenCalledWith(
            expect.objectContaining({pageNumber: 3})
        );
        expect(result.current.hasPreviousPage).toBe(true);
        expect(result.current.hasNextPage).toBe(true);

        const previousPageResult = await result.current.fetchPreviousPage();

        expect(previousPageResult.data?.pages.map((page) => page.number)).toEqual([2, 3]);
    });

    it('has no previous page on the first page', async () => {
        getWorkflowExecutionTaskExecutionChildrenMock.mockResolvedValue({number: 0, totalPages: 3});

        const {result} = renderHook(
            () => useGetWorkflowExecutionTaskExecutionChildrenInfiniteQuery({id: 5, taskExecutionId: 9}, true),
            {wrapper}
        );

        await waitFor(() => expect(result.current.isSuccess).toBe(true));

        expect(result.current.hasPreviousPage).toBe(false);
    });
});
