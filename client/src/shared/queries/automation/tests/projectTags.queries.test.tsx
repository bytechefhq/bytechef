import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {renderHook, waitFor} from '@testing-library/react';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {ProjectTagKeys, useGetProjectTagsQuery} from '../projectTags.queries';

const {getProjectTagsMock} = vi.hoisted(() => ({getProjectTagsMock: vi.fn()}));

vi.mock('@/shared/middleware/automation/configuration', () => ({
    ProjectTagApi: class {
        getProjectTags = getProjectTagsMock;
    },
}));

let queryClient: QueryClient;

function Wrapper({children}: {children: ReactNode}) {
    return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
}

describe('projectTags.queries', () => {
    beforeEach(() => {
        getProjectTagsMock.mockReset();

        queryClient = new QueryClient({defaultOptions: {queries: {retry: false}}});

        useWorkspaceStore.setState({currentWorkspaceId: 7});
    });

    it('keys the cache by workspace, so two workspaces cannot share an entry', () => {
        expect(ProjectTagKeys.workspaceProjectTags(1)).toEqual(['projectTags', 1]);
        expect(ProjectTagKeys.workspaceProjectTags(1)).not.toEqual(ProjectTagKeys.workspaceProjectTags(2));
    });

    it('asks for the tags of the current workspace', async () => {
        getProjectTagsMock.mockResolvedValue([{id: 10, name: 'billing'}]);

        const {result} = renderHook(() => useGetProjectTagsQuery(useWorkspaceStore.getState().currentWorkspaceId!), {
            wrapper: Wrapper,
        });

        await waitFor(() => expect(result.current.isSuccess).toBe(true));

        expect(getProjectTagsMock).toHaveBeenCalledWith({id: 7});
        expect(result.current.data).toEqual([{id: 10, name: 'billing'}]);
        expect(queryClient.getQueryData(ProjectTagKeys.workspaceProjectTags(7))).toEqual([{id: 10, name: 'billing'}]);
    });

    it('does not fetch while no workspace is selected', () => {
        useWorkspaceStore.getState().clearCurrentWorkspaceId();

        const {result} = renderHook(() => useGetProjectTagsQuery(useWorkspaceStore.getState().currentWorkspaceId!), {
            wrapper: Wrapper,
        });

        expect(result.current.fetchStatus).toBe('idle');
        expect(getProjectTagsMock).not.toHaveBeenCalled();
    });
});
