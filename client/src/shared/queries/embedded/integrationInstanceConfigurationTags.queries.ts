import {IntegrationInstanceConfigurationTagApi, Tag} from '@/shared/middleware/embedded/configuration';

/* eslint-disable sort-keys */
import {useQuery} from '@tanstack/react-query';

export const IntegrationInstanceConfigurationTagKeys = {
    integrationInstanceConfigurationTags: ['integrationInstanceConfigurationTags'] as const,
};

export const useGetIntegrationInstanceConfigurationTagsQuery = () =>
    useQuery<Tag[], Error>({
        queryKey: IntegrationInstanceConfigurationTagKeys.integrationInstanceConfigurationTags,
        queryFn: () => new IntegrationInstanceConfigurationTagApi().getIntegrationInstanceConfigurationTags(),
    });
