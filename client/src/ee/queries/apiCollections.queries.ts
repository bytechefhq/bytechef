import {
    ApiCollection,
    ApiCollectionApi,
    Environment,
    GetWorkspaceApiCollectionsRequest,
} from '@/middleware/automation/api-platform';

/* eslint-disable sort-keys */
import {useQuery} from '@tanstack/react-query';

export const ApiCollectionKeys = {
    filteredProjectInstances: (filters: {
        id?: number;
        environment?: Environment;
        projectId?: number;
        tagId?: number;
    }) => [...ApiCollectionKeys.apiCollections, filters],
    apiCollections: ['apiCollections'] as const,
};

export const useGetApiCollectionsQuery = (request: GetWorkspaceApiCollectionsRequest) =>
    useQuery<ApiCollection[], Error>({
        queryKey: ApiCollectionKeys.filteredProjectInstances(request),
        queryFn: () => new ApiCollectionApi().getWorkspaceApiCollections(request),
    });
