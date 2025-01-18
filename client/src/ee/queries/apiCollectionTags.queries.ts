import {ApiCollectionTagApi} from '@/ee/shared/middleware/automation/api-platform';

/* eslint-disable sort-keys */
import {Tag} from '@/shared/middleware/automation/configuration';
import {useQuery} from '@tanstack/react-query';

export const ApiCollectionTagKeys = {
    apiCollectionTags: ['apiCollectionTags'] as const,
};

export const useGetApiCollectionTagsQuery = () =>
    useQuery<Tag[], Error>({
        queryKey: ApiCollectionTagKeys.apiCollectionTags,
        queryFn: () => new ApiCollectionTagApi().getApiCollectionTags(),
    });
