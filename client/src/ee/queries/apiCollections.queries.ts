import {
    ApiCollection,
    ApiCollectionApi,
    Environment,
    GetWorkspaceApiCollectionsRequest,
} from '@/ee/shared/middleware/automation/api-platform';

/* eslint-disable sort-keys */
import {useQuery} from '@tanstack/react-query';

export const ApiCollectionKeys = {
    filteredProjectDeployments: (filters: {
        id?: number;
        environment?: Environment;
        projectId?: number;
        tagId?: number;
    }) => [...ApiCollectionKeys.apiCollections, filters],
    apiCollections: ['apiCollections'] as const,
};

export const useGetApiCollectionsQuery = (request: GetWorkspaceApiCollectionsRequest) =>
    useQuery<ApiCollection[], Error>({
        queryKey: ApiCollectionKeys.filteredProjectDeployments(request),
        queryFn: () => new ApiCollectionApi().getWorkspaceApiCollections(request),
    });
