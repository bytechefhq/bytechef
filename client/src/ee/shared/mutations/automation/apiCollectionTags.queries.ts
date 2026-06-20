import {ApiCollectionTagApi} from '@/ee/shared/middleware/automation/api-platform';

/* eslint-disable sort-keys */
import {Tag} from '@/shared/middleware/automation/configuration';
import {useQuery} from '@tanstack/react-query';

export const ApiCollectionTagKeys = {
    apiCollectionTags: (id: number) => ['apiCollectionTags', id] as const,
};

export const useGetApiCollectionTagsQuery = (id: number) =>
    useQuery<Tag[], Error>({
        queryKey: ApiCollectionTagKeys.apiCollectionTags(id),
        queryFn: () => new ApiCollectionTagApi().getApiCollectionTags({id}),
    });
