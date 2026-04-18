import {WorkflowNodeParameterApi} from '@/shared/middleware/platform/configuration';
import {createTestQueryClientWrapper} from '@/shared/util/test-utils';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {act, renderHook, waitFor} from '@testing-library/react';
import {ReactNode} from 'react';
import {MockInstance, afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import {
    useDeleteClusterElementParameterMutation,
    useDeleteWorkflowNodeParameterMutation,
    useUpdateClusterElementParameterMutation,
    useUpdateWorkflowNodeParameterMutation,
} from './workflowNodeParameters.mutations';

vi.mock('@/shared/middleware/platform/configuration', () => ({
    WorkflowNodeParameterApi: vi.fn(),
}));

const INVALIDATED_KEYS = [
    ['workflowNodeDynamicProperties'],
    ['clusterElementDynamicProperties'],
    ['workflowNodeOptions'],
    ['clusterElementNodeOptions'],
    ['clusterElementOptions'],
];

const SUCCESSFUL_RESPONSE = {
    displayConditions: {},
    metadata: {},
    parameters: {propertyA: 'valueA'},
};

describe('workflowNodeParameters mutations', () => {
    let mockUpdateWorkflowNodeParameter: ReturnType<typeof vi.fn>;
    let mockUpdateClusterElementParameter: ReturnType<typeof vi.fn>;
    let mockDeleteWorkflowNodeParameter: ReturnType<typeof vi.fn>;
    let mockDeleteClusterElementParameter: ReturnType<typeof vi.fn>;

    beforeEach(() => {
        mockUpdateWorkflowNodeParameter = vi.fn().mockResolvedValue(SUCCESSFUL_RESPONSE);
        mockUpdateClusterElementParameter = vi.fn().mockResolvedValue(SUCCESSFUL_RESPONSE);
        mockDeleteWorkflowNodeParameter = vi.fn().mockResolvedValue(SUCCESSFUL_RESPONSE);
        mockDeleteClusterElementParameter = vi.fn().mockResolvedValue(SUCCESSFUL_RESPONSE);

        const MockApi = vi.fn(function MockApi(this: Record<string, unknown>) {
            this.deleteClusterElementParameter = mockDeleteClusterElementParameter;
            this.deleteWorkflowNodeParameter = mockDeleteWorkflowNodeParameter;
            this.updateClusterElementParameter = mockUpdateClusterElementParameter;
            this.updateWorkflowNodeParameter = mockUpdateWorkflowNodeParameter;
        });

        vi.mocked(WorkflowNodeParameterApi).mockImplementation(MockApi as unknown as typeof WorkflowNodeParameterApi);
    });

    afterEach(() => {
        vi.clearAllMocks();
    });

    describe('useUpdateWorkflowNodeParameterMutation', () => {
        it('invalidates dependent query caches after a successful save so stale cached snapshots are refetched', async () => {
            const {invalidateSpy, wrapper} = createSpyingWrapper();

            const {result} = renderHook(() => useUpdateWorkflowNodeParameterMutation(), {wrapper});

            await act(async () => {
                await result.current.mutateAsync({
                    environmentId: 1,
                    id: 'workflow-1',
                    updateWorkflowNodeParameterRequest: {path: 'propertyA', type: 'STRING', value: {}},
                    workflowNodeName: 'httpClient_1',
                });
            });

            expectAllDependentQueriesInvalidated(invalidateSpy);
        });

        it('still runs the caller-supplied onSuccess after invalidating caches', async () => {
            const callerOnSuccess = vi.fn();

            const {result} = renderHook(() => useUpdateWorkflowNodeParameterMutation({onSuccess: callerOnSuccess}), {
                wrapper: createTestQueryClientWrapper(),
            });

            await act(async () => {
                await result.current.mutateAsync({
                    environmentId: 1,
                    id: 'workflow-1',
                    updateWorkflowNodeParameterRequest: {path: 'propertyA', type: 'STRING', value: {}},
                    workflowNodeName: 'httpClient_1',
                });
            });

            expect(callerOnSuccess).toHaveBeenCalledWith(
                SUCCESSFUL_RESPONSE,
                expect.objectContaining({workflowNodeName: 'httpClient_1'})
            );
        });

        it('does not invalidate dependent caches when the mutation fails', async () => {
            mockUpdateWorkflowNodeParameter.mockRejectedValue(new Error('boom'));

            const {invalidateSpy, wrapper} = createSpyingWrapper();

            const {result} = renderHook(() => useUpdateWorkflowNodeParameterMutation({onError: vi.fn()}), {wrapper});

            await act(async () => {
                await result.current
                    .mutateAsync({
                        environmentId: 1,
                        id: 'workflow-1',
                        updateWorkflowNodeParameterRequest: {path: 'propertyA', type: 'STRING', value: {}},
                        workflowNodeName: 'httpClient_1',
                    })
                    .catch(() => {});
            });

            await waitFor(() => expect(result.current.isError).toBe(true));

            expect(invalidateSpy).not.toHaveBeenCalled();
        });
    });

    describe('useUpdateClusterElementParameterMutation', () => {
        it('invalidates dependent query caches after a successful save', async () => {
            const {invalidateSpy, wrapper} = createSpyingWrapper();

            const {result} = renderHook(() => useUpdateClusterElementParameterMutation(), {wrapper});

            await act(async () => {
                await result.current.mutateAsync({
                    clusterElementType: 'model',
                    clusterElementWorkflowNodeName: 'chatModel_1',
                    environmentId: 1,
                    id: 'workflow-1',
                    updateClusterElementParameterRequest: {path: 'propertyA', type: 'STRING', value: {}},
                    workflowNodeName: 'aiAgent_1',
                });
            });

            expectAllDependentQueriesInvalidated(invalidateSpy);
        });
    });

    describe('useDeleteWorkflowNodeParameterMutation', () => {
        it('invalidates dependent query caches after a successful delete', async () => {
            const {invalidateSpy, wrapper} = createSpyingWrapper();

            const {result} = renderHook(() => useDeleteWorkflowNodeParameterMutation(), {wrapper});

            await act(async () => {
                await result.current.mutateAsync({
                    deleteClusterElementParameterRequest: {path: 'propertyA'},
                    environmentId: 1,
                    id: 'workflow-1',
                    workflowNodeName: 'httpClient_1',
                });
            });

            expectAllDependentQueriesInvalidated(invalidateSpy);
        });
    });

    describe('useDeleteClusterElementParameterMutation', () => {
        it('invalidates dependent query caches after a successful delete', async () => {
            const {invalidateSpy, wrapper} = createSpyingWrapper();

            const {result} = renderHook(() => useDeleteClusterElementParameterMutation(), {wrapper});

            await act(async () => {
                await result.current.mutateAsync({
                    clusterElementType: 'model',
                    clusterElementWorkflowNodeName: 'chatModel_1',
                    deleteClusterElementParameterRequest: {path: 'propertyA'},
                    environmentId: 1,
                    id: 'workflow-1',
                    workflowNodeName: 'aiAgent_1',
                });
            });

            expectAllDependentQueriesInvalidated(invalidateSpy);
        });
    });
});

function createSpyingWrapper() {
    const queryClient = new QueryClient({
        defaultOptions: {
            mutations: {retry: false},
            queries: {retry: false},
        },
    });

    const invalidateSpy = vi.spyOn(queryClient, 'invalidateQueries');

    const wrapper = ({children}: {children: ReactNode}) => (
        <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    );

    return {invalidateSpy, wrapper};
}

function expectAllDependentQueriesInvalidated(invalidateSpy: MockInstance) {
    const invalidatedKeys = invalidateSpy.mock.calls.map((call) => (call[0] as {queryKey: unknown[]}).queryKey);

    for (const expectedKey of INVALIDATED_KEYS) {
        expect(invalidatedKeys).toContainEqual(expectedKey);
    }
}
