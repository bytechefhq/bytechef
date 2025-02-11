import {GitConfiguration, GitConfigurationApi} from '@/ee/shared/middleware/platform/configuration';

/* eslint-disable sort-keys */
import {useQuery} from '@tanstack/react-query';

export const GitConfigurationKeys = {
    workspaceGitConfiguration: (workspaceId: number) =>
        [...GitConfigurationKeys.gitConfiguration, workspaceId] as const,
    gitConfiguration: ['gitConfiguration'] as const,
};

export const useGetWorkspaceGitConfigurationQuery = (workspaceId: number) =>
    useQuery<GitConfiguration, Error>({
        queryKey: GitConfigurationKeys.workspaceGitConfiguration(workspaceId),
        queryFn: () => new GitConfigurationApi().getGitConfiguration({id: workspaceId}),
    });
