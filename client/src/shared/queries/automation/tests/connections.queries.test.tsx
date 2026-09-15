import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {environmentStore} from '@/shared/stores/useEnvironmentStore';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {renderHook, waitFor} from '@testing-library/react';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {ConnectionKeys, useGetConnectionTagsQuery} from '../connections.queries';

const {getConnectionTagsMock} = vi.hoisted(() => ({getConnectionTagsMock: vi.fn()}));

vi.mock('@/shared/middleware/automation/configuration', () => ({
    ConnectionTagApi: class {
        getConnectionTags = getConnectionTagsMock;
    },
}));

let queryClient: QueryClient;

function Wrapper({children}: {children: ReactNode}) {
    return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
}

describe('connections.queries', () => {
    beforeEach(() => {
        getConnectionTagsMock.mockReset();

        queryClient = new QueryClient({defaultOptions: {queries: {retry: false}}});

        useWorkspaceStore.setState({currentWorkspaceId: 7});
    });

    it('keys the cache by workspace, so two workspaces cannot share an entry', () => {
        expect(ConnectionKeys.workspaceConnectionTags(1)).toEqual(['automation_connectionTags', 1]);
        expect(ConnectionKeys.workspaceConnectionTags(1)).not.toEqual(ConnectionKeys.workspaceConnectionTags(2));
    });

    it('asks for the tags of the current workspace', async () => {
        getConnectionTagsMock.mockResolvedValue([{id: 10, name: 'billing'}]);

        const {result} = renderHook(() => useGetConnectionTagsQuery(), {wrapper: Wrapper});

        await waitFor(() => expect(result.current.isSuccess).toBe(true));

        const {currentEnvironmentId} = environmentStore.getState();

        expect(getConnectionTagsMock).toHaveBeenCalledWith({environmentId: currentEnvironmentId, id: 7});
        expect(result.current.data).toEqual([{id: 10, name: 'billing'}]);
        expect(queryClient.getQueryData(ConnectionKeys.workspaceConnectionTags(7, currentEnvironmentId))).toEqual([
            {id: 10, name: 'billing'},
        ]);
    });

    it('does not fetch while no workspace is selected', () => {
        useWorkspaceStore.getState().clearCurrentWorkspaceId();

        const {result} = renderHook(() => useGetConnectionTagsQuery(), {wrapper: Wrapper});

        expect(result.current.fetchStatus).toBe('idle');
        expect(getConnectionTagsMock).not.toHaveBeenCalled();
    });
});
