import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {act, renderHook} from '@testing-library/react';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const {createApiClientMock, deleteApiClientMock, updateApiClientMock} = vi.hoisted(() => ({
    createApiClientMock: vi.fn(),
    deleteApiClientMock: vi.fn(),
    updateApiClientMock: vi.fn(),
}));

vi.mock('@/ee/shared/middleware/automation/api-platform', () => ({
    ApiClientApi: vi.fn(function ApiClientApiMock() {
        return {
            createApiClient: createApiClientMock,
            deleteApiClient: deleteApiClientMock,
            updateApiClient: updateApiClientMock,
        };
    }),
}));

import {
    useCreateApiClientMutation,
    useDeleteApiClientMutation,
    useUpdateApiClientMutation,
} from '../apiClients.mutations';

const wrapper = ({children}: {children: ReactNode}) => (
    <QueryClientProvider client={new QueryClient({defaultOptions: {mutations: {retry: false}}})}>
        {children}
    </QueryClientProvider>
);

describe('apiClients.mutations', () => {
    beforeEach(() => {
        createApiClientMock.mockReset();
        deleteApiClientMock.mockReset();
        updateApiClientMock.mockReset();
    });

    it('creates an api client and hands the secret to onSuccess', async () => {
        const onSuccess = vi.fn();
        const apiClient = {name: 'Billing', secretKey: ''};

        createApiClientMock.mockResolvedValue({secretKey: 'secret'});

        const {result} = renderHook(() => useCreateApiClientMutation({onSuccess}), {wrapper});

        await act(async () => {
            await result.current.mutateAsync(apiClient);
        });

        expect(createApiClientMock).toHaveBeenCalledWith({apiClient});
        expect(onSuccess).toHaveBeenCalledWith({secretKey: 'secret'}, apiClient, undefined, expect.anything());
    });

    it('deletes an api client by id', async () => {
        deleteApiClientMock.mockResolvedValue(undefined);

        const {result} = renderHook(() => useDeleteApiClientMutation(), {wrapper});

        await act(async () => {
            await result.current.mutateAsync(5);
        });

        expect(deleteApiClientMock).toHaveBeenCalledWith({id: 5});
    });

    it('updates an api client by its own id and reports failures through onError', async () => {
        const onError = vi.fn();
        const apiClient = {id: 9, name: 'Billing', secretKey: ''};
        const failure = new Error('conflict');

        updateApiClientMock.mockRejectedValue(failure);

        const {result} = renderHook(() => useUpdateApiClientMutation({onError}), {wrapper});

        await act(async () => {
            await expect(result.current.mutateAsync(apiClient)).rejects.toBe(failure);
        });

        expect(updateApiClientMock).toHaveBeenCalledWith({apiClient, id: 9});
        expect(onError).toHaveBeenCalledWith(failure, apiClient, undefined, expect.anything());
    });
});
