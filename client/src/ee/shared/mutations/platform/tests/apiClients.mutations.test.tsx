import {useGetApiClientsQuery} from '@/ee/shared/queries/platform/apiClients.queries';
import {createTestQueryClientWrapper, renderHook, resetAll, waitFor} from '@/shared/util/test-utils';
import {afterEach, describe, expect, it, vi} from 'vitest';

import {
    useCreateApiClientMutation,
    useDeleteApiClientMutation,
    useUpdateApiClientMutation,
} from '../apiClients.mutations';

const {createApiClientMock, deleteApiClientMock, getApiClientsMock, updateApiClientMock} = vi.hoisted(() => ({
    createApiClientMock: vi.fn(),
    deleteApiClientMock: vi.fn(),
    getApiClientsMock: vi.fn(),
    updateApiClientMock: vi.fn(),
}));

vi.mock('@/ee/shared/middleware/automation/api-platform', () => ({
    ApiClientApi: class {
        createApiClient = createApiClientMock;
        deleteApiClient = deleteApiClientMock;
        getApiClients = getApiClientsMock;
        updateApiClient = updateApiClientMock;
    },
}));

describe('apiClients mutations', () => {
    afterEach(() => {
        resetAll();
    });

    it('creates an API client and reports success', async () => {
        const onSuccess = vi.fn();

        createApiClientMock.mockResolvedValue({secretKey: 'secret'});

        const {result} = renderHook(() => useCreateApiClientMutation({onSuccess}), {
            wrapper: createTestQueryClientWrapper(),
        });

        result.current.mutate({name: 'client', secretKey: 'secret'});

        await waitFor(() => expect(onSuccess).toHaveBeenCalled());

        expect(createApiClientMock).toHaveBeenCalledWith({apiClient: {name: 'client', secretKey: 'secret'}});
    });

    it('reports a create failure to the caller', async () => {
        const onError = vi.fn();

        createApiClientMock.mockRejectedValue(new Error('cannot create'));

        const {result} = renderHook(() => useCreateApiClientMutation({onError}), {
            wrapper: createTestQueryClientWrapper(),
        });

        result.current.mutate({name: 'client', secretKey: 'secret'});

        await waitFor(() => expect(onError).toHaveBeenCalled());
    });

    it('deletes an API client by id', async () => {
        const onSuccess = vi.fn();

        deleteApiClientMock.mockResolvedValue(undefined);

        const {result} = renderHook(() => useDeleteApiClientMutation({onSuccess}), {
            wrapper: createTestQueryClientWrapper(),
        });

        result.current.mutate(4);

        await waitFor(() => expect(onSuccess).toHaveBeenCalled());

        expect(deleteApiClientMock).toHaveBeenCalledWith({id: 4});
    });

    it('updates an API client under its own id', async () => {
        const onSuccess = vi.fn();

        updateApiClientMock.mockResolvedValue(undefined);

        const {result} = renderHook(() => useUpdateApiClientMutation({onSuccess}), {
            wrapper: createTestQueryClientWrapper(),
        });

        result.current.mutate({id: 4, name: 'renamed', secretKey: 'secret'});

        await waitFor(() => expect(onSuccess).toHaveBeenCalled());

        expect(updateApiClientMock).toHaveBeenCalledWith({
            apiClient: {id: 4, name: 'renamed', secretKey: 'secret'},
            id: 4,
        });
    });
});

describe('apiClients queries', () => {
    afterEach(() => {
        resetAll();
    });

    it('loads the API clients', async () => {
        getApiClientsMock.mockResolvedValue([{id: 4, name: 'client'}]);

        const {result} = renderHook(() => useGetApiClientsQuery(), {wrapper: createTestQueryClientWrapper()});

        await waitFor(() => expect(result.current.data).toEqual([{id: 4, name: 'client'}]));
    });
});
