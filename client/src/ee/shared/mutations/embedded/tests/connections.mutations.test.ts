import {useUpdateConnectionMutation} from '@/ee/shared/mutations/embedded/connections.mutations';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {act, renderHook} from '@testing-library/react';
import {type ReactNode, createElement} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const hoisted = vi.hoisted(() => ({
    updateConnection: vi.fn(),
}));

vi.mock('@/ee/shared/middleware/embedded/configuration', () => ({
    ConnectionApi: class {
        updateConnection = hoisted.updateConnection;
    },
}));

const createWrapper = () => {
    const queryClient = new QueryClient({defaultOptions: {mutations: {retry: false}}});

    return ({children}: {children: ReactNode}) => createElement(QueryClientProvider, {client: queryClient}, children);
};

describe('useUpdateConnectionMutation', () => {
    beforeEach(() => {
        hoisted.updateConnection.mockReset();
        hoisted.updateConnection.mockResolvedValue(undefined);
    });

    it('forwards the shared flag to the update request', async () => {
        const {result} = renderHook(() => useUpdateConnectionMutation(), {wrapper: createWrapper()});

        await act(async () => {
            await result.current.mutateAsync({
                componentName: 'airtable',
                connectionVersion: 1,
                id: 1102,
                name: 'Acme Ops Airtable',
                parameters: {},
                shared: true,
                tags: [],
                version: 2,
            });
        });

        expect(hoisted.updateConnection).toHaveBeenCalledWith({
            id: 1102,
            updateConnectionRequest: {name: 'Acme Ops Airtable', shared: true, tags: [], version: 2},
        });
    });

    it('sends shared as false when the admin stops sharing', async () => {
        const {result} = renderHook(() => useUpdateConnectionMutation(), {wrapper: createWrapper()});

        await act(async () => {
            await result.current.mutateAsync({
                componentName: 'airtable',
                connectionVersion: 1,
                id: 1102,
                name: 'Acme Ops Airtable',
                parameters: {},
                shared: false,
                tags: [],
                version: 3,
            });
        });

        expect(hoisted.updateConnection).toHaveBeenCalledWith({
            id: 1102,
            updateConnectionRequest: {name: 'Acme Ops Airtable', shared: false, tags: [], version: 3},
        });
    });
});
