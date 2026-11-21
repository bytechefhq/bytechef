import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {renderHook, waitFor} from '@testing-library/react';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {ProjectDeploymentTagKeys, useGetProjectDeploymentTagsQuery} from '../projectDeploymentTags.queries';

const {getProjectDeploymentTagsMock} = vi.hoisted(() => ({getProjectDeploymentTagsMock: vi.fn()}));

vi.mock('@/shared/middleware/automation/configuration', () => ({
    ProjectDeploymentTagApi: class {
        getProjectDeploymentTags = getProjectDeploymentTagsMock;
    },
}));

let queryClient: QueryClient;

function Wrapper({children}: {children: ReactNode}) {
    return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
}

describe('projectDeploymentTags.queries', () => {
    beforeEach(() => {
        getProjectDeploymentTagsMock.mockReset();

        queryClient = new QueryClient({defaultOptions: {queries: {retry: false}}});

        useWorkspaceStore.setState({currentWorkspaceId: 7});
    });

    it('keys the cache by workspace, so two workspaces cannot share an entry', () => {
        expect(ProjectDeploymentTagKeys.workspaceProjectDeploymentTags({environmentId: 2, id: 1})).toEqual([
            'projectDeploymentTags',
            {environmentId: 2, id: 1},
        ]);
        expect(ProjectDeploymentTagKeys.workspaceProjectDeploymentTags({environmentId: 2, id: 1})).not.toEqual(
            ProjectDeploymentTagKeys.workspaceProjectDeploymentTags({environmentId: 2, id: 2})
        );
    });

    it('asks for the tags of the current workspace', async () => {
        getProjectDeploymentTagsMock.mockResolvedValue([{id: 10, name: 'billing'}]);

        const {result} = renderHook(
            () =>
                useGetProjectDeploymentTagsQuery({
                    environmentId: 2,
                    id: useWorkspaceStore.getState().currentWorkspaceId!,
                }),
            {wrapper: Wrapper}
        );

        await waitFor(() => expect(result.current.isSuccess).toBe(true));

        expect(getProjectDeploymentTagsMock).toHaveBeenCalledWith({environmentId: 2, id: 7});
        expect(result.current.data).toEqual([{id: 10, name: 'billing'}]);
        expect(
            queryClient.getQueryData(ProjectDeploymentTagKeys.workspaceProjectDeploymentTags({environmentId: 2, id: 7}))
        ).toEqual([{id: 10, name: 'billing'}]);
    });

    it('does not fetch while no workspace is selected', () => {
        useWorkspaceStore.getState().clearCurrentWorkspaceId();

        const {result} = renderHook(
            () =>
                useGetProjectDeploymentTagsQuery({
                    environmentId: 2,
                    id: useWorkspaceStore.getState().currentWorkspaceId!,
                }),
            {wrapper: Wrapper}
        );

        expect(result.current.fetchStatus).toBe('idle');
        expect(getProjectDeploymentTagsMock).not.toHaveBeenCalled();
    });
});
