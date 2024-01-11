/* eslint-disable sort-keys */
import {IntegrationInstanceTagApi, TagModel} from '@/middleware/embedded/configuration';
import {useQuery} from '@tanstack/react-query';

export const IntegrationInstanceTagKeys = {
    integrationInstanceTags: ['integrationInstanceTags'] as const,
};

export const useGetIntegrationInstanceTagsQuery = () =>
    useQuery<TagModel[], Error>({
        queryKey: IntegrationInstanceTagKeys.integrationInstanceTags,
        queryFn: () => new IntegrationInstanceTagApi().getIntegrationInstanceTags(),
    });
