import {GitConfiguration, GitConfigurationApi} from '@/ee/shared/middleware/platform/configuration';

/* eslint-disable sort-keys */
import {useQuery} from '@tanstack/react-query';

export const GitConfigurationKeys = {
    gitConfiguration: ['gitConfiguration'] as const,
};

export const useGetGitConfigurationQuery = () =>
    useQuery<GitConfiguration, Error>({
        queryKey: GitConfigurationKeys.gitConfiguration,
        queryFn: () => new GitConfigurationApi().getGitConfiguration(),
    });
