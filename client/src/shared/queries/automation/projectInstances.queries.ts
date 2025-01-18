/* eslint-disable sort-keys */
import {
    GetWorkspaceProjectInstancesRequest,
    ProjectInstance,
    ProjectInstanceApi,
} from '@/shared/middleware/automation/configuration';
import {useQuery} from '@tanstack/react-query';

export const ProjectInstanceKeys = {
    filteredProjectInstances: (filters: GetWorkspaceProjectInstancesRequest) => [
        ...ProjectInstanceKeys.projectInstances,
        filters.id,
        filters,
    ],
    projectInstance: (id: number) => [...ProjectInstanceKeys.projectInstances, id],
    projectInstances: ['projectInstances'] as const,
};

export const useGetProjectInstanceQuery = (id: number, enabled?: boolean) =>
    useQuery<ProjectInstance, Error>({
        queryKey: ProjectInstanceKeys.projectInstance(id),
        queryFn: () => new ProjectInstanceApi().getProjectInstance({id}),
        enabled: enabled === undefined ? true : enabled,
    });

export const useGetWorkspaceProjectInstancesQuery = (filters: GetWorkspaceProjectInstancesRequest) =>
    useQuery<ProjectInstance[], Error>({
        queryKey: ProjectInstanceKeys.filteredProjectInstances(filters),
        queryFn: () => new ProjectInstanceApi().getWorkspaceProjectInstances(filters),
    });
