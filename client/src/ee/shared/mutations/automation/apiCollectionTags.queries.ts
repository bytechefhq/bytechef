import {ApiCollectionTagApi} from '@/ee/shared/middleware/automation/api-platform';

/* eslint-disable sort-keys */
import {Tag} from '@/shared/middleware/automation/configuration';
import {useQuery} from '@tanstack/react-query';

export const ApiCollectionTagKeys = {
    apiCollectionTags: ['apiCollectionTags'] as const,
    workspaceApiCollectionTags: (workspaceId: number) => [...ApiCollectionTagKeys.apiCollectionTags, workspaceId],
};

export const useGetApiCollectionTagsQuery = (workspaceId: number) =>
    useQuery<Tag[], Error>({
        queryKey: ApiCollectionTagKeys.workspaceApiCollectionTags(workspaceId),
        queryFn: () => new ApiCollectionTagApi().getApiCollectionTags({id: workspaceId}),
    });
