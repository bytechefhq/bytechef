import {Workspace, WorkspaceApi} from '@/ee/shared/middleware/automation/configuration';

/* eslint-disable sort-keys */
import {useQuery} from '@tanstack/react-query';

export const WorkspaceKeys = {
    workspaces: ['workspaces'] as const,
};

export const useGetWorkspacesQuery = (enabled?: boolean) =>
    useQuery<Workspace[], Error>({
        queryKey: WorkspaceKeys.workspaces,
        queryFn: () => new WorkspaceApi().getWorkspaces(),
        enabled: enabled === undefined ? true : enabled,
    });
