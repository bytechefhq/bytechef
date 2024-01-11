/* eslint-disable sort-keys */
import {IntegrationTagApi, TagModel} from '@/middleware/embedded/configuration';
import {useQuery} from '@tanstack/react-query';

export const IntegrationTagKeys = {
    integrationTags: ['integrationTags'] as const,
};

export const useGetIntegrationTagsQuery = () =>
    useQuery<TagModel[], Error>({
        queryKey: IntegrationTagKeys.integrationTags,
        queryFn: () => new IntegrationTagApi().getIntegrationTags(),
    });
