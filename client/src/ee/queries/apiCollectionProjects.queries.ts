import {
    GetWorkspaceApiCollectionProjectsRequest,
    ProjectApi,
    ProjectBasic,
} from '@/ee/shared/middleware/automation/api-platform';

/* eslint-disable sort-keys */
import {useQuery} from '@tanstack/react-query';

export const ApiCollectionProjectKeys = {
    filteredProjects: (filters: GetWorkspaceApiCollectionProjectsRequest) => [
        ...ApiCollectionProjectKeys.apiCollectionProjects,
        filters.id,
        filters,
    ],
    project: (id: number) => [...ApiCollectionProjectKeys.apiCollectionProjects, id],
    apiCollectionProjects: ['apiCollectionProjects'] as const,
};

export const useGetWorkspaceApiCollectionProjectsQuery = (filters: GetWorkspaceApiCollectionProjectsRequest) =>
    useQuery<ProjectBasic[], Error>({
        queryKey: ApiCollectionProjectKeys.filteredProjects(filters),
        queryFn: () => new ProjectApi().getWorkspaceApiCollectionProjects(filters),
    });
