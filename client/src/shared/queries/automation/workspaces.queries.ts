import {WorkspaceApi, WorkspaceModel} from '@/shared/middleware/automation/configuration';

/* eslint-disable sort-keys */
import {useQuery} from '@tanstack/react-query';

export const WorkspaceKeys = {
    userWorkspaces: (userId: number) => [WorkspaceKeys.workspaces, 'users', userId] as const,
    workspaces: ['workspaces'] as const,
};

export const useGetUserWorkspacesQuery = (userId: number, enabled?: boolean) =>
    useQuery<WorkspaceModel[], Error>({
        queryKey: WorkspaceKeys.userWorkspaces(userId),
        queryFn: () => new WorkspaceApi().getUserWorkspaces({id: userId}),
        enabled: enabled === undefined ? true : enabled,
    });
