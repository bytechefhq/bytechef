import {
    useGetClusterElementOutputQuery,
    useGetWorkflowNodeOutputQuery,
} from '@/shared/queries/platform/workflowNodeOutputs.queries';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {renderHook, waitFor} from '@testing-library/react';
import {type ReactNode, createElement} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const mockGetClusterElementOutput = vi.fn();
const mockGetWorkflowNodeOutput = vi.fn();

vi.mock('@/shared/middleware/platform/configuration', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/middleware/platform/configuration')>()),
    WorkflowNodeOutputApi: class {
        getClusterElementOutput = mockGetClusterElementOutput;
        getWorkflowNodeOutput = mockGetWorkflowNodeOutput;
    },
}));

const createWrapper = () => {
    const queryClient = new QueryClient();

    return ({children}: {children: ReactNode}) => createElement(QueryClientProvider, {client: queryClient}, children);
};

describe('workflowNodeOutputs queries', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('should not retry a failed workflow node output request', async () => {
        mockGetWorkflowNodeOutput.mockRejectedValue(new Error('Cannot deserialize value'));

        const {result} = renderHook(
            () => useGetWorkflowNodeOutputQuery({environmentId: 1, id: 'workflowId', workflowNodeName: 'webhook_1'}),
            {wrapper: createWrapper()}
        );

        await waitFor(() => expect(result.current.isError).toBe(true), {interval: 5, timeout: 5000});

        expect(mockGetWorkflowNodeOutput).toHaveBeenCalledTimes(1);
    });

    it('should not retry a failed cluster element output request', async () => {
        mockGetClusterElementOutput.mockRejectedValue(new Error('Cannot deserialize value'));

        const {result} = renderHook(
            () =>
                useGetClusterElementOutputQuery({
                    clusterElementType: 'tools',
                    clusterElementWorkflowNodeName: 'tool_1',
                    environmentId: 1,
                    id: 'workflowId',
                    workflowNodeName: 'aiAgent_1',
                }),
            {wrapper: createWrapper()}
        );

        await waitFor(() => expect(result.current.isError).toBe(true), {interval: 5, timeout: 5000});

        expect(mockGetClusterElementOutput).toHaveBeenCalledTimes(1);
    });
});
