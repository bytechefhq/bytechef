import {WorkspaceApi, WorkspaceModel} from '@/shared/middleware/automation/configuration';

/* eslint-disable sort-keys */
import {useQuery} from '@tanstack/react-query';

export const WorkspaceKeys = {
    workspaces: ['workspaces'] as const,
};

export const useGetWorkspacesQuery = () =>
    useQuery<WorkspaceModel[], Error>({
        queryKey: WorkspaceKeys.workspaces,
        queryFn: () => new WorkspaceApi().getWorkspaces(),
    });
