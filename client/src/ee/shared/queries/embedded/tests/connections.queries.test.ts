import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {renderHook, waitFor} from '@testing-library/react';
import {ReactNode, createElement} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {ConnectionKeys, useGetAllConnectedUserConnectionsQuery} from '../connections.queries';

const hoisted = vi.hoisted(() => ({
    getAllConnectedUserConnections: vi.fn(),
}));

vi.mock('@/ee/shared/middleware/embedded/configuration', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/ee/shared/middleware/embedded/configuration')>()),
    ConnectionApi: vi.fn().mockImplementation(function () {
        return {getAllConnectedUserConnections: hoisted.getAllConnectedUserConnections};
    }),
}));

const createWrapper = () => {
    const queryClient = new QueryClient({defaultOptions: {queries: {retry: false}}});

    return ({children}: {children: ReactNode}) => createElement(QueryClientProvider, {client: queryClient}, children);
};

describe('connections.queries (embedded)', () => {
    describe('ConnectionKeys', () => {
        it('keys a connected user connections query by connected user', () => {
            const request = {componentName: 'slack'};

            expect(ConnectionKeys.connectedUserConnections(5, request)).toEqual([
                'embedded_connections',
                'connectedUser',
                5,
                request,
            ]);
        });

        it('never shares a key between connected user and admin connection lists', () => {
            const request = {componentName: 'slack'};

            expect(ConnectionKeys.connectedUserConnections(5, request)).not.toEqual(
                ConnectionKeys.filteredConnections(request)
            );
            expect(ConnectionKeys.connectedUserConnections(5, request)).not.toEqual(
                ConnectionKeys.connectedUserConnections(6, request)
            );
        });

        it('keys every connection of a connected user by connected user', () => {
            expect(ConnectionKeys.allConnectedUserConnections(5)).toEqual([
                'embedded_connections',
                'connectedUser',
                5,
                'all',
            ]);
            expect(ConnectionKeys.allConnectedUserConnections(5)).not.toEqual(
                ConnectionKeys.allConnectedUserConnections(6)
            );
        });
    });

    describe('useGetAllConnectedUserConnectionsQuery', () => {
        beforeEach(() => {
            hoisted.getAllConnectedUserConnections.mockReset();
        });

        it('reads every connection of the connected user in one request', async () => {
            const connections = [{componentName: 'slack', id: 1, name: 'Slack', origin: 'SELF_SERVE'}];

            hoisted.getAllConnectedUserConnections.mockResolvedValue(connections);

            const {result} = renderHook(() => useGetAllConnectedUserConnectionsQuery(5), {
                wrapper: createWrapper(),
            });

            await waitFor(() => {
                expect(result.current.data).toEqual(connections);
            });

            expect(hoisted.getAllConnectedUserConnections).toHaveBeenCalledTimes(1);
            expect(hoisted.getAllConnectedUserConnections).toHaveBeenCalledWith({connectedUserId: 5});
        });
    });
});
