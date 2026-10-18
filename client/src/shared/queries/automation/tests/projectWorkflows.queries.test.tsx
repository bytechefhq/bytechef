import {ResponseError} from '@/shared/middleware/automation/configuration';
import {ProjectWorkflowKeys, useGetProjectWorkflowQuery} from '@/shared/queries/automation/projectWorkflows.queries';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {renderHook, waitFor} from '@testing-library/react';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const hoisted = vi.hoisted(() => ({
    getProjectWorkflow: vi.fn(),
}));

vi.mock('@/shared/middleware/automation/configuration', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/middleware/automation/configuration')>()),
    WorkflowApi: class {
        getProjectWorkflow = hoisted.getProjectWorkflow;
    },
}));

const createWrapper = (queryClient: QueryClient) => {
    const Wrapper = ({children}: {children: ReactNode}) => (
        <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    );

    return Wrapper;
};

const responseError = (status: number) => new ResponseError(new Response(null, {status}), 'Response returned an error');

describe('useGetProjectWorkflowQuery', () => {
    let queryClient: QueryClient;

    beforeEach(() => {
        hoisted.getProjectWorkflow.mockReset();

        queryClient = new QueryClient({defaultOptions: {queries: {retryDelay: 0}}});
    });

    it.each([400, 401, 403, 404])('does not retry a workflow the server answers with %i', async (status) => {
        hoisted.getProjectWorkflow.mockRejectedValue(responseError(status));

        const {result} = renderHook(() => useGetProjectWorkflowQuery(1, 2), {wrapper: createWrapper(queryClient)});

        await waitFor(() => expect(result.current.isError).toBe(true));

        expect(hoisted.getProjectWorkflow).toHaveBeenCalledTimes(1);
    });

    it('still retries a server error', async () => {
        hoisted.getProjectWorkflow.mockRejectedValue(responseError(503));

        const {result} = renderHook(() => useGetProjectWorkflowQuery(1, 2), {wrapper: createWrapper(queryClient)});

        await waitFor(() => expect(result.current.isError).toBe(true));

        expect(hoisted.getProjectWorkflow).toHaveBeenCalledTimes(4);
    });

    it.each([408, 429])('still retries a transient %i', async (status) => {
        hoisted.getProjectWorkflow.mockRejectedValue(responseError(status));

        const {result} = renderHook(() => useGetProjectWorkflowQuery(1, 2), {wrapper: createWrapper(queryClient)});

        await waitFor(() => expect(result.current.isError).toBe(true));

        expect(hoisted.getProjectWorkflow).toHaveBeenCalledTimes(4);
    });

    it('still retries a network failure', async () => {
        hoisted.getProjectWorkflow.mockRejectedValue(new TypeError('Failed to fetch'));

        const {result} = renderHook(() => useGetProjectWorkflowQuery(1, 2), {wrapper: createWrapper(queryClient)});

        await waitFor(() => expect(result.current.isError).toBe(true));

        expect(hoisted.getProjectWorkflow).toHaveBeenCalledTimes(4);
    });

    it('does not refetch on mount when a caller with a stale time finds fresh data, but does after invalidation', async () => {
        hoisted.getProjectWorkflow.mockResolvedValue({id: 'workflow-1'});

        const {result: viewerResult} = renderHook(() => useGetProjectWorkflowQuery(1, 2), {
            wrapper: createWrapper(queryClient),
        });

        await waitFor(() => expect(viewerResult.current.isSuccess).toBe(true));

        const {result: editorResult} = renderHook(
            () => useGetProjectWorkflowQuery(1, 2, true, undefined, {staleTime: 30_000}),
            {wrapper: createWrapper(queryClient)}
        );

        await new Promise((resolve) => setTimeout(resolve, 10));

        expect(editorResult.current.data).toEqual({id: 'workflow-1'});
        expect(hoisted.getProjectWorkflow).toHaveBeenCalledTimes(1);

        await queryClient.invalidateQueries({queryKey: ProjectWorkflowKeys.projectWorkflow(1, 2)});

        await waitFor(() => expect(hoisted.getProjectWorkflow).toHaveBeenCalledTimes(2));
    });
});
